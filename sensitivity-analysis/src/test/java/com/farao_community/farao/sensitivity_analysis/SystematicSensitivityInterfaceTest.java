/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.json.SensitivityJson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystematicSensitivityAdapter.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class SystematicSensitivityInterfaceTest {

    private static final double FLOW_TOLERANCE = 0.1;

    private Crac crac;
    private Network network;
    private SystematicSensitivityResult systematicAnalysisResultOk;
    private SystematicSensitivityResult systematicAnalysisResultFailed;
    private SensitivityAnalysisParameters defaultParameters;
    private SensitivityAnalysisParameters fallbackParameters;
    private ComputationManager computationManager;

    @Before
    public void setUp() {

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        PowerMockito.mockStatic(SystematicSensitivityAdapter.class);
        computationManager = Mockito.mock(ComputationManager.class);

        try {
            defaultParameters = SensitivityJson.createObjectMapper().readValue(getClass().getResourceAsStream("/DefaultSensitivityComputationParameters.json"), SensitivityAnalysisParameters.class);
            fallbackParameters = SensitivityJson.createObjectMapper().readValue(getClass().getResourceAsStream("/FallbackSystematicSensitivityComputationParameters.json"), SensitivityAnalysisParameters.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRunDefaultConfigOk() {
        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultOk);

        // run engine
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(defaultParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .build();
        SystematicSensitivityResult systematicSensitivityAnalysisResult = systematicSensitivityInterface.run(network);

        // assert results
        assertNotNull(systematicSensitivityAnalysisResult);
        assertFalse(systematicSensitivityInterface.isFallback());
        for (Cnec cnec: crac.getFlowCnecs()) {
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
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultFailed);

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(defaultParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .build();

        // run - expected failure
        try {
            systematicSensitivityInterface.run(network);
            fail();
        } catch (SensitivityAnalysisException e) {
            assertTrue(e.getMessage().contains("Sensitivity analysis failed with default parameters. No fallback parameters available."));
        }
    }

    @Test
    public void testRunDefaultConfigFailsButFallbackOk() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultOk);

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(defaultParameters)
            .withFallbackParameters(fallbackParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .build();

        // run
        SystematicSensitivityResult systematicSensitivityAnalysisResult = systematicSensitivityInterface.run(network);

        // assert results
        assertNotNull(systematicSensitivityAnalysisResult);
        assertTrue(systematicSensitivityInterface.isFallback());
        for (Cnec cnec: crac.getFlowCnecs()) {
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
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(defaultParameters)))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultFailed);

        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), ArgumentMatchers.eq(fallbackParameters)))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultFailed);

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withDefaultParameters(defaultParameters)
            .withFallbackParameters(fallbackParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .build();

        // run - expected failure
        try {
            systematicSensitivityInterface.run(network);
            fail();
        } catch (SensitivityAnalysisException e) {
            assertTrue(e.getMessage().contains("Sensitivity analysis failed with all available sensitivity parameters."));
        }
    }

    private SystematicSensitivityResult buildSystematicAnalysisResultOk() {
        Random random = new Random();
        random.setSeed(42L);
        SystematicSensitivityResult result = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(result.isSuccess()).thenReturn(true);
        crac.getFlowCnecs().forEach(cnec -> {
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

    private SystematicSensitivityResult buildSystematicAnalysisResultFailed() {
        SystematicSensitivityResult result = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(result.isSuccess()).thenReturn(false);
        return result;
    }
}
