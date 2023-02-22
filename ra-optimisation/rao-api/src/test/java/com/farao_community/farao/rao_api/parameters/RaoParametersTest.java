/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginParametersExtension;
import com.google.auto.service.AutoService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Country;
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
// TODO : split in several tests : roundTrip, loadConfig, incompatibility
public class RaoParametersTest {

    private PlatformConfig config;
    private InMemoryPlatformConfig platformCfg;
    private FileSystem fileSystem;

    @Before
    public void setUp() {
        config = Mockito.mock(PlatformConfig.class);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformCfg = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    public void testExtensions() {
        RaoParameters parameters = new RaoParameters();
        DummyExtension dummyExtension = new DummyExtension();
        parameters.addExtension(DummyExtension.class, dummyExtension);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensions().contains(dummyExtension));
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void testNoExtensions() {
        RaoParameters parameters = new RaoParameters();

        assertEquals(0, parameters.getExtensions().size());
        assertFalse(parameters.getExtensions().contains(new DummyExtension()));
        assertFalse(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkConfig() {

        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("rao-with-mnec-limitation", Boolean.toString(true));
        moduleConfig.setStringProperty("rao-with-loop-flow-limitation", Boolean.toString(false));
        moduleConfig.setStringProperty("loop-flow-constraint-adjustment-coefficient", Objects.toString(15.0));
        moduleConfig.setStringProperty("loop-flow-violation-cost", Objects.toString(10.0));
        moduleConfig.setStringProperty("forbid-cost-increase", Boolean.toString(true));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        parameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());

        assertTrue(Objects.nonNull(parameters.getExtension(LoopFlowParametersExtension.class)));
        assertTrue(Objects.isNull(parameters.getExtension(MnecParametersExtension.class)));
        assertEquals(15., parameters.getExtension(LoopFlowParametersExtension.class).getConstraintAdjustmentCoefficient(), 1e-6);
        assertEquals(10., parameters.getExtension(LoopFlowParametersExtension.class).getViolationCost(), 1e-6);
        assertTrue(parameters.getObjectiveFunctionParameters().getForbidCostIncrease());
    }

    @Test
    public void testExtensionFromConfig() {
        RaoParameters parameters = RaoParameters.load(config);

        assertEquals(1, parameters.getExtensions().size());
        assertTrue(parameters.getExtensionByName("dummyExtension") instanceof DummyExtension);
        assertNotNull(parameters.getExtension(DummyExtension.class));
    }

    @Test
    public void checkMnecConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("mnec-acceptable-margin-diminution", Objects.toString(100.0));
        moduleConfig.setStringProperty("mnec-violation-cost", Objects.toString(5.0));
        moduleConfig.setStringProperty("mnec-constraint-adjustment-coefficient", Objects.toString(0.1));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        parameters.addExtension(MnecParametersExtension.class, MnecParametersExtension.loadDefault());

        assertEquals(100, parameters.getExtension(MnecParametersExtension.class).getAcceptableMarginDecrease(), 1e-6);
        assertEquals(5, parameters.getExtension(MnecParametersExtension.class).getViolationCost(), 1e-6);
        assertEquals(0.1, parameters.getExtension(MnecParametersExtension.class).getConstraintAdjustmentCoefficient(), 1e-6);
    }

    @Test
    public void checkPtdfConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringListProperty("relative-margin-ptdf-boundaries", new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}")));
        moduleConfig.setStringProperty("ptdf-sum-lower-bound", Objects.toString(5.0));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        parameters.addExtension(RelativeMarginParametersExtension.class, RelativeMarginParametersExtension.loadDefault());

        assertEquals(5, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfSumLowerBound(), 1e-6);
        assertEquals(2, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(2, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    public void checkLoopFlowConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("loop-flow-approximation", "UPDATE_PTDF_WITH_TOPO");
        moduleConfig.setStringProperty("loop-flow-constraint-adjustment-coefficient", Objects.toString(5.0));
        moduleConfig.setStringProperty("loop-flow-violation-cost", Objects.toString(20.6));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);
        parameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());

        assertEquals(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO, parameters.getExtension(LoopFlowParametersExtension.class).getApproximation());
        assertEquals(5, parameters.getExtension(LoopFlowParametersExtension.class).getConstraintAdjustmentCoefficient(), 1e-6);
        assertEquals(20.6, parameters.getExtension(LoopFlowParametersExtension.class).getViolationCost(), 1e-6);
    }

