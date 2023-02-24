/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.rao_api.parameters.extensions.*;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersConfigTest {
    private PlatformConfig mockedPlatformConfig;
    private InMemoryPlatformConfig platformCfg;
    private FileSystem fileSystem;
    static double DOUBLE_TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        mockedPlatformConfig = Mockito.mock(PlatformConfig.class);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    // TODO : test with multiple elements
    @Test
    public void checkObjectiveFunctionConfig() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", "MAX_MIN_RELATIVE_MARGIN_IN_AMPERE");
        objectiveFunctionModuleConfig.setStringProperty("forbid-cost-increase", Objects.toString(true));
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        objectiveFunctionModuleConfig.setStringProperty("preventive-stop-criterion", "MIN_OBJECTIVE");
        objectiveFunctionModuleConfig.setStringProperty("curative-stop-criterion", "PREVENTIVE_OBJECTIVE");

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, objectiveFunctionParameters.getObjectiveFunctionType());
        assertEquals(true, objectiveFunctionParameters.getForbidCostIncrease());
        assertEquals(123, objectiveFunctionParameters.getCurativeMinObjImprovement(), DOUBLE_TOLERANCE);
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, objectiveFunctionParameters.getPreventiveStopCriterion());
        assertEquals(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE, objectiveFunctionParameters.getCurativeStopCriterion());
    }

    @Test
    public void checkRangeActionsOptimizationConfig() {
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(4));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-penalty-cost", Objects.toString(44));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-sensitivity-threshold", Objects.toString(7));
        rangeActionsOptimizationModuleConfig.setStringProperty("pst-model", "APPROXIMATED_INTEGERS");
        rangeActionsOptimizationModuleConfig.setStringProperty("hvdc-penalty-cost", Objects.toString(33));
        rangeActionsOptimizationModuleConfig.setStringProperty("hvdc-sensitivity-threshold", Objects.toString(8));
        rangeActionsOptimizationModuleConfig.setStringProperty("injection-ra-penalty-cost", Objects.toString(22));
        rangeActionsOptimizationModuleConfig.setStringProperty("injection-ra-sensitivity-threshold", Objects.toString(9));
        MapModuleConfig linearOptimizationSolverModuleConfig = platformCfg.createModuleConfig("linear-optimization-solver");
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
        assertEquals(33, params.getHvdcPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(8, params.getHvdcSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getInjectionRaPenaltyCost(), DOUBLE_TOLERANCE);
        assertEquals(9, params.getInjectionRaSensitivityThreshold(), DOUBLE_TOLERANCE);
        assertEquals(RangeActionsOptimizationParameters.Solver.XPRESS, params.getLinearOptimizationSolver().getSolver());
        assertEquals(22, params.getLinearOptimizationSolver().getRelativeMipGap(), DOUBLE_TOLERANCE);
        assertEquals("blabla", params.getLinearOptimizationSolver().getSolverSpecificParameters());
    }

    // TODO : predefined combinations yaml
    @Test
    public void checkTopoActionsOptimizationConfig() {
        MapModuleConfig topoActionsModuleConfig = platformCfg.createModuleConfig("topological-actions-optimization");
        topoActionsModuleConfig.setStringProperty("max-search-tree-depth", Objects.toString(3));
        topoActionsModuleConfig.setStringProperty("predefined-combinations", "[ \"na12 + na22\", \"na41 + na5 + na6\"]");
        topoActionsModuleConfig.setStringProperty("relative-minimum-impact-threshold", Objects.toString(0.9));
        topoActionsModuleConfig.setStringProperty("absolute-minimum-impact-threshold", Objects.toString(22));
        topoActionsModuleConfig.setStringProperty("skip-actions-far-from-most-limiting-element", Objects.toString(true));
        topoActionsModuleConfig.setStringProperty("max-number-of-boundaries-for-skipping-actions", Objects.toString(3333));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        TopoOptimizationParameters params = parameters.getTopoOptimizationParameters();
        assertEquals(3, params.getMaxSearchTreeDepth(), DOUBLE_TOLERANCE);
        //assertEquals("[ "na1 + na2", "na4 + na5 + na6"]", params.getPredefinedCombinations());
        assertEquals(0.9, params.getRelativeMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertEquals(22, params.getAbsoluteMinImpactThreshold(), DOUBLE_TOLERANCE);
        assertTrue(params.getSkipActionsFarFromMostLimitingElement());
        assertEquals(3333, params.getMaxNumberOfBoundariesForSkippingActions(), DOUBLE_TOLERANCE);
    }

    @Test
    public void checkMultiThreadingConfig() {
        MapModuleConfig multiThreadingModuleConfig = platformCfg.createModuleConfig("multi-threading");
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
    public void checkSecondPreventiveRaoConfig() {
        MapModuleConfig secondPreventiveRaoModuleConfig = platformCfg.createModuleConfig("second-preventive-rao");
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
    public void checkRaUsageLimitsPerContingencyConfig() {
        MapModuleConfig raUsageLimitsModuleConfig = platformCfg.createModuleConfig("ra-usage-limits-per-contingency");
        raUsageLimitsModuleConfig.setStringProperty("max-curative-ra", Objects.toString(3));
        raUsageLimitsModuleConfig.setStringProperty("max-curative-tso", Objects.toString(13));
        // TODO : yaml config. try with multiple tsos
//        raUsageLimitsModuleConfig.setStringProperty("max-curative-topo-per-tso", "{ \"ABC\" : 5 }");
//        raUsageLimitsModuleConfig.setStringProperty("max-curative-pst-per-tso", "{ \"DEF\" : 3 }");
//        raUsageLimitsModuleConfig.setStringProperty("max-curative-ra-per-tso", "{ \"GHI\" : 2 }");

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        RaUsageLimitsPerContingencyParameters params = parameters.getRaUsageLimitsPerContingencyParameters();
        assertEquals(3, params.getMaxCurativeRa(), DOUBLE_TOLERANCE);
        assertEquals(13, params.getMaxCurativeTso(), DOUBLE_TOLERANCE);
    }

    @Test
    public void checkNotOptimizedCnecsConfig() {
        MapModuleConfig notOptimizedModuleConfig = platformCfg.createModuleConfig("not-optimized-cnecs");
        notOptimizedModuleConfig.setStringProperty("do-not-optimize-curative-cnecs-for-tsos-without-cras", Objects.toString(false));
//        notOptimizedModuleConfig.setStringProperty("do-not-optimize-cnec-secured-by-its-pst", "{\"na1 + na2\" : \"na3\",\"na4 + na5\" : \"na6\"}");
        // TODO : yaml config.
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        NotOptimizedCnecsParameters params = parameters.getNotOptimizedCnecsParameters();
        assertFalse(params.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
    }

    @Test
    public void checkLoadFlowParametersConfig() {
        MapModuleConfig loadFlowModuleConfig = platformCfg.createModuleConfig("load-flow-and-sensitivity-computation");
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
    public void checkLoopFlowParametersConfig() {
        ModuleConfig loopFlowModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("acceptable-increase"), anyDouble())).thenReturn(32.);
        Mockito.when(loopFlowModuleConfig.getEnumProperty(eq("approximation"), eq(LoopFlowParametersExtension.Approximation.class), any())).thenReturn(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(loopFlowModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("loop-flow-parameters")).thenReturn(Optional.of(loopFlowModuleConfig));
        LoopFlowParametersConfigLoader configLoader = new LoopFlowParametersConfigLoader();
        LoopFlowParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        // TODO: yaml config
//        loopFlowModuleConfig.setStringProperty("countries", "[ \"FR\", \"ES;\", \"PT\" ]");

        assertEquals(32, parameters.getAcceptableIncrease(), DOUBLE_TOLERANCE);
        assertEquals(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO, parameters.getApproximation());
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void checkMnecParametersConfig() {
        ModuleConfig mnecModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("acceptable-margin-decrease"), anyDouble())).thenReturn(32.);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("violation-cost"), anyDouble())).thenReturn(43.);
        Mockito.when(mnecModuleConfig.getDoubleProperty(eq("constraint-adjustment-coefficient"), anyDouble())).thenReturn(45.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("mnec-parameters")).thenReturn(Optional.of(mnecModuleConfig));
        MnecParametersConfigLoader configLoader = new MnecParametersConfigLoader();
        MnecParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        assertEquals(32, parameters.getAcceptableMarginDecrease(), DOUBLE_TOLERANCE);
        assertEquals(43, parameters.getViolationCost(), DOUBLE_TOLERANCE);
        assertEquals(45, parameters.getConstraintAdjustmentCoefficient(), DOUBLE_TOLERANCE);
    }

    @Test
    public void checkRelativeMarginsConfig() {
        ModuleConfig relativeMarginsModuleConfig = Mockito.mock(ModuleConfig.class);
        Mockito.when(relativeMarginsModuleConfig.getDoubleProperty(eq("ptdf-sum-lower-bound"), anyDouble())).thenReturn(32.);
        Mockito.when(mockedPlatformConfig.getOptionalModuleConfig("relative-margins-parameters")).thenReturn(Optional.of(relativeMarginsModuleConfig));
        // TODO : yaml
//        relativeMarginsModuleConfig.setStringProperty("ptdf-boundaries", "[ \"{FR}-{BE}\", \"{FR}-{DE}\"]");
        RelativeMarginsParametersConfigLoader configLoader = new RelativeMarginsParametersConfigLoader();
        RelativeMarginsParametersExtension parameters = configLoader.load(mockedPlatformConfig);
        assertEquals(32, parameters.getPtdfSumLowerBound(), DOUBLE_TOLERANCE);
    }

    @Test
    public void checkMultipleConfigs() {
        MapModuleConfig objectiveFunctionModuleConfig = platformCfg.createModuleConfig("objective-function");
        objectiveFunctionModuleConfig.setStringProperty("type", Objects.toString("MAX_MIN_RELATIVE_MARGIN_IN_AMPERE"));
        objectiveFunctionModuleConfig.setStringProperty("curative-min-obj-improvement", Objects.toString(123.0));
        MapModuleConfig rangeActionsOptimizationModuleConfig = platformCfg.createModuleConfig("range-actions-optimization");
        rangeActionsOptimizationModuleConfig.setStringProperty("max-mip-iterations", Objects.toString(32));
        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        assertEquals(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, parameters.getObjectiveFunctionParameters().getObjectiveFunctionType());
        assertEquals(123, parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), 1e-6);
        assertEquals(32, parameters.getRangeActionsOptimizationParameters().getMaxMipIterations(), 1e-6);
        assertTrue(Objects.isNull(parameters.getExtension(LoopFlowParametersExtension.class)));
        assertTrue(Objects.isNull(parameters.getExtension(MnecParametersExtension.class)));
        assertTrue(Objects.isNull(parameters.getExtension(RelativeMarginsParametersExtension.class)));
    }
}
