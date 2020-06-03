/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputationParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.iidm.network.Network;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAnalysisService.class})
public class SystematicSensitivityComputationTest {

    private static final double FLOW_TOLERANCE = 0.1;

    private Crac crac;
    private RaoData initialRaoData;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultOk;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultFailed;
    private RaoParameters raoParameters;
    private RaoParameters raoParametersWithFallback;

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        initialRaoData = new RaoData(network, crac);
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);

        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/SystematicSensitivityComputationParameters.json"));
        raoParametersWithFallback = JsonRaoParameters.read(getClass().getResourceAsStream("/SystematicSensitivityComputationParametersWithFallback.json"));
    }

    @Test
    public void testRunDefaultConfigOk() {
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultOk);

        // run engine
        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParameters.getExtension(SystematicSensitivityComputationParameters.class), computationManager);
        systematicSensitivityComputation.run(initialRaoData);

        // assert results
        assertNotNull(initialRaoData);
        assertFalse(systematicSensitivityComputation.isFallback());
        String resultVariant = initialRaoData.getWorkingVariantId();
        Assert.assertEquals(10.0, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        Assert.assertEquals(15.0, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigFailsAndNoFallback() {
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParameters.getExtension(SystematicSensitivityComputationParameters.class), computationManager);

        // run - expected failure
        try {
            systematicSensitivityComputation.run(initialRaoData);
            fail();
        } catch (SensitivityComputationException e) {
            assertTrue(e.getMessage().contains("Sensitivity computation failed with default parameters. No fallback parameters available."));
        }
    }

    @Test
    public void testRunDefaultConfigFailsButFallbackOk() {
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class).getDefaultParameters())))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class).getFallbackParameters())))
            .thenReturn(systematicAnalysisResultOk);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class), computationManager);

        // run
        systematicSensitivityComputation.run(initialRaoData);

        // assert
        assertTrue(systematicSensitivityComputation.isFallback());
        String resultVariant = initialRaoData.getWorkingVariantId();
        Assert.assertEquals(10.0, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        Assert.assertEquals(15.0, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigAndFallbackFail() {
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class).getDefaultParameters())))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class).getFallbackParameters())))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParametersWithFallback.getExtension(SystematicSensitivityComputationParameters.class), computationManager);

        // run - expected failure
        try {
            systematicSensitivityComputation.run(initialRaoData);
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
}
