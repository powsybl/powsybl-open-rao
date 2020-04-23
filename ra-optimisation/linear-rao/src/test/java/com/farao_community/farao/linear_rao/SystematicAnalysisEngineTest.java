/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
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
import com.powsybl.sensitivity.SensitivityComputationResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

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

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        crac.synchronize(network);

        initialLinearRaoData = new LinearRaoData(network, crac);
        PowerMockito.mockStatic(SystematicSensitivityAnalysisService.class);
    }

    @Test
    public void testRunDefaultConfigOk() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run OK
        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(getSensiResultOk());

        // run engine
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);
        systematicAnalysisEngine.run(initialLinearRaoData);

        // assert results
        assertNotNull(initialLinearRaoData);
        assertFalse(systematicAnalysisEngine.isFallback());
        String resultVariant = initialLinearRaoData.getWorkingVariantId();
        assertEquals(10.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        assertEquals(15.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);

    }

    @Test
    public void testRunDefaultConfigFailsAndNoFallback() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParameters.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(getSensiResultWithNull());

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
            .thenReturn(getSensiResultWithNull());

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(fallbackConfig)))
            .thenReturn(getSensiResultOk());

        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        // run
        systematicAnalysisEngine.run(initialLinearRaoData);

        // assert
        assertTrue(systematicAnalysisEngine.isFallback());
        String resultVariant = initialLinearRaoData.getWorkingVariantId();
        assertEquals(10.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInMW(), FLOW_TOLERANCE);
        assertEquals(15.0, initialLinearRaoData.getCrac().getCnec("cnec2basecase").getExtension(CnecResultExtension.class).getVariant(resultVariant).getFlowInA(), FLOW_TOLERANCE);
    }

    @Test
    public void testRunDefaultConfigAndFallbackFail() {

        RaoParameters raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/LinearRaoParametersWithFallback.json"));
        ComputationManager computationManager = DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager();

        // mock sensi service - run with null sensi
        SensitivityComputationParameters defaultConfig = raoParameters.getExtension(LinearRaoParameters.class).getSensitivityComputationParameters();
        SensitivityComputationParameters fallbackConfig = raoParameters.getExtension(LinearRaoParameters.class).getFallbackSensiParameters();

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(defaultConfig)))
            .thenReturn(getSensiResultWithNull());

        Mockito.when(SystematicSensitivityAnalysisService.runAnalysis(Mockito.any(), Mockito.any(), Mockito.any(), eq(fallbackConfig)))
            .thenReturn(getSensiResultWithNull());

        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(raoParameters.getExtension(LinearRaoParameters.class), computationManager);

        // run - expected failure
        try {
            systematicAnalysisEngine.run(initialLinearRaoData);
            fail();
        } catch (SensitivityComputationException e) {
            assertTrue(e.getMessage().contains("Sensitivity computation failed with all available sensitivity parameters."));
        }
    }

    private SystematicSensitivityAnalysisResult getSensiResultOk() {
        Map<State, SensitivityComputationResults> sensi = new HashMap<>();
        Map<Cnec, Double> flowInAmpere = new HashMap<>();
        Map<Cnec, Double> flowInMW = new HashMap<>();

        SensitivityComputationResults oneSensiMock = Mockito.mock(SensitivityComputationResults.class);
        crac.getStates().forEach(st -> sensi.put(st, oneSensiMock));
        crac.getCnecs().forEach(c -> flowInAmpere.put(c, 15.0));
        crac.getCnecs().forEach(c -> flowInMW.put(c, 10.0));

        return new SystematicSensitivityAnalysisResult(sensi, flowInMW, flowInAmpere);
    }

    private SystematicSensitivityAnalysisResult getSensiResultWithNull() {
        Map<State, SensitivityComputationResults> sensi = new HashMap<>();
        Map<Cnec, Double> flowInAmpere = new HashMap<>();
        Map<Cnec, Double> flowInMW = new HashMap<>();

        crac.getStates().forEach(st -> sensi.put(st, null));
        crac.getCnecs().forEach(c -> flowInAmpere.put(c, null));
        crac.getCnecs().forEach(c -> flowInMW.put(c, null));
        return new SystematicSensitivityAnalysisResult(sensi, flowInMW, flowInAmpere);
    }
}
