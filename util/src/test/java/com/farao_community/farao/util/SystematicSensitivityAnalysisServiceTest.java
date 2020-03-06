/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisServiceTest.class);

    private Network network;
    private ComputationManager computationManager;
    private Crac crac;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

        network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        crac = create();

        computationManager = LocalComputationManager.getDefault();
        SensitivityComputationFactory sensitivityComputationFactory = new MockSensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Test
    public void testSensiSAresult() {
        SystematicSensitivityAnalysisResult result = new SystematicSensitivityAnalysisResult(new HashMap<>(), new HashMap<>(), new HashMap<>());
        assertNotNull(result);
        assertNotNull(result.getStateSensiMap());
    }

    @Test(expected = FaraoException.class)
    public void testException() {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(new FaraoException("test exception."));
        LoadFlowService.init(loadFlowRunner, computationManager);
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
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

        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
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
        SystematicSensitivityAnalysisResult result = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
        assertNotNull(result);

        SensitivityComputationFactory sensitivityComputationFactory = new MockSensitivityComputationFactoryBroken();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
    }

    private static Crac create() {
        Crac crac = new SimpleCrac("idSimpleCracTestUS", "nameSimpleCracTestUS");

        ComplexContingency contingency1 = new ComplexContingency("Contingency FR1 FR3", "Trip of FFR1AA1 FFR3AA1 1",
                new HashSet<>(Arrays.asList(new NetworkElement("FFR1AA1  FFR3AA1  1"))));
        crac.addContingency(contingency1);
        ComplexContingency contingency2 = new ComplexContingency("Contingency FR1 FR2", "Trip of FFR1AA1 FFR2AA1 1",
                new HashSet<>(Arrays.asList(new NetworkElement("FFR1AA1  FFR2AA1  1"))));
        crac.addContingency(contingency2);

        // Instant
        Instant basecase = new Instant("initial", 0);
        Instant defaut = new Instant("default", 60);
        Instant curative = new Instant("curative", 1200);

        //NetworkElement
        NetworkElement monitoredElement1 = new NetworkElement("BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1 name");
        NetworkElement monitoredElement2 = new NetworkElement("FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1 name");

        // State
        State stateBasecase = new SimpleState(Optional.empty(), basecase);
        State stateCurativeContingency1 = new SimpleState(Optional.of(contingency1), curative);
        State stateCurativeContingency2 = new SimpleState(Optional.of(contingency2), curative);

        // Thresholds
        AbsoluteFlowThreshold thresholdAbsFlow = new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 1500);
        RelativeFlowThreshold thresholdRelativeFlow = new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30);

        Set<AbstractThreshold> thresholdsAbsFlow = Collections.singleton(thresholdAbsFlow);
        Set<AbstractThreshold> thresholdsRelativeFlow = Collections.singleton(thresholdRelativeFlow);
        // CNECs
        SimpleCnec cnec1basecase = new SimpleCnec("cnec1basecase", "", monitoredElement1, thresholdsAbsFlow, stateBasecase);
        SimpleCnec cnec1stateCurativeContingency1 = new SimpleCnec("cnec1stateCurativeContingency1", "", monitoredElement1, thresholdsAbsFlow, stateCurativeContingency1);
        SimpleCnec cnec1stateCurativeContingency2 = new SimpleCnec("cnec1stateCurativeContingency2", "", monitoredElement1, thresholdsAbsFlow, stateCurativeContingency2);
        cnec1basecase.setThresholds(thresholdsAbsFlow);
        cnec1stateCurativeContingency1.setThresholds(thresholdsAbsFlow);
        cnec1stateCurativeContingency2.setThresholds(thresholdsAbsFlow);

        SimpleCnec cnec2basecase = new SimpleCnec("cnec2basecase", "", monitoredElement2, thresholdsAbsFlow, stateBasecase);
        SimpleCnec cnec2stateCurativeContingency1 = new SimpleCnec("cnec2stateCurativeContingency1", "", monitoredElement2, thresholdsAbsFlow, stateCurativeContingency1);
        SimpleCnec cnec2stateCurativeContingency2 = new SimpleCnec("cnec2stateCurativeContingency2", "", monitoredElement2, thresholdsAbsFlow, stateCurativeContingency2);
        cnec2basecase.setThresholds(thresholdsRelativeFlow);
        cnec2stateCurativeContingency1.setThresholds(thresholdsRelativeFlow);
        cnec2stateCurativeContingency2.setThresholds(thresholdsRelativeFlow);

        crac.addCnec(cnec1basecase);
        crac.addCnec(cnec1stateCurativeContingency1);
        crac.addCnec(cnec1stateCurativeContingency2);
        crac.addCnec(cnec2basecase);
        crac.addCnec(cnec2stateCurativeContingency1);
        crac.addCnec(cnec2stateCurativeContingency2);

        return crac;
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
