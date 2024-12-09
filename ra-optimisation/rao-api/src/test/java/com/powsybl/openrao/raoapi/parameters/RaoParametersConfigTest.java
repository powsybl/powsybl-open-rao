/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.raoapi.parameters.extensions.*;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.*;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */

class RaoParametersConfigTest {
    private PlatformConfig mockedPlatformConfig;
    private InMemoryPlatformConfig platformCfg;
    static final double DOUBLE_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        mockedPlatformConfig = Mockito.mock(PlatformConfig.class);
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    void checkObjectiveFunctionConfig() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("rao-objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN");
        objectiveFunctionModuleConfig.setStringProperty("unit", "AMPERE");
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        objectiveFunctionModuleConfig.setStringProperty("preventive-stop-criterion", "MIN_OBJECTIVE");
        objectiveFunctionModuleConfig.setStringProperty("enforce-curative-security", "false");

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, objectiveFunctionParameters.getType());
        assertEquals(Unit.AMPERE, objectiveFunctionParameters.getUnit());
        assertEquals(123, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, objectiveFunctionParameters.getPreventiveStopCriterion());
        assertFalse(objectiveFunctionParameters.getEnforceCurativeSecurity());
    }

    @Test
    void checkRangeActionsOptimizationConfig() {
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("rao-range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(4));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-penalty-cost", Objects.toString(44));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-sensitivity-threshold", Objects.toString(7));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-model", "APPROXIMATED_INTEGERS");
        rangeActionsOptimizationModuleConfig.setStringProperty("hvdc-penalty-cost", Objects.toString(33));
        rangeActionsOptimizationModuleConfig.setStringProperty("hvdc-sensitivity-threshold", Objects.toString(8));
        rangeActionsOptimizationModuleConfig.setStringProperty("injection-ra-penalty-cost", Objects.toString(22));
        rangeActionsOptimizationModuleConfig.setStringProperty("injection-ra-sensitivity-threshold", Objects.toString(9));
        MapModuleConfig linearOptimizationSolverModuleConfig = platformCfg.createModuleConfig("rao-linear-optimization-solver");
        linearOptimizationSolverModuleConfig.setStringProperty("solver", "XPRESS");
        linearOptimizationSolverModuleConfig.setStringProperty("relative-mip-gap", Objects.toString(22));
        linearOptimizationSolverModuleConfig.setStringProperty("solver-specific-parameters", "blabla");
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        RangeActionsOptimizationParameters params = parameters.getRangeActionsOptimizationParameters();
        assertEquals(4, params.getMaxMipIterations(), DOUBLE_TOLERANCE);
        assertEquals(44, params.getPstPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(7, params.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, params.getPstModel());
        assertEquals(RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED, params.getRaRangeShrinking());
        assertEquals(33, params.getHvdcPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(8, params.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getInjectionRaPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(9, params.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.Solver.XPRESS, params.getLinearOptimizationSolver().getSolver());
        assertEquals(22, params.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("blabla", params.getLinearOptimizationSolver().getSolverSpecificParameters());
    }

    @Test
    void checkTopoActionsOptimizationConfig() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("rao-topological-actions-optimization");
        topoActionsModuleConfig.setStringProperty("max-preventive-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfig.setStringProperty("max-auto-search-tree-depth", Objects.toString(2));
        topoActionsModuleConfig.setStringProperty("max-curative-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12} + {na22}", "{na41} + {na5} + {na6}"));
        topoActionsModuleConfig.setStringProperty("relative-minimum-impact-threshold", Objects.toString(0.9));
        topoActionsModuleConfig.setStringProperty("absolute-minimum-impact-threshold", Objects.toString(22));
        topoActionsModuleConfig.setStringProperty("skip-actions-far-from-most-limiting-element", Objects.toString(true));
        topoActionsModuleConfig.setStringProperty("max-number-of-boundaries-for-skipping-actions", Objects.toString(3333));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        TopoOptimizationParameters params = parameters.getTopoOptimizationParameters();
        assertEquals(3, params.getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, params.getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(3, params.getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(List.of(List.of("na12", "na22"), List.of("na41", "na5", "na6")), params.getPredefinedCombinations());
        assertEquals(0.9, params.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(params.getSkipActionsFarFromMostLimitingElement());
        assertEquals(3333, params.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkMultiThreadingConfig() {
        MapModuleConfig multiThreadingModuleConfig = platformCfg.createModuleConfig("rao-multi-threading");
        multiThreadingModuleConfig.setStringProperty("contingency-scenarios-in-parallel", Objects.toString(3));
        multiThreadingModuleConfig.setStringProperty("preventive-leaves-in-parallel", Objects.toString(23));
        multiThreadingModuleConfig.setStringProperty("auto-leaves-in-parallel", Objects.toString(17));
        multiThreadingModuleConfig.setStringProperty("curative-leaves-in-parallel", Objects.toString(43));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        MultithreadingParameters params = parameters.getMultithreadingParameters();
        assertEquals(3, params.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(23, params.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(17, params.getAutoLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(43, params.getCurativeLeavesInParallel(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkSecondPreventiveRaoConfig() {
        MapModuleConfig secondPreventiveRaoModuleConfig = platformCfg.createModuleConfig("rao-second-preventive-rao");
        secondPreventiveRaoModuleConfig.setStringProperty("execution-condition", "POSSIBLE_CURATIVE_IMPROVEMENT");
        secondPreventiveRaoModuleConfig.setStringProperty("re-optimize-curative-range-actions", Objects.toString(false));
        secondPreventiveRaoModuleConfig.setStringProperty("hint-from-first-preventive-rao", Objects.toString(true));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        SecondPreventiveRaoParameters params = parameters.getSecondPreventiveRaoParameters();
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT, params.getExecutionCondition());
        assertFalse(params.getReOptimizeCurativeRangeActions());
        assertTrue(params.getHintFromFirstPreventiveRao());
    }

    @Test
    void checkNotOptimizedCnecsConfig() {
        MapModuleConfig notOptimizedModuleConfig = platformCfg.createModuleConfig("rao-not-optimized-cnecs");
        notOptimizedModuleConfig.setStringProperty("do-not-optimize-curative-cnecs-for-tsos-without-cras", Objects.toString(false));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        NotOptimizedCnecsParameters params = parameters.getNotOptimizedCnecsParameters();
        assertFalse(params.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
    }

    @Test
    void checkLoadFlowParametersConfig() {
        MapModuleConfig loadFlowModuleConfig = platformCfg.createModuleConfig("rao-load-flow-and-sensitivity-computation");
        loadFlowModuleConfig.setStringProperty("load-flow-provider", "Bonjour");
        loadFlowModuleConfig.setStringProperty("sensitivity-provider", "Au revoir");
        loadFlowModuleConfig.setStringProperty("sensitivity-failure-overcost", Objects.toString(32));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        LoadFlowAndSensitivityParameters params = parameters.getLoadFlowAndSensitivityParameters();
        assertEquals("Bonjour", params.getLoadFlowProvider());
        assertEquals("Au revoir", params.getSensitivityProvider());
        assertEquals(32, params.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkLoopFlowParametersConfig() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("acceptable-increase"), anyDouble())).thenReturn(32.);
        Mockito.when(loopFlowModuleConfig.getEnumProperty(eq("ptdf-approximation"), eq(PtdfApproximation.class), any())).thenReturn(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("FR", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParametersConfigLoader configLoader = new LoopFlowParametersConfigLoader();
        LoopFlowParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        assertEquals(32, parameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO, parameters.getPtdfApproximation());
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
        Set<Country> expectedCountries = Set.of(Country.FR, Country.ES, Country.PT);
        assertEquals(expectedCountries, parameters.getCountries());
    }

    @Test
    void checkMnecParametersConfig() {
        ModuleConfig mnecModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("acceptable-margin-decrease"), anyDouble())).thenReturn(32.);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-mnec-parameters")).thenReturn(Optional.of(mnecModuleConfig));
        MnecParametersConfigLoader configLoader = new MnecParametersConfigLoader();
        MnecParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        assertEquals(32, parameters.getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkRelativeMarginsConfig() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getDoubleProperty(eq("ptdf-sum-lower-bound"), anyDouble())).thenReturn(32.);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        RelativeMarginsParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        List<String> expectedBoundaries = List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}");
        assertEquals(32, parameters.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(expectedBoundaries, parameters.getPtdfBoundariesAsString());
    }

    @Test
    void checkMultipleConfigs() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("rao-objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN");
        objectiveFunctionModuleConfig.setStringProperty("unit", "AMPERE");
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("rao-range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(32));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, parameters.getObjectiveFunctionParameters().getType());
        assertEquals(Unit.AMPERE, parameters.getObjectiveFunctionParameters().getUnit());
        assertEquals(123, parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 1e-6);
        assertEquals(32, parameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), 1e-6);
        assertTrue(Objects.isNull(parameters.getExtension(LoopFlowParametersExtension.class)));
        assertTrue(Objects.isNull(parameters.getExtension(MnecParametersExtension.class)));
        assertTrue(Objects.isNull(parameters.getExtension(RelativeMarginsParametersExtension.class)));
    }

    @Test
    void inconsistentPredefinedCombinations1() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("rao-topological-actions-optimization");
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12 + {na22}", "{na41} + {na5} + {na6}"));
        RaoParameters parameters = new RaoParameters();
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(parameters, platformCfg));
    }

    @Test
    void inconsistentPredefinedCombinations2() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("rao-topological-actions-optimization");
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12} - {na22}", "{na41} + {na5} + {na6}"));
        RaoParameters parameters = new RaoParameters();
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(parameters, platformCfg));
    }

    @Test
    void inconsistentLoopFlowCountries() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("France", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParametersConfigLoader configLoader = new LoopFlowParametersConfigLoader();
        assertThrows(OpenRaoException.class, () -> configLoader.load(mockedPlatformConfig));
    }

    @Test
    void inconsistentRelativeMarginsBoundaries1() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR}{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        assertThrows(OpenRaoException.class, () -> configLoader.load(mockedPlatformConfig));
    }

    @Test
    void inconsistentRelativeMarginsBoundaries2() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR-{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        assertThrows(OpenRaoException.class, () -> configLoader.load(mockedPlatformConfig));
    }
}
