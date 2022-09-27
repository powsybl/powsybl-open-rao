/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

/*import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;*/

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PerimeterResultImplTest {
    /*private static final double DOUBLE_TOLERANCE = 1e-3;

    private PerimeterResultImpl perimeterResultImpl;
    private RangeActionSetpointResult prePerimeterRangeActionActivationResult;
    private OptimizationResult optimizationResult;
    private State mainOptimizationState;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private FlowCnec flowCnec1;
    private FlowCnec flowCnec2;
    private NetworkAction na1;
    private NetworkAction na2;
    private PstRangeAction pst1;
    private PstRangeAction pst2;

    @Before
    public void setUp() {
        prePerimeterRangeActionActivationResult = mock(RangeActionSetpointResult.class);
        optimizationResult = mock(OptimizationResult.class);

        mainOptimizationState = mock(State.class);
        ra1 = mock(RangeAction.class);
        ra2 = mock(RangeAction.class);
        when(prePerimeterRangeActionActivationResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));

        flowCnec1 = mock(FlowCnec.class);
        flowCnec2 = mock(FlowCnec.class);

        na1 = mock(NetworkAction.class);
        na2 = mock(NetworkAction.class);

        pst1 = mock(PstRangeAction.class);
        pst2 = mock(PstRangeAction.class);

        perimeterResultImpl = new PerimeterResultImpl(prePerimeterRangeActionActivationResult, optimizationResult);
    }

    @Test
    public void testGetActivatedRangeActions() {
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra1)).thenReturn(5.);
        when(optimizationResult.getOptimizedSetpoint(ra1, mainOptimizationState)).thenReturn(5.);
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra2)).thenReturn(15.);
        when(optimizationResult.getOptimizedSetpoint(ra2, mainOptimizationState)).thenReturn(50.);
        when(optimizationResult.getActivatedRangeActions(mainOptimizationState)).thenReturn(Set.of(ra2));
        assertEquals(Set.of(ra2), perimeterResultImpl.getActivatedRangeActions(mainOptimizationState));
    }

    @Test
    public void testGetFlow() {
        when(optimizationResult.getFlow(flowCnec1, Unit.MEGAWATT)).thenReturn(100.);
        when(optimizationResult.getFlow(flowCnec2, Unit.AMPERE)).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getFlow(flowCnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getFlow(flowCnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(optimizationResult.getCommercialFlow(flowCnec1, Unit.MEGAWATT)).thenReturn(100.);
        when(optimizationResult.getCommercialFlow(flowCnec2, Unit.AMPERE)).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getCommercialFlow(flowCnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getCommercialFlow(flowCnec2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(optimizationResult.getPtdfZonalSum(flowCnec1)).thenReturn(100.);
        when(optimizationResult.getPtdfZonalSum(flowCnec2)).thenReturn(200.);
        assertEquals(100., perimeterResultImpl.getPtdfZonalSum(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(200., perimeterResultImpl.getPtdfZonalSum(flowCnec2), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSums() {
        Map<FlowCnec, Double> map = Map.of(flowCnec1, 100., flowCnec2, 200.);
        when(optimizationResult.getPtdfZonalSums()).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getPtdfZonalSums());
    }

    @Test
    public void testIsActivated() {
        when(optimizationResult.isActivated(na1)).thenReturn(true);
        when(optimizationResult.isActivated(na2)).thenReturn(false);
        assertTrue(perimeterResultImpl.isActivated(na1));
        assertFalse(perimeterResultImpl.isActivated(na2));
    }

    @Test
    public void testGetActivatedNetworkActions() {
        when(optimizationResult.getActivatedNetworkActions()).thenReturn(Set.of(na1));
        assertEquals(Set.of(na1), perimeterResultImpl.getActivatedNetworkActions());
    }

    @Test
    public void testGetFunctionalCost() {
        when(optimizationResult.getFunctionalCost()).thenReturn(1000.);
        assertEquals(1000., perimeterResultImpl.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMostLimitingElements() {
        when(optimizationResult.getMostLimitingElements(anyInt())).thenReturn(List.of(flowCnec2, flowCnec1));
        assertEquals(List.of(flowCnec2, flowCnec1), perimeterResultImpl.getMostLimitingElements(100));
    }

    @Test
    public void testGetVirtualCost() {
        when(optimizationResult.getVirtualCost()).thenReturn(1000.);
        assertEquals(1000., perimeterResultImpl.getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCostNames() {
        when(optimizationResult.getVirtualCostNames()).thenReturn(Set.of("lf", "mnec"));
        assertEquals(Set.of("lf", "mnec"), perimeterResultImpl.getVirtualCostNames());
    }

    @Test
    public void testGetVirtualCostByName() {
        when(optimizationResult.getVirtualCost("lf")).thenReturn(1000.);
        when(optimizationResult.getVirtualCost("mnec")).thenReturn(2000.);
        assertEquals(1000., perimeterResultImpl.getVirtualCost("lf"), DOUBLE_TOLERANCE);
        assertEquals(2000., perimeterResultImpl.getVirtualCost("mnec"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCostlyElements() {
        when(optimizationResult.getCostlyElements("lf", 100)).thenReturn(List.of(flowCnec2, flowCnec1));
        when(optimizationResult.getCostlyElements("mnec", 100)).thenReturn(List.of(flowCnec1));
        assertEquals(List.of(flowCnec2, flowCnec1), perimeterResultImpl.getCostlyElements("lf", 100));
        assertEquals(List.of(flowCnec1), perimeterResultImpl.getCostlyElements("mnec", 100));
    }

    @Test
    public void testGetRangeActions() {
        assertEquals(Set.of(ra1, ra2), perimeterResultImpl.getRangeActions());
    }

    @Test
    public void testGetOptimizedTap() {
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(pst1));

        when(optimizationResult.getOptimizedTap(pst1, mainOptimizationState)).thenReturn(10);
        when(optimizationResult.getOptimizedTap(pst2, mainOptimizationState)).thenThrow(new FaraoException("absent mock"));
        when(prePerimeterRangeActionActivationResult.getTap(pst2)).thenReturn(3);

        assertEquals(10, perimeterResultImpl.getOptimizedTap(pst1, mainOptimizationState));
        assertEquals(3, perimeterResultImpl.getOptimizedTap(pst2, mainOptimizationState));
    }

    @Test
    public void testGetOptimizedSetPoint() {
        when(optimizationResult.getRangeActions()).thenReturn(Set.of(ra1));
        when(optimizationResult.getOptimizedSetpoint(ra1, mainOptimizationState)).thenReturn(10.7);
        when(prePerimeterRangeActionActivationResult.getSetpoint(ra2)).thenReturn(3.5);

        assertEquals(10.7, perimeterResultImpl.getOptimizedSetpoint(ra1, mainOptimizationState), DOUBLE_TOLERANCE);
        assertEquals(3.5, perimeterResultImpl.getOptimizedSetpoint(ra2, mainOptimizationState), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetOptimizedTaps() {
        Map<PstRangeAction, Integer> map = Map.of(pst1, 10, pst2, 5);
        when(optimizationResult.getOptimizedTapsOnState(mainOptimizationState)).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getOptimizedTapsOnState(mainOptimizationState));
    }

    @Test
    public void testGetOptimizedSetPoints() {
        Map<RangeAction<?>, Double> map = Map.of(pst1, 10.6, pst2, 5.8, ra1, 52.5, ra2, 100.6);
        when(optimizationResult.getOptimizedSetpointsOnState(mainOptimizationState)).thenReturn(map);
        assertEquals(map, perimeterResultImpl.getOptimizedSetpointsOnState(mainOptimizationState));
    }

    @Test
    public void testGetSensitivityStatus() {
        when(optimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, perimeterResultImpl.getSensitivityStatus());
        when(optimizationResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        assertEquals(ComputationStatus.FALLBACK, perimeterResultImpl.getSensitivityStatus());
    }

    @Test
    public void testGetSensitivityValueOnRa() {
        assertEquals(0., perimeterResultImpl.getSensitivityValue(flowCnec1, ra1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., perimeterResultImpl.getSensitivityValue(flowCnec1, ra2, Unit.AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetSensitivityValueOnGlsk() {
        assertEquals(0., perimeterResultImpl.getSensitivityValue(flowCnec1, mock(SensitivityVariableSet.class), Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0., perimeterResultImpl.getSensitivityValue(flowCnec1, mock(SensitivityVariableSet.class), Unit.AMPERE), DOUBLE_TOLERANCE);
    }*/
}
