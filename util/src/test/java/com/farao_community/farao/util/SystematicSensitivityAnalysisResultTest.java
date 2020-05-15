/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityAnalysisResultTest {
    private static final double EPSILON = 1e-2;
    private SensitivityComputationResults sensitivityComputationResults;
    private Network network;
    private Cnec nStateCnec;
    private Cnec contingencyCnec;
    private RangeAction rangeAction;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();
        sensitivityComputationResults = (new MockSensiFactory()).create(network, null, 0)
                .run(new CracFactorsProvider(crac), new CracContingenciesProvider(crac), network.getVariantManager().getWorkingVariantId(), null).join();
        nStateCnec = crac.getCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        contingencyCnec = crac.getCnec("cnec1stateCurativeContingency1");
    }

    @Test
    public void testCompleteResultManipulation() {
        // When
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(sensitivityComputationResults);

        // Then
        assertTrue(result.isSuccess());
        //  in basecase
        double vNom = network.getBranch(nStateCnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV();
        assertEquals(10, result.getReferenceFlow(nStateCnec), EPSILON);
        assertEquals(100, result.getReferenceIntensity(nStateCnec), EPSILON);
        assertEquals(0.5, result.getSensitivityOnFlow(rangeAction, nStateCnec), EPSILON);
        assertEquals(0.25, result.getSensitivityOnIntensity(rangeAction, nStateCnec), EPSILON);

        //  after contingency
        assertEquals(-20, result.getReferenceFlow(contingencyCnec), EPSILON);
        assertEquals(-200, result.getReferenceIntensity(contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnFlow(rangeAction, contingencyCnec), EPSILON);
        assertEquals(-5, result.getSensitivityOnIntensity(rangeAction, contingencyCnec), EPSILON);

    }

    @Test
    public void testIncompleteSensiResult() {
        // When
        SensitivityComputationResults sensitivityComputationResults = Mockito.mock(SensitivityComputationResults.class);
        Mockito.when(sensitivityComputationResults.isOk()).thenReturn(false);
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(sensitivityComputationResults);

        // Then
        assertFalse(result.isSuccess());
    }

    private final class MockSensiFactory implements SensitivityComputationFactory {
        private final class MockSensi implements SensitivityComputation {
            private Network network;

            private MockSensi(Network network) {
                this.network = network;
            }

            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                List<SensitivityValue> values = sensitivityFactorsProvider.getFactors(network).stream()
                        .map(factor -> {
                            if (factor.getFunction() instanceof BranchFlow) {
                                return new SensitivityValue(factor, 0.5, 10, 10);
                            } else if (factor.getFunction() instanceof BranchIntensity) {
                                return new SensitivityValue(factor, 0.25, 100, -10);
                            } else {
                                throw new AssertionError();
                            }
                        })
                        .collect(Collectors.toList());
                return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", values));
            }

            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, ContingenciesProvider contingenciesProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                List<SensitivityValue> nStateValues = sensitivityFactorsProvider.getFactors(network).stream()
                        .map(factor -> {
                            if (factor.getFunction() instanceof BranchFlow) {
                                return new SensitivityValue(factor, 0.5, 10, 10);
                            } else if (factor.getFunction() instanceof BranchIntensity) {
                                return new SensitivityValue(factor, 0.25, 100, -10);
                            } else {
                                throw new AssertionError();
                            }
                        })
                        .collect(Collectors.toList());
                Map<String, List<SensitivityValue>> contingenciesValues = contingenciesProvider.getContingencies(network).stream()
                        .collect(Collectors.toMap(
                            contingency -> contingency.getId(),
                            contingency -> sensitivityFactorsProvider.getFactors(network).stream()
                               .map(factor -> {
                                   if (factor.getFunction() instanceof BranchFlow) {
                                       return new SensitivityValue(factor, -5, -20, 20);
                                   } else if (factor.getFunction() instanceof BranchIntensity) {
                                       return new SensitivityValue(factor, 5, 200, -20);
                                   } else {
                                       throw new AssertionError();
                                   }
                               })
                               .collect(Collectors.toList())
                        ));
                return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", nStateValues, contingenciesValues));
            }

            @Override
            public String getName() {
                return "MockSensi";
            }

            @Override
            public String getVersion() {
                return "0";
            }
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new MockSensi(network);
        }
    }
}