    @Test
    public void checkPerimetersParallelConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("perimeters-in-parallel", Objects.toString(10));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(10, parameters.getMultithreadingParameters().getContingencyScenariosInParallel());
    }

    @Test
    public void checkSolverAndMipConfig() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("pst-optimization-approximation", "APPROXIMATED_INTEGERS");
        moduleConfig.setStringProperty("optimization-solver", "XPRESS");
        moduleConfig.setStringProperty("relative-mip-gap", Objects.toString(1e-3));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS, parameters.getRangeActionsOptimizationParameters().getPstModel());
        assertEquals(RangeActionsOptimizationParameters.Solver.XPRESS, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver());
        assertEquals(1e-3, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap(), 1e-6);
    }

    @Test
    public void checkInjectionRaParameters() {
        MapModuleConfig moduleConfig = platformCfg.createModuleConfig("rao-parameters");
        moduleConfig.setStringProperty("injection-ra-penalty-cost", Objects.toString(1.2));
        moduleConfig.setStringProperty("injection-ra-sensitivity-threshold", Objects.toString(0.55));

        RaoParameters parameters = new RaoParameters();
        RaoParameters.load(parameters, platformCfg);

        assertEquals(1.2, parameters.getRangeActionsOptimizationParameters().getInjectionRaPenaltyCost(), 1e-3);
        assertEquals(0.55, parameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold(), 1e-3);
    }

    @Test
    public void testUpdatePtdfWithTopo() {
        assertFalse(LoopFlowParametersExtension.Approximation.FIXED_PTDF.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithTopologicalChange());
        assertTrue(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithTopologicalChange());
    }

    @Test
    public void testUpdatePtdfWithPst() {
        assertFalse(LoopFlowParametersExtension.Approximation.FIXED_PTDF.shouldUpdatePtdfWithPstChange());
        assertFalse(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO.shouldUpdatePtdfWithPstChange());
        assertTrue(LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO_AND_PST.shouldUpdatePtdfWithPstChange());
    }

    @Test
    public void testSetBoundariesFromCountryCodes() {
        RaoParameters parameters = new RaoParameters();
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{FR}-{ES}", "{ES}-{PT}"));
        parameters.addExtension(RelativeMarginParametersExtension.class, RelativeMarginParametersExtension.loadDefault());
        parameters.getExtension(RelativeMarginParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    public void testSetBoundariesFromEiCodes() {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(RelativeMarginParametersExtension.class, RelativeMarginParametersExtension.loadDefault());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{10YBE----------2}-{10YFR-RTE------C}", "{10YBE----------2}-{22Y201903144---9}"));
        parameters.getExtension(RelativeMarginParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(2, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YFR-RTE------C")), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    public void testSetBoundariesFromMixOfCodes() {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(RelativeMarginParametersExtension.class, RelativeMarginParametersExtension.loadDefault());
        List<String> stringBoundaries = new ArrayList<>(Collections.singletonList("{BE}-{22Y201903144---9}+{22Y201903145---4}-{DE}"));
        parameters.getExtension(RelativeMarginParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.BE)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.DE)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903145---4")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    public void testRelativePositiveMargins() {
        assertTrue(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertTrue(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT.relativePositiveMargins());
        assertFalse(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE.relativePositiveMargins());
        assertFalse(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT.relativePositiveMargins());
    }

    @Test
    public void testRelativeNetworkActionMinimumImpactThresholdBounds() {
        RaoParameters parameters = new RaoParameters();
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(-0.5);
        assertEquals(0, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
        parameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(1.1);
        assertEquals(1, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold(), 1e-6);
    }

    @Test
    public void testMaxNumberOfBoundariesForSkippingNetworkActionsBounds() {
        RaoParameters parameters = new RaoParameters();
        TopoOptimizationParameters topoOptimizationParameters = parameters.getTopoOptimizationParameters();
        topoOptimizationParameters.setMaxNumberOfBoundariesForSkippingActions(300);
        assertEquals(300, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions());
        topoOptimizationParameters.setMaxNumberOfBoundariesForSkippingActions(-2);
        assertEquals(0, topoOptimizationParameters.getMaxNumberOfBoundariesForSkippingActions());
    }

    @Test
    public void testNegativeCurativeRaoMinObjImprovement() {
        RaoParameters parameters = new RaoParameters();
        ObjectiveFunctionParameters objectiveFunctionParameters = parameters.getObjectiveFunctionParameters();
        objectiveFunctionParameters.setCurativeMinObjImprovement(100);
        assertEquals(100, objectiveFunctionParameters.getCurativeMinObjImprovement(), 1e-6);
        objectiveFunctionParameters.setCurativeMinObjImprovement(-100);
        assertEquals(100, objectiveFunctionParameters.getCurativeMinObjImprovement(), 1e-6);
    }

    @Test
    public void testNonNullMaps() {
        RaoParameters parameters = new RaoParameters();
        RaUsageLimitsPerContingencyParameters rulpcp = parameters.getRaUsageLimitsPerContingencyParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();

        // default
        assertNotNull(rulpcp.getMaxCurativeRaPerTso());
        assertTrue(rulpcp.getMaxCurativeRaPerTso().isEmpty());

        assertNotNull(rulpcp.getMaxCurativePstPerTso());
        assertTrue(rulpcp.getMaxCurativePstPerTso().isEmpty());

        assertNotNull(rulpcp.getMaxCurativeTopoPerTso());
        assertTrue(rulpcp.getMaxCurativeTopoPerTso().isEmpty());

        assertNotNull(nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
        assertTrue(nocp.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty());

        // using setters
        rulpcp.setMaxCurativeRaPerTso(Map.of("fr", 2));
        rulpcp.setMaxCurativeRaPerTso(null);
        assertNotNull(rulpcp.getMaxCurativeRaPerTso());
        assertTrue(rulpcp.getMaxCurativeRaPerTso().isEmpty());

        rulpcp.setMaxCurativePstPerTso(Map.of("fr", 2));
        rulpcp.setMaxCurativePstPerTso(null);
        assertNotNull(rulpcp.getMaxCurativePstPerTso());
        assertTrue(rulpcp.getMaxCurativePstPerTso().isEmpty());

        rulpcp.setMaxCurativeTopoPerTso(Map.of("fr", 2));
        rulpcp.setMaxCurativeTopoPerTso(null);
        assertNotNull(rulpcp.getMaxCurativeTopoPerTso());
        assertTrue(rulpcp.getMaxCurativeTopoPerTso().isEmpty());

        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(null);
        assertNotNull(nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
        assertTrue(nocp.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty());
    }

    @Test
    public void testIllegalValues() {
        RaoParameters parameters = new RaoParameters();
        RaUsageLimitsPerContingencyParameters rulpcp = parameters.getRaUsageLimitsPerContingencyParameters();

        rulpcp.setMaxCurativeRa(2);
        rulpcp.setMaxCurativeRa(-2);
        assertEquals(0, rulpcp.getMaxCurativeRa());

        rulpcp.setMaxCurativeTso(2);
        rulpcp.setMaxCurativeTso(-2);
        assertEquals(0, rulpcp.getMaxCurativeTso());
    }

    @Test(expected = FaraoException.class)
    public void testIncompatibleParameters1() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();

        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
    }

    @Test(expected = FaraoException.class)
    public void testIncompatibleParameters2() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();

        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
    }

    @Test
    public void testIncompatibleParameters3() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        assertEquals(Map.of("cnec1", "pst1"), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
    }

    @Test
    public void testIncompatibleParameters4() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(null);
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        assertEquals(Collections.emptyMap(), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());

    }

    @Test
    public void testIncompatibleParameters5() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Collections.emptyMap());
        assertEquals(Collections.emptyMap(), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
    }

    @Test
    public void testIncompatibleParameters6() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        assertTrue(nocp.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
    }

    @Test
    // TODO : update
    public void testLoad() {
        ModuleConfig raoParametersModule = Mockito.mock(ModuleConfig.class);
        Mockito.when(raoParametersModule.getEnumProperty(eq("preventive-stop-criterion"), eq(ObjectiveFunctionParameters.PreventiveStopCriterion.class), any())).thenReturn(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE);
        Mockito.when(raoParametersModule.getIntProperty(eq("maximum-search-depth"), anyInt())).thenReturn(2);
        Mockito.when(raoParametersModule.getDoubleProperty(eq("relative-network-action-minimum-impact-threshold"), anyDouble())).thenReturn(0.1);
        Mockito.when(raoParametersModule.getDoubleProperty(eq("absolute-network-action-minimum-impact-threshold"), anyDouble())).thenReturn(20.0);
        Mockito.when(raoParametersModule.getIntProperty(eq("preventive-leaves-in-parallel"), anyInt())).thenReturn(4);
        Mockito.when(raoParametersModule.getIntProperty(eq("curative-leaves-in-parallel"), anyInt())).thenReturn(2);
        Mockito.when(raoParametersModule.getBooleanProperty(eq("skip-network-actions-far-from-most-limiting-element"), anyBoolean())).thenReturn(true);
        Mockito.when(raoParametersModule.getIntProperty(eq("max-number-of-boundaries-for-skipping-network-actions"), anyInt())).thenReturn(1);
        Mockito.when(raoParametersModule.getEnumProperty(eq("curative-rao-stop-criterion"), eq(ObjectiveFunctionParameters.CurativeStopCriterion.class), any())).thenReturn(ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE);
        Mockito.when(raoParametersModule.getDoubleProperty(eq("curative-rao-min-obj-improvement"), anyDouble())).thenReturn(456.0);
        Mockito.when(raoParametersModule.getBooleanProperty(eq("curative-rao-optimize-operators-not-sharing-cras"), anyBoolean())).thenReturn(false);
        Mockito.when(raoParametersModule.getEnumProperty(eq("second-preventive-optimization-condition"), eq(SecondPreventiveRaoParameters.ExecutionCondition.class), any())).thenReturn(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE);
        Mockito.when(raoParametersModule.getBooleanProperty(eq("global-opt-in-second-preventive"), anyBoolean())).thenReturn(true);
        Mockito.when(raoParametersModule.getBooleanProperty(eq("second-preventive-hint-from-first-preventive"), anyBoolean())).thenReturn(true);

        Mockito.when(config.getOptionalModuleConfig("search-tree-rao-parameters")).thenReturn(Optional.of(raoParametersModule));

        RaoParameters parameters = RaoParameters.load(config);
        assertEquals(ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE, parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
    }

    @Test
    public void testIncompatibleMaxCraParameters() {
        RaoParameters parameters = new RaoParameters();
        RaUsageLimitsPerContingencyParameters rulpcp = parameters.getRaUsageLimitsPerContingencyParameters();

        rulpcp.setMaxCurativeRaPerTso(Map.of("RTE", 5, "REE", 1));

        Exception exception = assertThrows(FaraoException.class, () -> rulpcp.setMaxCurativeTopoPerTso(Map.of("RTE", 6)));
        assertEquals("TSO RTE has a maximum number of allowed CRAs smaller than the number of allowed topological CRAs. This is not supported.", exception.getMessage());
        assertTrue(rulpcp.getMaxCurativeTopoPerTso().isEmpty());

        exception = assertThrows(FaraoException.class, () -> rulpcp.setMaxCurativePstPerTso(Map.of("REE", 2)));
        assertEquals("TSO REE has a maximum number of allowed CRAs smaller than the number of allowed PST CRAs. This is not supported.", exception.getMessage());
        assertTrue(rulpcp.getMaxCurativePstPerTso().isEmpty());
    }

    private static class DummyExtension extends AbstractExtension<RaoParameters> {

        @Override
        public String getName() {
            return "dummyExtension";
        }
    }

    @AutoService(RaoParameters.ConfigLoader.class)
    public static class DummyLoader implements RaoParameters.ConfigLoader<DummyExtension> {

        @Override
        public DummyExtension load(PlatformConfig platformConfig) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummyExtension";
        }

        @Override
        public String getCategoryName() {
            return "rao-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

}
