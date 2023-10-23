/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.extensions.*;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class RaoParametersConfigTest {
    static double DOUBLE_TOLERANCE = 1e-6;
    private PlatformConfig mockedPlatformConfig;
    private InMemoryPlatformConfig platformCfg;

    static Stream<Arguments> generateIntMap() {
        return Stream.of(
            Arguments.of(List.of("{ABC:5", "{DEF}:6"), "{ABC contains too few or too many occurences of \"{ or \"}"),
            Arguments.of(List.of("{ABC}+5", "{DEF}:6"), "String-Integer pairs separated by \":\" must be defined, e.g {String1}:Integer instead of {ABC}+5"),
            Arguments.of(List.of("{ABC}", "{DEF}:6"), "String-Integer pairs separated by \":\" must be defined, e.g {String1}:Integer instead of {ABC}"),
            Arguments.of(List.of("5", "{DEF}:6"), "String-Integer pairs separated by \":\" must be defined, e.g {String1}:Integer instead of 5")
        );
    }

    static Stream<Arguments> generateStringStringMap() {
        return Stream.of(
            Arguments.of(List.of("{cnec1:{pst1}", "{halfline1Cnec2 + halfline2Cnec2}:{pst2}"), "{cnec1 contains too few or too many occurences of \"{ or \"}"),
            Arguments.of(List.of("{cnec1}:pst1}", "{halfline1Cnec2 + halfline2Cnec2}:{pst2}"), "pst1} contains too few or too many occurences of \"{ or \"}"),
            Arguments.of(List.of("{cnec1}:", "{halfline1Cnec2 + halfline2Cnec2}:{pst2}"), "String pairs separated by \":\" must be defined, e.g {String1}:{String2} instead of {cnec1}:"),
            Arguments.of(List.of(":{pst1}", "{halfline1Cnec2 + halfline2Cnec2}:{pst2}"), " contains too few or too many occurences of \"{ or \"}"),
            Arguments.of(List.of("{cnec1}{blabla}:{pst1}"), "{cnec1}{blabla} contains too few or too many occurences of \"{ or \"}")
        );
    }

    @BeforeEach
    public void setUp() {
        mockedPlatformConfig = Mockito.mock(PlatformConfig.class);
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    void checkObjectiveFunctionConfig() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("rao-objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN_IN_AMPERE");
        objectiveFunctionModuleConfig.setStringProperty("forbid-cost-increase", Objects.toString(true));
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        objectiveFunctionModuleConfig.setStringProperty("preventive-stop-criterion", "MIN_OBJECTIVE");
        objectiveFunctionModuleConfig.setStringProperty("curative-stop-criterion", "PREVENTIVE_OBJECTIVE");

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertTrue(objectiveFunctionParameters.getForbidCostIncrease());
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, objectiveFunctionParameters.getType());
        assertEquals(123, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, objectiveFunctionParameters.getPreventiveStopCriterion());
        assertEquals(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE, objectiveFunctionParameters.getCurativeStopCriterion());
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
        topoActionsModuleConfig.setStringProperty("max-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12} + {na22}", "{na41} + {na5} + {na6}"));
        topoActionsModuleConfig.setStringProperty("relative-minimum-impact-threshold", Objects.toString(0.9));
        topoActionsModuleConfig.setStringProperty("absolute-minimum-impact-threshold", Objects.toString(22));
        topoActionsModuleConfig.setStringProperty("skip-actions-far-from-most-limiting-element", Objects.toString(true));
        topoActionsModuleConfig.setStringProperty("max-number-of-boundaries-for-skipping-actions", Objects.toString(3333));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        TopoOptimizationParameters params = parameters.getTopoOptimizationParameters();
        assertEquals(3, params.getMaxSearchTreeDepth(), DOUBLE_TOLERANCE);
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
        multiThreadingModuleConfig.setStringProperty("curative-leaves-in-parallel", Objects.toString(43));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        MultithreadingParameters params = parameters.getMultithreadingParameters();
        assertEquals(3, params.getContingencyScenariosInParallel(), DOUBLE_TOLERANCE);
        assertEquals(23, params.getPreventiveLeavesInParallel(), DOUBLE_TOLERANCE);
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
    void checkRaUsageLimitsPerContingencyConfig() {
        MapModuleConfig raUsageLimitsModuleConfig = platformCfg.createModuleConfig("rao-ra-usage-limits-per-contingency");
        raUsageLimitsModuleConfig.setStringProperty("max-curative-ra", Objects.toString(3));
        raUsageLimitsModuleConfig.setStringProperty("max-curative-tso", Objects.toString(13));
        raUsageLimitsModuleConfig.setStringListProperty("max-curative-topo-per-tso", List.of("{ABC}:5", "{DEF}:6"));
        raUsageLimitsModuleConfig.setStringListProperty("max-curative-pst-per-tso", List.of("{ABC}:54", "{DEF}:64"));
        raUsageLimitsModuleConfig.setStringListProperty("max-curative-ra-per-tso", List.of("{ABC}:55", "{DEF}:66"));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        RaUsageLimitsPerContingencyParameters params = parameters.getRaUsageLimitsPerContingencyParameters();
        assertEquals(3, params.getMaxCurativeRa(), DOUBLE_TOLERANCE);
        assertEquals(13, params.getMaxCurativeTso(), DOUBLE_TOLERANCE);
        Map<String, Integer> expectedTopoTsoMap = Map.of("ABC", 5, "DEF", 6);
        Map<String, Integer> expectedPstTsoMap = Map.of("ABC", 54, "DEF", 64);
        Map<String, Integer> expectedRaTsoMap = Map.of("ABC", 55, "DEF", 66);
        assertEquals(expectedTopoTsoMap, params.getMaxCurativeTopoPerTso());
        assertEquals(expectedPstTsoMap, params.getMaxCurativePstPerTso());
        assertEquals(expectedRaTsoMap, params.getMaxCurativeRaPerTso());
    }

    @Test
    void checkNotOptimizedCnecsConfig() {
        MapModuleConfig notOptimizedModuleConfig = platformCfg.createModuleConfig("rao-not-optimized-cnecs");
        notOptimizedModuleConfig.setStringProperty("do-not-optimize-curative-cnecs-for-tsos-without-cras", Objects.toString(false));
        notOptimizedModuleConfig.setStringListProperty("do-not-optimize-cnec-secured-by-its-pst", List.of("{cnec1}:{pst1}", "{halfline1Cnec2 + halfline2Cnec2}:{pst2}"));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        NotOptimizedCnecsParameters params = parameters.getNotOptimizedCnecsParameters();
        Map<String, String> expectedCnecPstMap = Map.of("cnec1", "pst1", "halfline1Cnec2 + halfline2Cnec2", "pst2");
        assertFalse(params.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        assertEquals(expectedCnecPstMap, params.getDoNotOptimizeCnecsSecuredByTheirPst());
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
        Mockito.when(loopFlowModuleConfig.getEnumProperty(eq("approximation"), eq(LoopFlowParametersExtension.Approximation.class), any())).thenReturn(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("FR", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParametersConfigLoader configLoader = new LoopFlowParametersConfigLoader();
        LoopFlowParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        assertEquals(32, parameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        assertEquals(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO, parameters.getApproximation());
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
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN_IN_AMPERE");
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("rao-range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(32));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, parameters.getObjectiveFunctionParameters().getType());
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
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoParameters.load(parameters, platformCfg));
        assertEquals("{na12  contains too few or too many occurences of \"{ or \"}", exception.getMessage());
    }

    @Test
    void inconsistentPredefinedCombinations2() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("rao-topological-actions-optimization");
        topoActionsModuleConfig.setStringListProperty("predefined-combinations", List.of("{na12} - {na22}", "{na41} + {na5} + {na6}"));
        RaoParameters parameters = new RaoParameters();
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoParameters.load(parameters, platformCfg));
        assertEquals("{na12} - {na22} contains too few or too many occurences of \"{ or \"}", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("generateIntMap")
    void inconsistentStringIntMap(List<String> source, String message) {
        MapModuleConfig raUsageLimitsModuleConfig = platformCfg.createModuleConfig("rao-ra-usage-limits-per-contingency");
        raUsageLimitsModuleConfig.setStringListProperty("max-curative-topo-per-tso", source);
        RaoParameters parameters = new RaoParameters();
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoParameters.load(parameters, platformCfg));
        assertEquals(message, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("generateStringStringMap")
    void inconsistentStringStringMap(List<String> source, String message) {
        MapModuleConfig notOptimizedModuleConfig = platformCfg.createModuleConfig("rao-not-optimized-cnecs");
        notOptimizedModuleConfig.setStringListProperty("do-not-optimize-cnec-secured-by-its-pst", source);
        RaoParameters parameters = new RaoParameters();
        FaraoException exception = assertThrows(FaraoException.class, () -> RaoParameters.load(parameters, platformCfg));
        assertEquals(message, exception.getMessage());
    }

    @Test
    void inconsistentLoopFlowCountries() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getStringListProperty(eq("countries"), anyList())).thenReturn(List.of("France", "ES", "PT"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParametersConfigLoader configLoader = new LoopFlowParametersConfigLoader();
        FaraoException exception = assertThrows(FaraoException.class, () -> configLoader.load(mockedPlatformConfig));
        assertEquals("[France] could not be recognized as a country", exception.getMessage());
    }

    @Test
    void inconsistentRelativeMarginsBoundaries1() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR}{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        FaraoException exception = assertThrows(FaraoException.class, () -> configLoader.load(mockedPlatformConfig));
        assertEquals("ZoneToZonePtdfDefinition should have the following syntax: {Code_1}-{Code_2}+{Code_3}... where Code_i are 16-characters EI codes or 2-characters country codes.", exception.getMessage());
    }

    @Test
    void inconsistentRelativeMarginsBoundaries2() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getStringListProperty(eq("ptdf-boundaries"), anyList())).thenReturn(List.of("{FR-{BE}"));
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("rao-relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        FaraoException exception = assertThrows(FaraoException.class, () -> configLoader.load(mockedPlatformConfig));
        assertEquals("ZoneToZonePtdfDefinition should have the following syntax: {Code_1}-{Code_2}+{Code_3}... where Code_i are 16-characters EI codes or 2-characters country codes.", exception.getMessage());
    }
}
