/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.json.JsonSensitivityComputationParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
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
public class SystematicSensitivityAnalysisInterfaceTest {

    private static final double FLOW_TOLERANCE = 0.1;

    private Crac crac;
    private Network network;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultOk;
    private SystematicSensitivityAnalysisResult systematicAnalysisResultFailed;
    private SensitivityComputationParameters defaultParameters;
    private SensitivityComputationParameters fallbackParameters;

    @Before
    public void setUp() {

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);

        defaultParameters = JsonSensitivityComputationParameters.read(getClass().getResourceAsStream("/DefaultSensitivityComputationParameters.json"));
        fallbackParameters = JsonSensitivityComputationParameters.read(getClass().getResourceAsStream("/FallbackSystematicSensitivityComputationParameters.json"));
    }

    @Test
    public void testRunDefaultConfigOk() {
        // mock sensi service - run OK
        BDDMockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultOk);

        // run engine
        SystematicSensitivityAnalysisInterface systematicSensitivityAnalysisInterface = new SystematicSensitivityAnalysisInterface(defaultParameters);
        systematicSensitivityAnalysisInterface.setSensitivityProvider(Mockito.mock(SensitivityProvider.class));
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = systematicSensitivityAnalysisInterface.run(network, crac);

        // assert results
        assertNotNull(systematicSensitivityAnalysisResult);
        assertFalse(systematicSensitivityAnalysisInterface.isFallback());
        for (Cnec cnec: crac.getCnecs()) {
            if (cnec.getId().equals("cnec2basecase")) {
                assertEquals(1400., systematicSensitivityAnalysisResult.getReferenceFlow(cnec), FLOW_TOLERANCE);
                assertEquals(2000., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec), FLOW_TOLERANCE);
            } else {
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceFlow(cnec), FLOW_TOLERANCE);
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec), FLOW_TOLERANCE);
            }
        }
    }

    @Test
    public void testRunDefaultConfigFailsAndNoFallback() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityAnalysisInterface systematicSensitivityAnalysisInterface = new SystematicSensitivityAnalysisInterface(defaultParameters);
        systematicSensitivityAnalysisInterface.setSensitivityProvider(Mockito.mock(SensitivityProvider.class));

        // run - expected failure
        try {
            systematicSensitivityAnalysisInterface.run(network, crac);
            fail();
        } catch (SensitivityComputationException e) {
            assertTrue(e.getMessage().contains("Sensitivity computation failed with default parameters. No fallback parameters available."));
        }
    }

    @Test
    public void testRunDefaultConfigFailsButFallbackOk() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenReturn(systematicAnalysisResultOk);

        SystematicSensitivityAnalysisInterface systematicSensitivityAnalysisInterface = new SystematicSensitivityAnalysisInterface(
            defaultParameters, fallbackParameters);
        systematicSensitivityAnalysisInterface.setSensitivityProvider(Mockito.mock(SensitivityProvider.class));

        // run
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = systematicSensitivityAnalysisInterface.run(network, crac);

        // assert results
        assertNotNull(systematicSensitivityAnalysisResult);
        assertTrue(systematicSensitivityAnalysisInterface.isFallback());
        for (Cnec cnec: crac.getCnecs()) {
            if (cnec.getId().equals("cnec2basecase")) {
                assertEquals(1400., systematicSensitivityAnalysisResult.getReferenceFlow(cnec), FLOW_TOLERANCE);
                assertEquals(2000., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec), FLOW_TOLERANCE);
            } else {
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceFlow(cnec), FLOW_TOLERANCE);
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec), FLOW_TOLERANCE);
            }
        }
    }

    @Test
    public void testRunDefaultConfigAndFallbackFail() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAnalysisService.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenReturn(systematicAnalysisResultFailed);

        SystematicSensitivityAnalysisInterface systematicSensitivityAnalysisInterface = new SystematicSensitivityAnalysisInterface(
            defaultParameters, fallbackParameters);
        systematicSensitivityAnalysisInterface.setSensitivityProvider(Mockito.mock(SensitivityProvider.class));

        // run - expected failure
        try {
            systematicSensitivityAnalysisInterface.run(network, crac);
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
