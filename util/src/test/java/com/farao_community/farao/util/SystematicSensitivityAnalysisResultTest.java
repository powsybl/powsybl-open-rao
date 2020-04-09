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
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SystematicSensitivityAnalysisResultTest {
    private Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
    private Map<Cnec, Double> cnecFlowMap = new HashMap<>();
    private Map<Cnec, Double> cnecIntensityMap = new HashMap<>();
    private State state;
    private Cnec cnec;
    private RangeAction rangeAction;

    @Before
    public void setUp() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();
        SensitivityComputationResults sensiResults = (new MockSensiFactory()).create(network, null, 0)
                .run(new CracFactorsProvider(crac), null, null).join();
        state = crac.getPreventiveState();
        cnec = crac.getCnec("cnec1basecase");
        rangeAction = crac.getRangeAction("pst");
        stateSensiMap.put(state, sensiResults);
        cnecFlowMap.put(cnec, 10.);
        cnecIntensityMap.put(cnec, 100.);
    }

    @Test
    public void testCompleteResultManipulation() {
        // When
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap, cnecIntensityMap);

        // Then
        assertEquals(10, result.getFlow(cnec).get(), 0.1);
        assertEquals(100, result.getIntensity(cnec).get(), 0.1);
        assertFalse(result.anyStateDiverged());
        assertEquals(0.5, result.getSensitivity(cnec, state, rangeAction).get(), 0.1);

    }

    @Test
    public void testIncompleteSensiResult() {
        // When
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(Collections.singletonMap(state, null), cnecFlowMap, cnecIntensityMap);

        // Then
        assertTrue(result.anyStateDiverged());
        assertFalse(result.getSensitivity(cnec, state, rangeAction).isPresent());
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
                        .map(factor -> new SensitivityValue(factor, 0.5, 10, 10))
                        .collect(Collectors.toList());
                return CompletableFuture.completedFuture(new SensitivityComputationResults(true, Collections.emptyMap(), "", values));
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
