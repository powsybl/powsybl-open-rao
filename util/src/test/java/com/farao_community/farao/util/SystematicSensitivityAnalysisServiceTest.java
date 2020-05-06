/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisServiceTest {

    private Network network;
    private ComputationManager computationManager;
    private Crac crac;
    private SensitivityComputationParameters sensitivityComputationParameters;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = new MockSensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        sensitivityComputationParameters = Mockito.mock(SensitivityComputationParameters.class);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Test
    public void testSensiSAresult() {
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(new HashMap<>(), new HashMap<>(), new HashMap<>());
        assertNotNull(result);
    }

    @Test(expected = FaraoException.class)
    public void testException() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(new FaraoException("test exception."));
        LoadFlowService.init(loadFlowRunner, computationManager);
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        assertNotNull(result);
    }

    @Test
    public void testSensiSArunSensitivitySA() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        // Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Network network = (Network) args[0];
                network.getBranches().forEach(branch -> {
                    branch.getTerminal1().setP(120.);
                    branch.getTerminal2().setP(120.);
                }
                    );
                return new LoadFlowResultImpl(true, Collections.emptyMap(), "");
            }
        }).when(loadFlowRunner).run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        LoadFlowService.init(loadFlowRunner, computationManager);

        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        assertNotNull(result);
    }

    @Test
    public void testSensiSArunSensitivitySAFailure() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        // Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl());
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Network network = (Network) args[0];
                network.getBranches().forEach(branch -> {
                        branch.getTerminal1().setP(120.);
                        branch.getTerminal2().setP(120.);
                    }
                );
                return new LoadFlowResultImpl(true, Collections.emptyMap(), "");
            }
        }).when(loadFlowRunner).run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        LoadFlowService.init(loadFlowRunner, computationManager);
        crac.addRangeAction(new PstWithRange("myPst", new NetworkElement(network.getTwoWindingsTransformers().iterator().next().getId())));
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        assertNotNull(result);

        SensitivityComputationFactory sensitivityComputationFactory = new MockSensitivityComputationFactoryBroken();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
    }

    @Test
    public void testPositiveIntensitySignManagement() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Network network = (Network) args[0];
                network.getBranches().forEach(branch -> {
                    branch.getTerminal1().setP(120.);
                    branch.getTerminal1().setQ(0);
                    branch.getTerminal2().setP(120.);
                    branch.getTerminal2().setQ(0);
                });
                network.getBusView().getBuses().forEach(bus -> {
                    bus.setV(225);
                });

                return new LoadFlowResultImpl(true, Collections.emptyMap(), "");
            }
        }).when(loadFlowRunner).run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        LoadFlowService.init(loadFlowRunner, computationManager);

        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        crac.getCnecs().forEach(cnec ->  {
            assertTrue("Failed to produce positive intensity flow when active power flow is positive", result.getIntensity(cnec).get() > 0);
        });
    }

    @Test
    public void testNegativeIntensitySignManagement() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Network network = (Network) args[0];
                network.getBranches().forEach(branch -> {
                    branch.getTerminal1().setP(-120.);
                    branch.getTerminal1().setQ(0);
                    branch.getTerminal2().setP(-120.);
                    branch.getTerminal2().setQ(0);
                });
                network.getBusView().getBuses().forEach(bus -> {
                    bus.setV(225);
                });
                return new LoadFlowResultImpl(true, Collections.emptyMap(), "");
            }
        }).when(loadFlowRunner).run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        LoadFlowService.init(loadFlowRunner, computationManager);

        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        crac.getCnecs().forEach(cnec ->  {
            assertTrue("Failed to produce negative intensity flow when active power flow is negative", result.getIntensity(cnec).get() < 0);
        });
    }

    public class MockSensitivityComputationFactory implements SensitivityComputationFactory {
        class MockSensitivityComputation implements SensitivityComputation {
            private final Network network;

            MockSensitivityComputation(Network network) {
                this.network = network;
            }

            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
                return CompletableFuture.completedFuture(randomResults(network, sensitivityFactorsProvider));
            }

            private SensitivityComputationResults randomResults(Network network, SensitivityFactorsProvider sensitivityFactorsProvider) {
                List<SensitivityValue> randomSensitivities = sensitivityFactorsProvider.getFactors(network).stream().map(factor -> new SensitivityValue(factor, Math.random(), Math.random(), Math.random())).collect(Collectors.toList());
                return new SensitivityComputationResults(true, Collections.emptyMap(), "", randomSensitivities);
            }

            @Override
            public String getName() {
                return "Mock";
            }

            @Override
            public String getVersion() {
                return "Mock";
            }
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new MockSensitivityComputation(network);
        }
    }

    public class MockSensitivityComputationFactoryBroken implements SensitivityComputationFactory {
        class MockSensitivityComputation implements SensitivityComputation {

            MockSensitivityComputation() {
            }

            @Override
            public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider sensitivityFactorsProvider, String s, SensitivityComputationParameters sensitivityComputationParameters) {
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
        }

        @Override
        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
            return new MockSensitivityComputation();
        }
    }
}
