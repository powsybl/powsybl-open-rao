/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.commons.config.*;
import com.powsybl.commons.test.AbstractSerDeTest;
import com.powsybl.iidm.network.Country;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */

class RaoParametersYamlConfigTest extends AbstractSerDeTest {
    static final double DOUBLE_TOLERANCE = 1e-6;

    public RaoParameters loadRaoParameters(String configFile) {
        Path path = Paths.get(new File(getClass().getResource("/" + configFile + ".yml").getFile()).getAbsolutePath());
        Path subPath = path.getParent();
        PlatformConfig platformConfig = new PlatformConfig(PlatformConfig.loadModuleRepository(subPath, configFile), subPath);
        return RaoParameters.load(platformConfig);
    }

    @Test
    void testConfigWithExtensions() throws IOException {
        RaoParameters parameters = loadRaoParameters("config_withExtensions");

        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, objectiveFunctionParameters.getType());
        assertEquals(Unit.AMPERE, objectiveFunctionParameters.getUnit());
        assertEquals(3, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertFalse(objectiveFunctionParameters.getEnforceCurativeSecurity());

        RangeActionsOptimizationParameters rangeActionsOptimizationParameters = parameters.getRangeActionsOptimizationParameters();
        assertEquals(2, rangeActionsOptimizationParameters.getMaxMipIterations(), DOUBLE_TOLERANCE);
        assertEquals(0.02, rangeActionsOptimizationParameters.getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, rangeActionsOptimizationParameters.getPstModel());
        assertEquals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED, rangeActionsOptimizationParameters.getRaRangeShrinking());
        assertEquals(0.002, rangeActionsOptimizationParameters.getHvdcPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(0.003, rangeActionsOptimizationParameters.getInjectionRaPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.3, rangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.Solver.XPRESS, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolver());
        assertEquals(0.004, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("BLABLABLA", rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolverSpecificParameters());

        TopoOptimizationParameters topoOptimizationParameters = parameters.getTopoOptimizationParameters();
        assertEquals(3, topoOptimizationParameters.getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, topoOptimizationParameters.getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(3, topoOptimizationParameters.getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(List.of(List.of("na1", "na2"), List.of("na3", "na4", "na5")), topoOptimizationParameters.getPredefinedCombinations());
        assertEquals(0.02, topoOptimizationParameters.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(2.0, topoOptimizationParameters.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(topoOptimizationParameters.getSkipActionsFarFromMostLimitingElement());
        assertEquals(3, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);

        MultithreadingParameters multithreadingParameters = parameters.getMultithreadingParameters();
        assertEquals(4, multithreadingParameters.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(5, multithreadingParameters.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(3, multithreadingParameters.getAutoLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(6, multithreadingParameters.getCurativeLeavesInParallel(), DOUBLE_TOLERANCE);

        SecondPreventiveRaoParameters secondPreventiveRaoParameters = parameters.getSecondPreventiveRaoParameters();
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT, secondPreventiveRaoParameters.getExecutionCondition());
        assertTrue(secondPreventiveRaoParameters.getReOptimizeCurativeRangeActions());
        assertTrue(secondPreventiveRaoParameters.getHintFromFirstPreventiveRao());

        NotOptimizedCnecsParameters notOptimizedCnecsParameters = parameters.getNotOptimizedCnecsParameters();
        assertFalse(notOptimizedCnecsParameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters();
        assertEquals("LOADFLOW_PROVIDER", loadFlowAndSensitivityParameters.getLoadFlowProvider());
        assertEquals("SENSI_PROVIDER", loadFlowAndSensitivityParameters.getSensitivityProvider());
        assertEquals(2, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);

        // EXTENSIONS
        assertEquals(3, parameters.getExtensions().size());

        LoopFlowParametersExtension loopFlowParameters = parameters.getExtension(LoopFlowParametersExtension.class);
        assertNotNull(loopFlowParameters);
        assertEquals(11, loopFlowParameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO, loopFlowParameters.getPtdfApproximation());
        assertEquals(12, loopFlowParameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(13, loopFlowParameters.getViolationCost(), DOUBLE_TOLERANCE);
        Set<Country> expectedCountries = Set.of(Country.FR, Country.ES, Country.PT);
        assertEquals(expectedCountries, loopFlowParameters.getCountries());

        MnecParametersExtension mnecParametersExtension = parameters.getExtension(MnecParametersExtension.class);
        assertNotNull(mnecParametersExtension);
        assertEquals(55, mnecParametersExtension.getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
        assertEquals(11, mnecParametersExtension.getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(12, mnecParametersExtension.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);

        RelativeMarginsParametersExtension relativeMarginsParametersExtension = parameters.getExtension(RelativeMarginsParametersExtension.class);
        assertNotNull(relativeMarginsParametersExtension);
        List<String> expectedBoundaries = List.of("{FR}-{BE}", "{FR}-{DE}");
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST, relativeMarginsParametersExtension.getPtdfApproximation());
        assertEquals(0.02, relativeMarginsParametersExtension.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(expectedBoundaries, relativeMarginsParametersExtension.getPtdfBoundariesAsString());

        // Compare to json
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_config_withExtensions.json");
    }

    @Test
    void testConfigWithoutExtensions() throws IOException {
        RaoParameters parameters = loadRaoParameters("config_withoutExtensions");

        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, objectiveFunctionParameters.getType());
        assertEquals(Unit.AMPERE, objectiveFunctionParameters.getUnit());
        assertEquals(3, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertFalse(objectiveFunctionParameters.getEnforceCurativeSecurity());

        RangeActionsOptimizationParameters rangeActionsOptimizationParameters = parameters.getRangeActionsOptimizationParameters();
        assertEquals(2, rangeActionsOptimizationParameters.getMaxMipIterations(), DOUBLE_TOLERANCE);
        assertEquals(0.02, rangeActionsOptimizationParameters.getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, rangeActionsOptimizationParameters.getPstModel());
        assertEquals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED, rangeActionsOptimizationParameters.getRaRangeShrinking());
        assertEquals(0.002, rangeActionsOptimizationParameters.getHvdcPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(0.003, rangeActionsOptimizationParameters.getInjectionRaPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.3, rangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.Solver.XPRESS, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolver());
        assertEquals(0.004, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("BLABLABLA", rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolverSpecificParameters());

        TopoOptimizationParameters topoOptimizationParameters = parameters.getTopoOptimizationParameters();
        assertEquals(3, topoOptimizationParameters.getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, topoOptimizationParameters.getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(3, topoOptimizationParameters.getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(List.of(List.of("na1", "na2"), List.of("na3", "na4", "na5")), topoOptimizationParameters.getPredefinedCombinations());
        assertEquals(0.02, topoOptimizationParameters.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(2.0, topoOptimizationParameters.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(topoOptimizationParameters.getSkipActionsFarFromMostLimitingElement());
        assertEquals(3, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);

        MultithreadingParameters multithreadingParameters = parameters.getMultithreadingParameters();
        assertEquals(4, multithreadingParameters.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(5, multithreadingParameters.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(3, multithreadingParameters.getAutoLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(6, multithreadingParameters.getCurativeLeavesInParallel(), DOUBLE_TOLERANCE);

        SecondPreventiveRaoParameters secondPreventiveRaoParameters = parameters.getSecondPreventiveRaoParameters();
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT, secondPreventiveRaoParameters.getExecutionCondition());
        assertTrue(secondPreventiveRaoParameters.getReOptimizeCurativeRangeActions());
        assertTrue(secondPreventiveRaoParameters.getHintFromFirstPreventiveRao());

        NotOptimizedCnecsParameters notOptimizedCnecsParameters = parameters.getNotOptimizedCnecsParameters();
        assertFalse(notOptimizedCnecsParameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters();
        assertEquals("LOADFLOW_PROVIDER", loadFlowAndSensitivityParameters.getLoadFlowProvider());
        assertEquals("SENSI_PROVIDER", loadFlowAndSensitivityParameters.getSensitivityProvider());
        assertEquals(2, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);

        // EXTENSIONS
        assertEquals(0, parameters.getExtensions().size());

        LoopFlowParametersExtension loopFlowParameters = parameters.getExtension(LoopFlowParametersExtension.class);
        assertNull(loopFlowParameters);

        MnecParametersExtension mnecParametersExtension = parameters.getExtension(MnecParametersExtension.class);
        assertNull(mnecParametersExtension);

        RelativeMarginsParametersExtension relativeMarginsParametersExtension = parameters.getExtension(RelativeMarginsParametersExtension.class);
        assertNull(relativeMarginsParametersExtension);

        // Compare to json
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_config_withoutExtensions.json");
    }

    @Test
    void testConfigWithPartialExtensions() throws IOException {
        RaoParameters parameters = loadRaoParameters("config_withPartialExtensions");

        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, objectiveFunctionParameters.getType());
        assertEquals(Unit.MEGAWATT, objectiveFunctionParameters.getUnit());
        assertEquals(3, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertFalse(objectiveFunctionParameters.getEnforceCurativeSecurity());

        RangeActionsOptimizationParameters rangeActionsOptimizationParameters = parameters.getRangeActionsOptimizationParameters();
        assertEquals(10, rangeActionsOptimizationParameters.getMaxMipIterations(), DOUBLE_TOLERANCE);
        assertEquals(0.02, rangeActionsOptimizationParameters.getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, rangeActionsOptimizationParameters.getPstModel());
        assertEquals(RangeActionsOptimizationParameters.RaRangeShrinking.ENABLED, rangeActionsOptimizationParameters.getRaRangeShrinking());
        assertEquals(0.002, rangeActionsOptimizationParameters.getHvdcPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.2, rangeActionsOptimizationParameters.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(0.003, rangeActionsOptimizationParameters.getInjectionRaPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(0.3, rangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.Solver.CBC, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolver());
        assertEquals(0.004, rangeActionsOptimizationParameters.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("BLABLABLA", rangeActionsOptimizationParameters.getLinearOptimizationSolver().getSolverSpecificParameters());

        TopoOptimizationParameters topoOptimizationParameters = parameters.getTopoOptimizationParameters();
        assertEquals(3, topoOptimizationParameters.getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, topoOptimizationParameters.getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(3, topoOptimizationParameters.getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(List.of(List.of("na1", "na2"), List.of("na3", "na4", "na5")), topoOptimizationParameters.getPredefinedCombinations());
        assertEquals(0.02, topoOptimizationParameters.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(2.0, topoOptimizationParameters.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(topoOptimizationParameters.getSkipActionsFarFromMostLimitingElement());
        assertEquals(2, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);

        MultithreadingParameters multithreadingParameters = parameters.getMultithreadingParameters();
        assertEquals(1, multithreadingParameters.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(5, multithreadingParameters.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(2, multithreadingParameters.getAutoLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(6, multithreadingParameters.getCurativeLeavesInParallel(), DOUBLE_TOLERANCE);

        SecondPreventiveRaoParameters secondPreventiveRaoParameters = parameters.getSecondPreventiveRaoParameters();
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED, secondPreventiveRaoParameters.getExecutionCondition());
        assertTrue(secondPreventiveRaoParameters.getReOptimizeCurativeRangeActions());
        assertTrue(secondPreventiveRaoParameters.getHintFromFirstPreventiveRao());

        NotOptimizedCnecsParameters notOptimizedCnecsParameters = parameters.getNotOptimizedCnecsParameters();
        assertFalse(notOptimizedCnecsParameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters();
        assertEquals("OpenLoadFlow", loadFlowAndSensitivityParameters.getLoadFlowProvider());
        assertEquals("SENSI_PROVIDER", loadFlowAndSensitivityParameters.getSensitivityProvider());
        assertEquals(2, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);

        // EXTENSIONS
        assertEquals(2, parameters.getExtensions().size());

        LoopFlowParametersExtension loopFlowParameters = parameters.getExtension(LoopFlowParametersExtension.class);
        assertNotNull(loopFlowParameters);
        assertEquals(0, loopFlowParameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO, loopFlowParameters.getPtdfApproximation());
        assertEquals(12, loopFlowParameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(13, loopFlowParameters.getViolationCost(), DOUBLE_TOLERANCE);
        Set<Country> expectedCountries = Set.of(Country.FR, Country.ES, Country.PT);
        assertEquals(expectedCountries, loopFlowParameters.getCountries());

        MnecParametersExtension mnecParametersExtension = parameters.getExtension(MnecParametersExtension.class);
        assertNull(mnecParametersExtension);

        RelativeMarginsParametersExtension relativeMarginsParametersExtension = parameters.getExtension(RelativeMarginsParametersExtension.class);
        assertNotNull(relativeMarginsParametersExtension);
        List<String> expectedBoundaries = Collections.emptyList();
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST, relativeMarginsParametersExtension.getPtdfApproximation());
        assertEquals(0.02, relativeMarginsParametersExtension.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(expectedBoundaries, relativeMarginsParametersExtension.getPtdfBoundariesAsString());

        // Compare to json
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_config_withPartialExtensions.json");
    }

    @Test
    void testConfigWithOpenLoadFlowExtension() throws IOException {
        RaoParameters parameters = loadRaoParameters("config_withOpenLoadFlowExtension");
        LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters = parameters.getLoadFlowAndSensitivityParameters();
        assertEquals("OpenLoadFlow", loadFlowAndSensitivityParameters.getLoadFlowProvider());
        assertEquals("OpenLoadFlow", loadFlowAndSensitivityParameters.getSensitivityProvider());
        assertEquals(2, loadFlowAndSensitivityParameters.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);
        OpenLoadFlowParameters olfParams = loadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters().getLoadFlowParameters().getExtension(OpenLoadFlowParameters.class);
        assertNotNull(olfParams);
        assertEquals(0.444, olfParams.getMinPlausibleTargetVoltage(), DOUBLE_TOLERANCE);
        assertEquals(1.444, olfParams.getMaxPlausibleTargetVoltage(), DOUBLE_TOLERANCE);
        assertEquals(111, olfParams.getMaxNewtonRaphsonIterations(), DOUBLE_TOLERANCE);

        // Compare to json
        roundTripTest(parameters, JsonRaoParameters::write, JsonRaoParameters::read, "/RaoParameters_config_withOLFParams.json");
    }
}
