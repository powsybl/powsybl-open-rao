/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.util.SensitivityComputationException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class})
public class LinearRaoTest {

    private LinearRao linearRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;
    private SystematicAnalysisEngine systematicAnalysisEngine;
    private LinearOptimisationEngine linearOptimisationEngine;
    private Network network;
    private Crac crac;
    private String variantId;

    @Before
    public void setUp() {
        mockNativeLibraryLoader();

        linearRao = Mockito.spy(LinearRao.class);

        crac = CracImporters.importCrac("small-crac.json", getClass().getResourceAsStream("/small-crac.json"));
        network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        variantId = network.getVariantManager().getWorkingVariantId();
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        raoParameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(false);
        computationManager = LocalComputationManager.getDefault();

        systematicAnalysisEngine = Mockito.mock(SystematicAnalysisEngine.class);
        linearOptimisationEngine = Mockito.mock(LinearOptimisationEngine.class);
    }

    @Test
    public void getName() {
        assertEquals("LinearRao", linearRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", linearRao.getVersion());
    }

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        PowerMockito.doNothing().when(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void runLinearRaoWithSensitivityComputationError() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(systematicAnalysisEngine).run(any());
        try {
            linearRao.runLinearRao(network, crac, variantId, systematicAnalysisEngine, linearOptimisationEngine, raoParameters).join();
            fail();
        } catch (SensitivityComputationException e) {
            // should throw
        }
    }

    @Test
    public void runWithSensitivityComputationException() {
        Mockito.doThrow(new SensitivityComputationException("error with sensi")).when(linearRao).runLinearRao(any(), any(), anyString(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE, results.getExtension(LinearRaoResult.class).getSystematicSensitivityAnalysisStatus());
    }

    @Test
    public void runLinearRaoWithLinearOptimisationError() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        try {
            linearRao.runLinearRao(network, crac, variantId, systematicAnalysisEngine, linearOptimisationEngine, raoParameters).join();
            fail();
        } catch (LinearOptimisationException e) {
            // should throw
        }
    }

    @Test
    public void runWithLinearOptimisationException() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearRao).runLinearRao(any(), any(), anyString(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
        assertEquals(LinearRaoResult.LpStatus.FAILURE, results.getExtension(LinearRaoResult.class).getLpStatus());
    }

    @Test
    public void runWithFaraoException() {
        Mockito.doThrow(new FaraoException("farao exception")).when(linearRao).runLinearRao(any(), any(), anyString(), any(), any(), any());
        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }

    @Test
    public void runWithRaoParametersError() {
        raoParameters.removeExtension(LinearRaoParameters.class);

        RaoResult results = linearRao.run(network, crac, variantId, computationManager, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.FAILURE, results.getStatus());
        assertNotNull(results.getExtension(LinearRaoResult.class));
    }

    @Test
    public void runLinearRaoSecurityAnalysisWithoutRao() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        raoParameters.getExtension(LinearRaoParameters.class).setSecurityAnalysisWithoutRao(true);

        RaoResult results = linearRao.runLinearRao(network, crac, variantId, systematicAnalysisEngine, linearOptimisationEngine, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }

    @Test
    public void runLinearRaoSecurityAnalysisWithZeroIteration() {
        Mockito.doThrow(new LinearOptimisationException("error with optim")).when(linearOptimisationEngine).run(any());
        raoParameters.getExtension(LinearRaoParameters.class).setMaxIterations(0);

        RaoResult results = linearRao.runLinearRao(network, crac, variantId, systematicAnalysisEngine, linearOptimisationEngine, raoParameters).join();

        assertNotNull(results);
        assertEquals(RaoResult.Status.SUCCESS, results.getStatus());
    }

    @Test
    public void runTest() {
        /*Network network = NetworkImportsUtil.import12NodesNetwork();
        SimpleCrac crac = create();
        crac.synchronize(network);
        String variantId = "variant-test";

        ResultVariantManager variantManager = new ResultVariantManager();
        ResultVariantManager variantManagerSpy = Mockito.spy(variantManager);
        crac.addExtension(ResultVariantManager.class, variantManagerSpy);
        variantManagerSpy.createVariant("preOptimVariant");
        variantManagerSpy.createVariant("currentVariant1");
        variantManagerSpy.createVariant("currentVariant2");
        variantManagerSpy.createVariant("currentVariant3");
        Mockito.doReturn("preOptimVariant").doReturn("currentVariant1").doReturn("currentVariant2").doReturn("currentVariant3")
                .when(variantManagerSpy).createNewUniqueVariantId(Mockito.anyString());

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
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap1, new HashMap<>()),
                            new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap2, new HashMap<>()),
                            new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap3, new HashMap<>()));

        CompletableFuture<RaoResult> linearRaoResultCF = linearRao.run(network, crac, variantId, LocalComputationManager.getDefault(), raoParameters);

        assertNotNull(linearRaoResultCF);
        try {
            RaoResult linearRaoResult = linearRaoResultCF.get();
            assertTrue(linearRaoResult.isSuccessful());
            assertEquals("preOptimVariant", linearRaoResult.getPreOptimVariantId());
            assertEquals("currentVariant2", linearRaoResult.getPostOptimVariantId());

            CnecResultExtension cnecResultMap = crac.getCnecs().iterator().next().getExtension(CnecResultExtension.class);
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
        }*/
    }
}
