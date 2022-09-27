/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

/*import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.PrePerimeterResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;
import static com.farao_community.farao.commons.Unit.*;*/

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class AutomatonPerimeterResultImplTest {
    /*private static final double DOUBLE_TOLERANCE = 1e-3;

    private State state1;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private PstRangeAction pstRangeActionShifted;
    private HvdcRangeAction hvdcRangeActionShifted;
    private RangeAction unshiftedRangeAction;
    private Map<RangeAction<?>, Double> rangeActionsWithSetpoint;
    private AutomatonPerimeterResultImpl result;
    private PrePerimeterResult postAutoSensitivity;

    @Before
    public void setUp() {
        state1 = mock(State.class);
        cnec1 = mock(FlowCnec.class);
        cnec2 = mock(FlowCnec.class);
        networkAction1 = mock(NetworkAction.class);
        networkAction2 = mock(NetworkAction.class);
        pstRangeActionShifted = mock(PstRangeAction.class);
        hvdcRangeActionShifted = mock(HvdcRangeAction.class);
        unshiftedRangeAction = mock(RangeAction.class);
        postAutoSensitivity = mock(PrePerimeterResult.class);
        // Define rangeActionsWithSetpoint
        rangeActionsWithSetpoint = new HashMap<>();
        rangeActionsWithSetpoint.put(pstRangeActionShifted, 1.0);
        rangeActionsWithSetpoint.put(hvdcRangeActionShifted, 2.0);
        rangeActionsWithSetpoint.put(unshiftedRangeAction, 3.0);
        result = new AutomatonPerimeterResultImpl(postAutoSensitivity, Set.of(networkAction1), Set.of(pstRangeActionShifted, hvdcRangeActionShifted), rangeActionsWithSetpoint, state1);
    }

    @Test
    public void testGetPostAutomatonSensitivityAnalysisOutput() {
        assertEquals(postAutoSensitivity, result.getPostAutomatonSensitivityAnalysisOutput());
    }

    @Test
    public void testGetFlow() {
        when(postAutoSensitivity.getFlow(cnec1, AMPERE)).thenReturn(10.);
        when(postAutoSensitivity.getFlow(cnec1, MEGAWATT)).thenReturn(100.);
        assertEquals(10., result.getFlow(cnec1, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., result.getFlow(cnec1, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCommercialFlow() {
        when(postAutoSensitivity.getCommercialFlow(cnec1, AMPERE)).thenReturn(10.);
        when(postAutoSensitivity.getCommercialFlow(cnec1, MEGAWATT)).thenReturn(100.);
        assertEquals(10., result.getCommercialFlow(cnec1, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., result.getCommercialFlow(cnec1, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSum() {
        when(postAutoSensitivity.getPtdfZonalSum(cnec1)).thenReturn(10.);
        assertEquals(10., result.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetPtdfZonalSums() {
        when(postAutoSensitivity.getPtdfZonalSums()).thenReturn(Map.of(cnec1, 0.1));
        assertEquals(Map.of(cnec1, 0.1), result.getPtdfZonalSums());
    }

    @Test
    public void testGetActivatedRangeActions() {
        assertEquals(Set.of(pstRangeActionShifted, hvdcRangeActionShifted), result.getActivatedRangeActions(state1));
    }

    @Test
    public void testIsNetworkActionActivated() {
        assertTrue(result.isActivated(networkAction1));
        assertFalse(result.isActivated(networkAction2));
    }

    @Test
    public void testGetActivatedNetworkActions() {
        assertEquals(Set.of(networkAction1), result.getActivatedNetworkActions());
    }

    @Test
    public void testGetFunctionalCost() {
        when(postAutoSensitivity.getFunctionalCost()).thenReturn(350.);
        assertEquals(350., result.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetMostLimitingElements() {
        when(postAutoSensitivity.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec1));
        assertEquals(List.of(cnec2, cnec1), result.getMostLimitingElements(100));
    }

    @Test
    public void testGetVirtualCost() {
        when(postAutoSensitivity.getVirtualCost()).thenReturn(350.);
        assertEquals(350., result.getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetVirtualCostNames() {
        when(postAutoSensitivity.getVirtualCostNames()).thenReturn(Set.of("lf", "mnec"));
        assertEquals(Set.of("lf", "mnec"), result.getVirtualCostNames());
    }

    @Test
    public void testGetVirtualCostByName() {
        when(postAutoSensitivity.getVirtualCost("lf")).thenReturn(350.);
        when(postAutoSensitivity.getVirtualCost("mnec")).thenReturn(3500.);
        assertEquals(350., result.getVirtualCost("lf"), DOUBLE_TOLERANCE);
        assertEquals(3500., result.getVirtualCost("mnec"), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetCostlyElements() {
        when(postAutoSensitivity.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(postAutoSensitivity.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec2, cnec1));
        assertEquals(List.of(cnec2), result.getCostlyElements("lf", 100));
        assertEquals(List.of(cnec2, cnec1), result.getCostlyElements("mnec", 1000));
    }

    @Test
    public void testGetRangeActions() {
        when(postAutoSensitivity.getRangeActions()).thenReturn(rangeActionsWithSetpoint.keySet());
        assertEquals(Set.of(pstRangeActionShifted, hvdcRangeActionShifted, unshiftedRangeAction), result.getRangeActions());
    }

    @Test
    //TODO modify
    public void testGetTapsAndSetpoints() {
        when(postAutoSensitivity.getSetpoint(pstRangeActionShifted)).thenReturn(rangeActionsWithSetpoint.get(pstRangeActionShifted));
        when(postAutoSensitivity.getSetpoint(unshiftedRangeAction)).thenReturn(rangeActionsWithSetpoint.get(unshiftedRangeAction));
        when(postAutoSensitivity.getSetpoint(unshiftedRangeAction)).thenReturn(rangeActionsWithSetpoint.get(unshiftedRangeAction));
        when(postAutoSensitivity.getSetpoint(hvdcRangeActionShifted)).thenReturn(rangeActionsWithSetpoint.get(hvdcRangeActionShifted));
        when(pstRangeActionShifted.convertAngleToTap(rangeActionsWithSetpoint.get(pstRangeActionShifted))).thenReturn(55);
        assertEquals(55, result.getOptimizedTap(pstRangeActionShifted, state1));
        assertEquals(1., result.getOptimizedSetpoint(pstRangeActionShifted, state1), DOUBLE_TOLERANCE);
        assertEquals(2., result.getOptimizedSetpoint(hvdcRangeActionShifted, state1), DOUBLE_TOLERANCE);
        assertEquals(3., result.getOptimizedSetpoint(unshiftedRangeAction, state1), DOUBLE_TOLERANCE);
        assertTrue(rangeActionsWithSetpoint.equals(result.getOptimizedSetpointsOnState(state1)));
        assertTrue(Map.of(pstRangeActionShifted, 55).equals(result.getOptimizedTapsOnState(state1)));
    }

    @Test
    public void testWrongState() {
        State wrongState = mock(State.class);
        assertThrows(FaraoException.class, () -> result.getActivatedRangeActions(wrongState));
    }

    @Test
    public void testGetSensitivityStatus() {
        when(postAutoSensitivity.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, result.getSensitivityStatus());
    }

    @Test
    public void testGetSensitivityOnRangeAction() {
        RangeAction<?> rangeAction = mock(RangeAction.class);
        when(postAutoSensitivity.getSensitivityValue(cnec1, rangeAction, MEGAWATT)).thenReturn(100.);
        when(postAutoSensitivity.getSensitivityValue(cnec1, rangeAction, AMPERE)).thenReturn(1000.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, rangeAction, MEGAWATT)).thenReturn(200.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, rangeAction, AMPERE)).thenReturn(2000.);
        assertEquals(100., result.getSensitivityValue(cnec1, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., result.getSensitivityValue(cnec1, rangeAction, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200., result.getSensitivityValue(cnec2, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2000., result.getSensitivityValue(cnec2, rangeAction, AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    public void testGetSensitivityOnLinearGlsk() {
        SensitivityVariableSet linearGlsk = mock(SensitivityVariableSet.class);
        when(postAutoSensitivity.getSensitivityValue(cnec1, linearGlsk, MEGAWATT)).thenReturn(100.);
        when(postAutoSensitivity.getSensitivityValue(cnec1, linearGlsk, AMPERE)).thenReturn(1000.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, linearGlsk, MEGAWATT)).thenReturn(200.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, linearGlsk, AMPERE)).thenReturn(2000.);
        assertEquals(100., result.getSensitivityValue(cnec1, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., result.getSensitivityValue(cnec1, linearGlsk, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200., result.getSensitivityValue(cnec2, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2000., result.getSensitivityValue(cnec2, linearGlsk, AMPERE), DOUBLE_TOLERANCE);
    }*/
}
