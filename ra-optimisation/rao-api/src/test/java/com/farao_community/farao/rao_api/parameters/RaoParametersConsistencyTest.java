/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.EICode;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.iidm.network.Country;
import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

/**
 @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class RaoParametersConsistencyTest {
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
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.FR)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.ES)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode(Country.PT)), 1e-6);
    }

    @Test
    public void testSetBoundariesFromEiCodes() {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("{10YBE----------2}-{10YFR-RTE------C}", "{10YBE----------2}-{22Y201903144---9}"));
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(2, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("10YFR-RTE------C")), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("10YBE----------2")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(1).getWeight(new EICode("22Y201903144---9")), 1e-6);
    }

    @Test
    public void testSetBoundariesFromMixOfCodes() {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(RelativeMarginsParametersExtension.class, new RelativeMarginsParametersExtension());
        List<String> stringBoundaries = new ArrayList<>(Collections.singletonList("{BE}-{22Y201903144---9}+{22Y201903145---4}-{DE}"));
        parameters.getExtension(RelativeMarginsParametersExtension.class).setPtdfBoundariesFromString(stringBoundaries);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().size());
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.BE)), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode(Country.DE)), 1e-6);
        assertEquals(1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903145---4")), 1e-6);
        assertEquals(-1, parameters.getExtension(RelativeMarginsParametersExtension.class).getPtdfBoundaries().get(0).getWeight(new EICode("22Y201903144---9")), 1e-6);
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
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
    }

    @Test(expected = FaraoException.class)
    public void testIncompatibleParameters2() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();

        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
    }

    @Test
    public void testIncompatibleParameters3() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        assertEquals(Map.of("cnec1", "pst1"), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
    }

    @Test
    public void testIncompatibleParameters4() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(null);
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        assertEquals(Collections.emptyMap(), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());

    }

    @Test
    public void testIncompatibleParameters5() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(true);
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Collections.emptyMap());
        assertEquals(Collections.emptyMap(), nocp.getDoNotOptimizeCnecsSecuredByTheirPst());
    }

    @Test
    public void testIncompatibleParameters6() {
        RaoParameters parameters = new RaoParameters();
        NotOptimizedCnecsParameters nocp = parameters.getNotOptimizedCnecsParameters();
        nocp.setDoNotOptimizeCnecsSecuredByTheirPst(Map.of("cnec1", "pst1"));
        nocp.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(false);
        assertFalse(nocp.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
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
}
