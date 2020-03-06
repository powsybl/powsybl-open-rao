/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

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
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.ra_optimisation.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class LinearRaoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRaoTest.class);

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;

    @Before
    public void setUp() {
        linearRao = new LinearRao();
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

        computationManager = LocalComputationManager.getDefault();
        raoParameters = RaoParameters.load(platformConfig);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        Mockito.when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Test
    public void getName() {
        assertEquals("LinearRao", linearRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", linearRao.getVersion());
    }

    @Test
    public void testBrokenParameters() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

        RaoParameters brokenParameters = RaoParameters.load(platformConfig);
        brokenParameters.removeExtension(LinearRaoParameters.class);

        boolean errorCaught = false;
        try {
            linearRao.run(Mockito.mock(Network.class), Mockito.mock(Crac.class), "", computationManager, brokenParameters);
        } catch (FaraoException e) {
            errorCaught = true;
            assertEquals("There are some issues in RAO parameters:" + System.lineSeparator() +
                    "Linear Rao parameters not available", e.getMessage());
        }
        assertTrue(errorCaught);
    }

    @Test
    public void testBrokenSensi() {
        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        stateSensiMap.put(new SimpleState(Optional.empty(), new Instant("myInstant", 0)), null);
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new SystematicSensitivityAnalysisResult(stateSensiMap, new HashMap<>(), new HashMap<>()));
        RaoComputationResult result;
        try {
            result = linearRao.run(Mockito.mock(Network.class), Mockito.mock(Crac.class), "", computationManager, raoParameters).get();
            assertEquals(RaoComputationResult.Status.FAILURE, result.getStatus());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void runTest() {
        Network network = Importers.loadNetwork(
                "TestCase12Nodes.uct",
                getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        Crac crac = create();
        crac.synchronize(network);
        String variantId = "variant-test";

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecMarginMap1 = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMarginMap1.put(cnec, 1.0));
        Map<Cnec, Double> cnecMarginMap2 = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMarginMap2.put(cnec, 5.0));
        Map<Cnec, Double> cnecMarginMap3 = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMarginMap3.put(cnec, 10.0));
        Map<Cnec, Double> cnecMaxThresholdMap = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecMaxThresholdMap.put(cnec, 500.));
        Map<Cnec, Double> cnecFlowMap1 = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap2 = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap3 = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecFlowMap1.put(cnec, 499.));
        crac.getCnecs().forEach(cnec -> cnecFlowMap2.put(cnec, 495.));
        crac.getCnecs().forEach(cnec -> cnecFlowMap3.put(cnec, 490.));
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap1, new HashMap<>()),
                            new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap2, new HashMap<>()),
                            new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap3, new HashMap<>()));

        List<MonitoredBranchResult> emptyMonitoredBranchResultList = new ArrayList<>();

        List<RemedialActionResult> remedialActionResults1 = new ArrayList<>();
        List<RemedialActionElementResult> remedialActionElementResultList1 = new ArrayList<>();
        remedialActionElementResultList1.add(new PstElementResult("BBE2AA1  BBE3AA1  1", 1., 2, 3., 4));
        remedialActionResults1.add(new RemedialActionResult("RA PST BE", "RA PST BE name", true, remedialActionElementResultList1));
        PreContingencyResult preContingencyResult1 = new PreContingencyResult(emptyMonitoredBranchResultList, remedialActionResults1);
        RaoComputationResult raoComputationResult1 = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preContingencyResult1);

        List<RemedialActionResult> remedialActionResults2 = new ArrayList<>();
        List<RemedialActionElementResult> remedialActionElementResultList2 = new ArrayList<>();
        remedialActionElementResultList2.add(new PstElementResult("BBE2AA1  BBE3AA1  1", 1., 2, 2., 3));
        remedialActionResults2.add(new RemedialActionResult("RA PST BE", "RA PST BE name", true, remedialActionElementResultList2));
        PreContingencyResult preContingencyResult2 = new PreContingencyResult(emptyMonitoredBranchResultList, remedialActionResults2);
        RaoComputationResult raoComputationResult2 = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preContingencyResult2);

        LinearRaoModeller linearRaoModellerMock = Mockito.mock(LinearRaoModeller.class);
        Mockito.when(linearRaoModellerMock.solve()).thenReturn(raoComputationResult1, raoComputationResult2);

        LinearRao linearRaoSpy = Mockito.spy(linearRao);
        Mockito.doReturn(linearRaoModellerMock).when(linearRaoSpy).createLinearRaoModeller(Mockito.any(), Mockito.any(), Mockito.any());
        CompletableFuture<RaoComputationResult> linearRaoResultCF = linearRaoSpy.run(network, crac, variantId, LocalComputationManager.getDefault(), raoParameters);
        assertNotNull(linearRaoResultCF);
        try {
            RaoComputationResult linearRaoResult = linearRaoResultCF.get();
            PreContingencyResult preContingencyResult = linearRaoResult.getPreContingencyResult();
            assertEquals(490, preContingencyResult.getMonitoredBranchResults().get(0).getPostOptimisationFlow(), .1);
            assertEquals(499, preContingencyResult.getMonitoredBranchResults().get(0).getPreOptimisationFlow(), .1);

            assertEquals(1, preContingencyResult.getRemedialActionResults().size());
            assertEquals("RA PST BE", preContingencyResult.getRemedialActionResults().get(0).getId());
            RemedialActionElementResult remedialActionElementResult = preContingencyResult.getRemedialActionResults().get(0).getRemedialActionElementResults().get(0);
            assertTrue(remedialActionElementResult instanceof PstElementResult);
            PstElementResult pstElementResult = (PstElementResult) remedialActionElementResult;
            assertEquals("BBE2AA1  BBE3AA1  1", pstElementResult.getId());
            assertEquals(1., pstElementResult.getPreOptimisationAngle(), 0.01);
            assertEquals(2, pstElementResult.getPreOptimisationTapPosition());
            assertEquals(2., pstElementResult.getPostOptimisationAngle(), 0.01);
            assertEquals(3, pstElementResult.getPostOptimisationTapPosition());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
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
        AbsoluteFlowThreshold thresholdAbsFlow = new AbsoluteFlowThreshold(Unit.MEGAWATT, Side.LEFT, Direction.BOTH, 1500);
        RelativeFlowThreshold thresholdRelativeFlow = new RelativeFlowThreshold(Side.LEFT, Direction.BOTH, 30);

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

        // RAs
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        crac.addRangeAction(pstRange);

        return crac;
    }
}
