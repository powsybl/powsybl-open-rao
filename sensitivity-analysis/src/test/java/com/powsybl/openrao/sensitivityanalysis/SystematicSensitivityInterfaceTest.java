/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SystematicSensitivityInterfaceTest {

    private static final double FLOW_TOLERANCE = 0.1;

    private Crac crac;
    private Network network;
    private SystematicSensitivityResult systematicAnalysisResultOk;
    private SystematicSensitivityResult systematicAnalysisResultFailed;
    private SensitivityAnalysisParameters defaultParameters;

    private MockedStatic<SystematicSensitivityAdapter> systematicSensitivityAdapterMockedStatic;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() {

        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        outageInstant = crac.getInstant("outage");
        systematicAnalysisResultOk = buildSystematicAnalysisResultOk();
        systematicAnalysisResultFailed = buildSystematicAnalysisResultFailed();

        systematicSensitivityAdapterMockedStatic = Mockito.mockStatic(SystematicSensitivityAdapter.class);
        try {
            defaultParameters = JsonSensitivityAnalysisParameters.createObjectMapper().readValue(getClass().getResourceAsStream("/DefaultSensitivityComputationParameters.json"), SensitivityAnalysisParameters.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        systematicSensitivityAdapterMockedStatic.close();
    }

    @Test
    void testRunDefaultConfigOk() {
        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultOk);

        // run engine
        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName("default-impl-name")
            .withParameters(defaultParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .withOutageInstant(outageInstant)
            .build();
        SystematicSensitivityResult systematicSensitivityAnalysisResult = systematicSensitivityInterface.run(network);

        // assert results
        assertNotNull(systematicSensitivityAnalysisResult);
        for (FlowCnec cnec : crac.getFlowCnecs()) {
            if (cnec.getId().equals("cnec2basecase")) {
                assertEquals(1400., systematicSensitivityAnalysisResult.getReferenceFlow(cnec, Side.LEFT), FLOW_TOLERANCE);
                assertEquals(2800., systematicSensitivityAnalysisResult.getReferenceFlow(cnec, Side.RIGHT), FLOW_TOLERANCE);
                assertEquals(2000., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec, Side.LEFT), FLOW_TOLERANCE);
                assertEquals(4000., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec, Side.RIGHT), FLOW_TOLERANCE);
            } else {
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceFlow(cnec, Side.LEFT), FLOW_TOLERANCE);
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceFlow(cnec, Side.RIGHT), FLOW_TOLERANCE);
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec, Side.LEFT), FLOW_TOLERANCE);
                assertEquals(0., systematicSensitivityAnalysisResult.getReferenceIntensity(cnec, Side.RIGHT), FLOW_TOLERANCE);
            }
        }
    }

    @Test
    void testRunDefaultConfigFails() {
        // mock sensi service - run with null sensi
        Mockito.when(SystematicSensitivityAdapter.runSensitivity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocationOnMock -> systematicAnalysisResultFailed);

        SystematicSensitivityInterface systematicSensitivityInterface = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName("default-impl-name")
            .withParameters(defaultParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class))
            .withOutageInstant(outageInstant)
            .build();

        // run - expected failure
        SystematicSensitivityResult result = systematicSensitivityInterface.run(network);
        assertFalse(result.isSuccess());
    }

    private SystematicSensitivityResult buildSystematicAnalysisResultOk() {
        Random random = new Random();
        random.setSeed(42L);
        SystematicSensitivityResult result = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(result.isSuccess()).thenReturn(true);
        crac.getFlowCnecs().forEach(cnec -> {
            if (cnec.getId().equals("cnec2basecase")) {
                Mockito.when(result.getReferenceFlow(cnec, Side.LEFT)).thenReturn(1400.);
                Mockito.when(result.getReferenceFlow(cnec, Side.RIGHT)).thenReturn(2800.);
                Mockito.when(result.getReferenceIntensity(cnec, Side.LEFT)).thenReturn(2000.);
                Mockito.when(result.getReferenceIntensity(cnec, Side.RIGHT)).thenReturn(4000.);
                crac.getRangeActions().forEach(rangeAction -> Mockito.when(result.getSensitivityOnFlow(Mockito.eq(rangeAction), Mockito.eq(cnec), Mockito.any())).thenReturn(random.nextDouble()));
            } else {
                Mockito.when(result.getReferenceFlow(Mockito.eq(cnec), Mockito.any())).thenReturn(0.0);
                Mockito.when(result.getReferenceIntensity(Mockito.eq(cnec), Mockito.any())).thenReturn(0.0);
                crac.getRangeActions().forEach(rangeAction -> Mockito.when(result.getSensitivityOnFlow(Mockito.eq(rangeAction), Mockito.eq(cnec), Mockito.any())).thenReturn(random.nextDouble()));
            }
        });
        return result;
    }

    private SystematicSensitivityResult buildSystematicAnalysisResultFailed() {
        SystematicSensitivityResult result = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(result.isSuccess()).thenReturn(false);
        return result;
    }

    @Test
    void testCannotBuildSystematicInterfaceWithoutOutageInstant() {
        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder systematicSensitivityInterfaceBuilder = SystematicSensitivityInterface.builder()
            .withSensitivityProviderName("default-impl-name")
            .withParameters(defaultParameters)
            .withSensitivityProvider(Mockito.mock(CnecSensitivityProvider.class));

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> systematicSensitivityInterfaceBuilder.build());
        assertEquals("Outage instant has not been defined in the systematic sensitivity interface", exception.getMessage());
    }

    @Test
    void testCannotUseANonOutageInstantInSystematicInterfaceBuilder() {
        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder();
        Instant preventiveInstant = crac.getPreventiveInstant();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> builder.withOutageInstant(preventiveInstant));
        assertEquals("Instant provided in the systematic sensitivity builder has to be an outage", exception.getMessage());
    }
}
