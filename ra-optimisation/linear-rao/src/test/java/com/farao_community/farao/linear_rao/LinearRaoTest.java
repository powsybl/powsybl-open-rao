/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.ra_optimisation.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
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
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean()))
                .thenReturn(new SystematicSensitivityAnalysisResult(stateSensiMap, new HashMap<>(), new HashMap<>()));
        RaoComputationResult result;
        Crac crac = new SimpleCrac("myCrac");
        try {
            result = linearRao.run(Mockito.mock(Network.class), crac, "", computationManager, raoParameters).get();
            assertEquals(RaoComputationResult.Status.FAILURE, result.getStatus());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void runTest() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        SimpleCrac crac = create();
        crac.synchronize(network);
        String variantId = "variant-test";

        ResultVariantManager variantManager = new ResultVariantManager();
        ResultVariantManager variantManagerSpy = Mockito.spy(variantManager);
        crac.addExtension(ResultVariantManager.class, variantManagerSpy);
        variantManagerSpy.createVariant("preOptimVariant");
        variantManagerSpy.createVariant("postOptimVariant");
        variantManagerSpy.createVariant("currentVariant1");
        variantManagerSpy.createVariant("currentVariant2");
        variantManagerSpy.createVariant("currentVariant3");
        Mockito.doReturn("preOptimVariant").doReturn("postOptimVariant").doReturn("currentVariant1").doReturn("currentVariant2").doReturn("currentVariant3")
                .when(variantManagerSpy).createNewUniqueVariantId();

        String preventiveState = crac.getPreventiveState().getId();
        RangeActionResultExtension rangeActionResultMap;
        rangeActionResultMap = crac.getRangeAction("RA PST BE").getExtension(RangeActionResultExtension.class);
        PstRangeResult currentVariant1 = (PstRangeResult) rangeActionResultMap.getVariant("currentVariant1");
        currentVariant1.setSetPoint(preventiveState, 3);
        currentVariant1.setTap(preventiveState, 4);
        PstRangeResult currentVariant2 = (PstRangeResult) rangeActionResultMap.getVariant("currentVariant2");
        currentVariant2.setSetPoint(preventiveState, 2);
        currentVariant2.setTap(preventiveState, 3);
        PstRangeResult currentVariant3 = (PstRangeResult) rangeActionResultMap.getVariant("currentVariant3");
        currentVariant3.setSetPoint(preventiveState, 2);
        currentVariant3.setTap(preventiveState, 3);

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap1 = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap2 = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap3 = new HashMap<>();
        crac.getCnecs().forEach(cnec -> cnecFlowMap1.put(cnec, 499.));
        crac.getCnecs().forEach(cnec -> cnecFlowMap2.put(cnec, 495.));
        crac.getCnecs().forEach(cnec -> cnecFlowMap3.put(cnec, 490.));
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean()))
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
        Mockito.when(linearRaoModellerMock.solve(Mockito.any())).thenReturn(raoComputationResult1, raoComputationResult2);

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

            ResultExtension<Cnec, CnecResult> cnecResultMap = crac.getCnecs().iterator().next().getExtension(CnecResultExtension.class);
            assertEquals(499, cnecResultMap.getVariant("preOptimVariant").getFlowInMW(), 0.01);
            assertEquals(490, cnecResultMap.getVariant("currentVariant2").getFlowInMW(), 0.01);
            RangeActionResultExtension pstResultMap = crac.getRangeAction("RA PST BE").getExtension(RangeActionResultExtension.class);
            assertEquals(0, ((PstRangeResult) pstResultMap.getVariant("preOptimVariant")).getTap(preventiveState));
            assertEquals(0., pstResultMap.getVariant("preOptimVariant").getSetPoint(preventiveState), 0.01);
            assertEquals(3, ((PstRangeResult) pstResultMap.getVariant("currentVariant2")).getTap(preventiveState));
            assertEquals(2., pstResultMap.getVariant("currentVariant2").getSetPoint(preventiveState), 0.01);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static SimpleCrac create() {
        SimpleCrac crac = CommonCracCreation.create();

        // RAs
        NetworkElement pstElement = new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name");
        PstRange pstRange = new PstWithRange("RA PST BE", pstElement);
        crac.addRangeAction(pstRange);

        return crac;
    }
}
