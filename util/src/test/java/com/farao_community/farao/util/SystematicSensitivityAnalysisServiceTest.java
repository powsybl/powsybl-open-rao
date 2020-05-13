/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisServiceTest {

    private Network network;
    private ComputationManager computationManager;
    private SimpleCrac crac;
    private SensitivityComputationParameters sensitivityComputationParameters;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = new SensitivityComputationFactoryRandomMock();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);
    }

    @Test
    public void testSensiSAresult() {
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(Mockito.mock(SensitivityComputationResults.class), network, crac);
        assertNotNull(result);
    }

    @Test
    public void testSensiSArunSensitivitySA() {
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        assertNotNull(result);
    }

    @Test
    public void testSensiSArunSensitivitySAFailure() {
        crac.addRangeAction(new PstWithRange("myPst", new NetworkElement(network.getTwoWindingsTransformers().iterator().next().getId())));
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        assertNotNull(result);

        SensitivityComputationFactory sensitivityComputationFactory = new SensitivityComputationFactoryBrokenMock();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
    }

    public class SensitivityComputationFactoryBrokenMock implements SensitivityComputationFactory {
        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new SensitivityComputation() {

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    throw new FaraoException("This should fail");
                }

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, ContingenciesProvider contingenciesProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                    throw new FaraoException("This should fail");
                }

                @Override
                public String getName() {
                    return "Mock";
                }

                @Override
                public String getVersion() {
                    return "Mock";
                }
            };
        }
    }

    public class SensitivityComputationFactoryRandomMock implements SensitivityComputationFactory {
        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int priority) {
            Random random = new Random();
            random.setSeed(42);
            return new SensitivityComputation() {
                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, ContingenciesProvider contingenciesProvider, String workingStateId, SensitivityComputationParameters sensiParameters) {
                    List<SensitivityValue> sensitivityValuesN = factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, random.nextDouble(), random.nextDouble(), random.nextDouble())).collect(Collectors.toList());
                    Map<String, List<SensitivityValue>> sensitivityValuesContingencies = contingenciesProvider.getContingencies(network).stream()
                            .collect(Collectors.toMap(
                                contingency -> contingency.getId(),
                                contingency -> factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, random.nextDouble(), random.nextDouble(), random.nextDouble())).collect(Collectors.toList())
                            ));
                    SensitivityComputationResults results = new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValuesN, sensitivityValuesContingencies);
                    return CompletableFuture.completedFuture(results);
                }

                @Override
                public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, String workingStateId, SensitivityComputationParameters sensiParameters) {
                    List<SensitivityValue> sensitivityValuesN = factorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, random.nextDouble(), random.nextDouble(), random.nextDouble())).collect(Collectors.toList());
                    SensitivityComputationResults results = new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValuesN, Collections.emptyMap());
                    return CompletableFuture.completedFuture(results);
                }

                @Override
                public String getName() {
                    return "Sensitivity computation mock";
                }

                @Override
                public String getVersion() {
                    return null;
                }
            };
        }
    }

}
