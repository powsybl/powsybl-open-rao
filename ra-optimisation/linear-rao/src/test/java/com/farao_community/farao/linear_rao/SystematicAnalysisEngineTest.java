/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class SystematicAnalysisEngineTest {

    private static final double FLOW_TOLERANCE = 0.1;

    private Crac crac;
    private LinearRaoData initialLinearRaoData;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultOk;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultFailed;
    private LinearRaoParameters linearRaoParameters;

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        initialLinearRaoData = new LinearRaoData(network, crac);
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);

        linearRaoParameters = new LinearRaoParameters();
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.setRaoWithLoopFlowLimitation(false);
        raoParameters.addExtension(LinearRaoParameters.class, linearRaoParameters);
    }

    @Test
    public void testRunDefaultConfigOkWithMinMarginInMegawatt() {

        linearRaoParameters.setObjectiveFunction(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultOk);

        // run engine
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(linearRaoParameters, computationManager);
        systematicAnalysisEngine.run(initialLinearRaoData);

        // assert results
        assertNotNull(initialLinearRaoData);
        assertFalse(systematicAnalysisEngine.isFallback());
        String resultVariant = initialLinearRaoData.getWorkingVariantId();
        assertEquals(1400.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        assertEquals(2000.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);

        // cnec2basebase is the limiting cnec, with a threshold of 1500 A (= 1500 * 380 * sqrt(3) / 1000 MW)
        assertEquals(-((1500 * 380 * Math.sqrt(3) / 1000) - 1400.0), initialLinearRaoData.getCracResult(resultVariant).getCost(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigOkWithMinMarginInAmpere() {

        linearRaoParameters.setObjectiveFunction(LinearRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE);
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultOk);

        // run engine
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(linearRaoParameters, computationManager);
        systematicAnalysisEngine.run(initialLinearRaoData);

        // assert results
        assertNotNull(initialLinearRaoData);
        assertFalse(systematicAnalysisEngine.isFallback());
        String resultVariant = initialLinearRaoData.getWorkingVariantId();
        assertEquals(1400.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        assertEquals(2000.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);

        // cnec2basebase is the limiting cnec, with a threshold of 1500 A (= 1500 * 380 * sqrt(3) / 1000 MW)
        assertEquals(-(1500.0 - 2000.0), initialLinearRaoData.getCracResult(resultVariant).getCost(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigFailsAndNoFallback() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        // run - expected failure
        try {
            systematicAnalysisEngine.run(initialLinearRaoData);
            fail();
        } catch (SensitivityComputationException e) {
            assertTrue(e.getMessage().contains("Sensitivity computation failed with default parameters. No fallback parameters available."));
        }
    }

    @Test
    public void testRunDefaultConfigFailsButFallbackOk() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersWithFallback.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        SensitivityComputationParameters defaultConfig = raoParameters.getExtension(LinearRaoParameters.class).getSensitivityComputationParameters();
        SensitivityComputationParameters fallbackConfig = raoParameters.getExtension(LinearRaoParameters.class).getFallbackSensiParameters();

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(defaultConfig)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(fallbackConfig)))
            .thenReturn(systematicAnalysisResultOk);

        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        // run
        systematicAnalysisEngine.run(initialLinearRaoData);

        // assert
        assertTrue(systematicAnalysisEngine.isFallback());
        String resultVariant = initialLinearRaoData.getWorkingVariantId();
        assertEquals(1400.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        assertEquals(2000.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigAndFallbackFail() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersWithFallback.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        SensitivityComputationParameters defaultConfig = raoParameters.getExtension(LinearRaoParameters.class).getSensitivityComputationParameters();
        SensitivityComputationParameters fallbackConfig = raoParameters.getExtension(LinearRaoParameters.class).getFallbackSensiParameters();

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(defaultConfig)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(fallbackConfig)))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        // run - expected failure
        try {
            systematicAnalysisEngine.run(initialLinearRaoData);
            fail();
        } catch (SensitivityComputationException e) {
            assertTrue(e.getMessage().contains("Sensitivity computation failed with all available sensitivity parameters."));
        }
    }

    private SystematicSensitivityAnalysisResult buildSystematicAnalysisResultOk() {
        Random random = new Random();
        random.setSeed(42L);
        SystematicSensitivityAnalysisResult result = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        Mockito.when(result.isSuccess()).thenReturn(true);
        crac.getCnecs().forEach(cnec -> {
            if (cnec.getId().equals("cnec2basecase")) {
                Mockito.when(result.getReferenceFlow(cnec)).thenReturn(1400.);
                Mockito.when(result.getReferenceIntensity(cnec)).thenReturn(2000.);
                crac.getRangeActions().forEach(rangeAction -> {
                    Mockito.when(result.getSensitivityOnFlow(rangeAction, cnec)).thenReturn(random.nextDouble());
                    Mockito.when(result.getSensitivityOnIntensity(rangeAction, cnec)).thenReturn(random.nextDouble());
                });
            } else {
                Mockito.when(result.getReferenceFlow(cnec)).thenReturn(0.0);
                Mockito.when(result.getReferenceIntensity(cnec)).thenReturn(0.0);
                crac.getRangeActions().forEach(rangeAction -> {
                    Mockito.when(result.getSensitivityOnFlow(rangeAction, cnec)).thenReturn(random.nextDouble());
                    Mockito.when(result.getSensitivityOnIntensity(rangeAction, cnec)).thenReturn(random.nextDouble());
                });
            }
        });
        return result;
    }

    private SystematicSensitivityAnalysisResult buildSystematicAnalysisResultFailed() {
        SystematicSensitivityAnalysisResult result = Mockito.mock(SystematicSensitivityAnalysisResult.class);
        Mockito.when(result.isSuccess()).thenReturn(false);
        return result;
    }

    @Test
    public void testLoopflowRelated() {
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();
        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersWithFallback.json"));
        SensitivityComputationParameters defaultConfig = raoParameters.getExtension(LinearRaoParameters.class).getSensitivityComputationParameters();
        SensitivityComputationParameters fallbackConfig = raoParameters.getExtension(LinearRaoParameters.class).getFallbackSensiParameters();
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(defaultConfig)))
                .thenReturn(systematicAnalysisResultFailed);
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(fallbackConfig)))
                .thenReturn(systematicAnalysisResultOk);
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);
        systematicAnalysisEngine.setLoopflowViolation(true);
        assertTrue(systematicAnalysisEngine.isLoopflowViolation());
        systematicAnalysisEngine.run(initialLinearRaoData);
        assertEquals(Double.MAX_VALUE, initialLinearRaoData.getCracResult().getVirtualCost(), 1.0);
    }
}
