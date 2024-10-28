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

import static com.powsybl.openrao.raoapi.parameters.extensions.ObjectiveFunctionParameters.getCurativeMinObjImprovement;
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
        objectiveFunctionModuleConfig.setStringProperty("enforce-curative-security", "false");
        MapModuleConfig objectiveFunctionModuleConfigExt = platformCfg.createModuleConfig("search-tree-objective-function");
        objectiveFunctionModuleConfigExt.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, objectiveFunctionParameters.getType());
        assertEquals(Unit.AMPERE, objectiveFunctionParameters.getUnit());
        assertEquals(123, getCurativeMinObjImprovement(parameters), DOUBLE_TOLERANCE);
        assertFalse(objectiveFunctionParameters.getEnforceCurativeSecurity());
    }

    @Test
    void checkRangeActionsOptimizationConfig() {
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("rao-range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-ra-min-impact-threshold", Objects.toString(44));
        rangeActionsOptimizationModuleConfig.setStringProperty("hvdc-ra-min-impact-threshold", Objects.toString(33));
        rangeActionsOptimizationModuleConfig.setStringProperty("injection-ra-min-impact-threshold", Objects.toString(22));
        MapModuleConfig rangeActionsOptimizationModuleConfigExt = platformCfg.createModuleConfig("search-tree-range-actions-optimization");
        rangeActionsOptimizationModuleConfigExt.setStringProperty("max-mip-iterations", Objects.toString(4));
        rangeActionsOptimizationModuleConfigExt.setStringProperty("pst-sensitivity-threshold", Objects.toString(7));
        rangeActionsOptimizationModuleConfigExt.setStringProperty("pst-model", "APPROXIMATED_INTEGERS");
        rangeActionsOptimizationModuleConfigExt.setStringProperty("hvdc-sensitivity-threshold", Objects.toString(8));
        rangeActionsOptimizationModuleConfigExt.setStringProperty("injection-ra-sensitivity-threshold", Objects.toString(9));
        MapModuleConfig linearOptimizationSolverModuleConfig = platformCfg.createModuleConfig("search-tree-linear-optimization-solver");
        linearOptimizationSolverModuleConfig.setStringProperty("solver", "XPRESS");
        linearOptimizationSolverModuleConfig.setStringProperty("relative-mip-gap", Objects.toString(22));
        linearOptimizationSolverModuleConfig.setStringProperty("solver-specific-parameters", "blabla");
        RaoParameters parameters = RaoParameters.load(platformCfg);
        RangeActionsOptimizationParameters params = parameters.getRangeActionsOptimizationParameters();
        com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters paramsExt = parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters();
        assertEquals(4, paramsExt.getMaxMipIterations(), DOUBLE_TOLERANCE);
        assertEquals(44, params.getPstRAMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(7, paramsExt.getPstSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, paramsExt.getPstModel());
        assertEquals(com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.RaRangeShrinking.DISABLED, paramsExt.getRaRangeShrinking());
        assertEquals(33, params.getHvdcRAMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(8, paramsExt.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getInjectionRAMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(9, paramsExt.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters.Solver.XPRESS, paramsExt.getLinearOptimizationSolver().getSolver());
        assertEquals(22, paramsExt.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("blabla", paramsExt.getLinearOptimizationSolver().getSolverSpecificParameters());
    }

    @Test
    void checkTopoActionsOptimizationConfig() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("rao-topological-actions-optimization");
        topoActionsModuleConfig.setStringProperty("relative-minimum-impact-threshold", Objects.toString(0.9));
        topoActionsModuleConfig.setStringProperty("absolute-minimum-impact-threshold", Objects.toString(22));
        MapModuleConfig topoActionsModuleConfigExt = platformCfg.createModuleConfig("search-tree-topological-actions-optimization");
        topoActionsModuleConfigExt.setStringProperty("max-preventive-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfigExt.setStringProperty("max-auto-search-tree-depth", Objects.toString(2));
        topoActionsModuleConfigExt.setStringProperty("max-curative-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfigExt.setStringListProperty("predefined-combinations", List.of("{na12} + {na22}", "{na41} + {na5} + {na6}"));
        topoActionsModuleConfigExt.setStringProperty("skip-actions-far-from-most-limiting-element", Objects.toString(true));
        topoActionsModuleConfigExt.setStringProperty("max-number-of-boundaries-for-skipping-actions", Objects.toString(3333));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        TopoOptimizationParameters params = parameters.getTopoOptimizationParameters();
        com.powsybl.openrao.raoapi.parameters.extensions.TopoOptimizationParameters paramsExt = parameters.getExtension(OpenRaoSearchTreeParameters.class).getTopoOptimizationParameters();
        assertEquals(3, paramsExt.getMaxPreventiveSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(2, paramsExt.getMaxAutoSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(3, paramsExt.getMaxCurativeSearchTreeDepth(), DOUBLE_TOLERANCE);
        assertEquals(List.of(List.of("na12", "na22"), List.of("na41", "na5", "na6")), paramsExt.getPredefinedCombinations());
        assertEquals(0.9, params.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(paramsExt.getSkipActionsFarFromMostLimitingElement());
        assertEquals(3333, paramsExt.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkMultiThreadingConfig() {
        MapModuleConfig multiThreadingModuleConfig = platformCfg.createModuleConfig("search-tree-multi-threading");
        multiThreadingModuleConfig.setStringProperty("available-cpus", Objects.toString(43));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        MultithreadingParameters params = parameters.getExtension(OpenRaoSearchTreeParameters.class).getMultithreadingParameters();
        assertEquals(43, params.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(43, params.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(1, params.getAutoLeavesInParallel(), DOUBLE_TOLERANCE);
        assertEquals(1, params.getCurativeLeavesInParallel(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkSecondPreventiveRaoConfig() {
        MapModuleConfig secondPreventiveRaoModuleConfig = platformCfg.createModuleConfig("search-tree-second-preventive-rao");
        secondPreventiveRaoModuleConfig.setStringProperty("execution-condition", "POSSIBLE_CURATIVE_IMPROVEMENT");
        secondPreventiveRaoModuleConfig.setStringProperty("re-optimize-curative-range-actions", Objects.toString(false));
        secondPreventiveRaoModuleConfig.setStringProperty("hint-from-first-preventive-rao", Objects.toString(true));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        SecondPreventiveRaoParameters params = parameters.getExtension(OpenRaoSearchTreeParameters.class).getSecondPreventiveRaoParameters();
        assertEquals(SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT, params.getExecutionCondition());
        assertFalse(params.getReOptimizeCurativeRangeActions());
        assertTrue(params.getHintFromFirstPreventiveRao());
    }

    @Test
    void checkNotOptimizedCnecsConfig() {
        MapModuleConfig notOptimizedModuleConfig = platformCfg.createModuleConfig("rao-not-optimized-cnecs");
        notOptimizedModuleConfig.setStringProperty("do-not-optimize-curative-cnecs-for-tsos-without-cras", Objects.toString(false));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        NotOptimizedCnecsParameters params = parameters.getNotOptimizedCnecsParameters();
        assertFalse(params.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
    }

    @Test
    void checkLoadFlowParametersConfig() {
        MapModuleConfig loadFlowModuleConfig = platformCfg.createModuleConfig("search-tree-load-flow-and-sensitivity-computation");
        loadFlowModuleConfig.setStringProperty("load-flow-provider", "Bonjour");
        loadFlowModuleConfig.setStringProperty("sensitivity-provider", "Au revoir");
        loadFlowModuleConfig.setStringProperty("sensitivity-failure-overcost", Objects.toString(32));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        LoadFlowAndSensitivityParameters paramsExt = parameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters();
        assertEquals("Bonjour", paramsExt.getLoadFlowProvider());
        assertEquals("Au revoir", paramsExt.getSensitivityProvider());
        assertEquals(32, paramsExt.getSensitivityFailureOvercost(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkLoopFlowParametersConfig() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("acceptable-increase"), anyDouble())).thenReturn(32.);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("FR", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParameters parameters = RaoParameters.load(mockedPlatformConfig).getLoopFlowParameters().get();
        assertEquals(32, parameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        Set<Country> expectedCountries = Set.of(Country.FR, Country.ES, Country.PT);
        assertEquals(expectedCountries, parameters.getCountries());
    }

    @Test
    void checkLoopFlowParametersConfigExtension() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getEnumProperty(eq("ptdf-approximation"), eq(PtdfApproximation.class), any())).thenReturn(PtdfApproximation.UPDATE_PTDF_WITH_TOPO);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("search-tree-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        OpenRaoSearchTreeParametersConfigLoader configLoader = new OpenRaoSearchTreeParametersConfigLoader();
        com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters parameters = configLoader.load(mockedPlatformConfig).getLoopFlowParameters().get();
        assertEquals(PtdfApproximation.UPDATE_PTDF_WITH_TOPO, parameters.getPtdfApproximation());
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
        Set<Country> expectedCountries = Set.of(Country.FR, Country.ES, Country.PT);
    }

    @Test
    void checkMnecParametersConfig() {
        ModuleConfig mnecModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("acceptable-margin-decrease"), anyDouble())).thenReturn(32.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-mnec-parameters")).thenReturn(Optional.of(mnecModuleConfig));
        MnecParameters parameters = RaoParameters.load(mockedPlatformConfig).getMnecParameters().get();
        assertEquals(32, parameters.getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkMnecParametersConfigExtension() {
        ModuleConfig mnecModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("search-tree-mnec-parameters")).thenReturn(Optional.of(mnecModuleConfig));
        OpenRaoSearchTreeParametersConfigLoader configLoader = new OpenRaoSearchTreeParametersConfigLoader();
        com.powsybl.openrao.raoapi.parameters.extensions.MnecParameters parameters = configLoader.load(mockedPlatformConfig).getMnecParameters().get();
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkRelativeMarginsConfig() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParameters parameters = RaoParameters.load(mockedPlatformConfig).getRelativeMarginsParameters().get();
        List<String> expectedBoundaries = List.of("{FR}-{BE}", "{FR}-{DE}", "{BE}-{22Y201903144---9}-{DE}+{22Y201903145---4}");
        assertEquals(expectedBoundaries, parameters.getPtdfBoundariesAsString());
    }

    @Test
    void checkRelativeMarginsConfigExtension() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getDoubleProperty(eq("ptdf-sum-lower-bound"), anyDouble())).thenReturn(32.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("search-tree-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        OpenRaoSearchTreeParametersConfigLoader configLoader = new OpenRaoSearchTreeParametersConfigLoader();
        com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParameters parameters = configLoader.load(mockedPlatformConfig).getRelativeMarginsParameters().get();
        assertEquals(32, parameters.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
    }

    @Test
    void checkMultipleConfigs() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("rao-objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN");
        objectiveFunctionModuleConfig.setStringProperty("unit", "AMPERE");
        MapModuleConfig objectiveFunctionExtModuleConfig = platformCfg.createModuleConfig("search-tree-objective-function");
        objectiveFunctionExtModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        MapModuleConfig rangeActionsOptimizationExtModuleConfig = platformCfg.createModuleConfig("search-tree-range-actions-optimization");
        rangeActionsOptimizationExtModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(32));
        RaoParameters parameters = RaoParameters.load(platformCfg);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, parameters.getObjectiveFunctionParameters().getType());
        assertEquals(Unit.AMPERE, parameters.getObjectiveFunctionParameters().getUnit());
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        assertEquals(123, searchTreeParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 1e-6);
        assertEquals(32, searchTreeParameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), 1e-6);
        assertTrue(parameters.getLoopFlowParameters().isEmpty());
        assertTrue(parameters.getMnecParameters().isEmpty());
        assertTrue(parameters.getRelativeMarginsParameters().isEmpty());
        assertTrue(searchTreeParameters.getLoopFlowParameters().isEmpty());
        assertTrue(searchTreeParameters.getMnecParameters().isEmpty());
        assertTrue(searchTreeParameters.getRelativeMarginsParameters().isEmpty());
    }

    @Test
    void inconsistentPredefinedCombinations1() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("search-tree-topological-actions-optimization");
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12 + {na22}", "{na41} + {na5} + {na6}"));
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(platformCfg));
    }

    @Test
    void inconsistentPredefinedCombinations2() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("search-tree-topological-actions-optimization");
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12} - {na22}", "{na41} + {na5} + {na6}"));
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(platformCfg));
    }

    @Test
    void inconsistentLoopFlowCountries() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("France", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(mockedPlatformConfig));
    }

    @Test
    void inconsistentRelativeMarginsBoundaries1() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR}{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(mockedPlatformConfig));
    }

    @Test
    void inconsistentRelativeMarginsBoundaries2() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR-{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        assertThrows(OpenRaoException.class, () -> RaoParameters.load(mockedPlatformConfig));
    }
}
