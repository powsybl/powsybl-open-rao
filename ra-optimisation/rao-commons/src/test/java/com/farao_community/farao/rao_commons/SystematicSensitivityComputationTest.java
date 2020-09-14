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
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
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
    private SensitivityComputationParameters defaultParameters;
    private SensitivityComputationParameters fallbackParameters;

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        initialRaoData = new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()));
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);

        defaultParameters = JsonSensitivityComputationParameters.read(getClass().getResourceAsStream("/DefaultSensitivityComputationParameters.json"));
        fallbackParameters = JsonSensitivityComputationParameters.read(getClass().getResourceAsStream("/FallbackSystematicSensitivityComputationParameters.json"));
    }

    @Test
    public void testRunDefaultConfigOk() {
        // mock sensi service - run OK
        BDDMockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultOk);

        // run engine
        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(defaultParameters);
        systematicSensitivityComputation.run(initialRaoData);
        initialRaoData.getRaoDataManager().updateCnecExtensions();

        // assert results
        assertNotNull(initialRaoData);
        assertFalse(systematicSensitivityComputation.isFallback());
        String resultVariant = initialRaoData.getWorkingVariantId();
        Assert.assertEquals(1400, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        Assert.assertEquals(2000, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigFailsAndNoFallback() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(defaultParameters);

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
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenReturn(systematicAnalysisResultOk);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            defaultParameters, fallbackParameters);

        // run
        systematicSensitivityComputation.run(initialRaoData);
        initialRaoData.getRaoDataManager().updateCnecExtensions();

        // assert
        assertTrue(systematicSensitivityComputation.isFallback());
        String resultVariant = initialRaoData.getWorkingVariantId();
        Assert.assertEquals(1400, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        Assert.assertEquals(2000, initialRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigAndFallbackFail() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            defaultParameters, fallbackParameters);

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
