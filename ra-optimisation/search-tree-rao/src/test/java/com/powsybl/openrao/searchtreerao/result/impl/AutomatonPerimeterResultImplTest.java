/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.data.cracapi.cnec.Side.RIGHT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class AutomatonPerimeterResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private State state1;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;
    private PstRangeAction pstRangeActionShifted;
    private HvdcRangeAction hvdcRangeActionShifted;
    private RangeAction<?> unshiftedRangeAction;
    private Map<RangeAction<?>, Double> rangeActionsWithSetpoint;
    private AutomatonPerimeterResultImpl result;
    private PrePerimeterResult postAutoSensitivity;

    @BeforeEach
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
        result = new AutomatonPerimeterResultImpl(postAutoSensitivity, Set.of(networkAction1), Set.of(), Set.of(pstRangeActionShifted, hvdcRangeActionShifted), rangeActionsWithSetpoint, state1);
    }

    @Test
    void testGetPostAutomatonSensitivityAnalysisOutput() {
        assertEquals(postAutoSensitivity, result.getPostAutomatonSensitivityAnalysisOutput());
    }

    @Test
    void testGetFlow() {
        when(postAutoSensitivity.getFlow(cnec1, RIGHT, AMPERE)).thenReturn(10.);
        when(postAutoSensitivity.getFlow(cnec1, RIGHT, MEGAWATT)).thenReturn(100.);
        assertEquals(10., result.getFlow(cnec1, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., result.getFlow(cnec1, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCommercialFlow() {
        when(postAutoSensitivity.getCommercialFlow(cnec1, RIGHT, AMPERE)).thenReturn(10.);
        when(postAutoSensitivity.getCommercialFlow(cnec1, RIGHT, MEGAWATT)).thenReturn(100.);
        assertEquals(10., result.getCommercialFlow(cnec1, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(100., result.getCommercialFlow(cnec1, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSum() {
        when(postAutoSensitivity.getPtdfZonalSum(cnec1, RIGHT)).thenReturn(10.);
        assertEquals(10., result.getPtdfZonalSum(cnec1, RIGHT), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetPtdfZonalSums() {
        when(postAutoSensitivity.getPtdfZonalSums()).thenReturn(Map.of(cnec1, Map.of(RIGHT, 0.1)));
        assertEquals(Map.of(cnec1, Map.of(RIGHT, 0.1)), result.getPtdfZonalSums());
    }

    @Test
    void testGetActivatedRangeActions() {
        assertEquals(Set.of(pstRangeActionShifted, hvdcRangeActionShifted), result.getActivatedRangeActions(state1));
    }

    @Test
    void testIsNetworkActionActivated() {
        assertTrue(result.isActivated(networkAction1));
        assertFalse(result.isActivated(networkAction2));
    }

    @Test
    void testGetActivatedNetworkActions() {
        assertEquals(Set.of(networkAction1), result.getActivatedNetworkActions());
        assertEquals(Set.of(networkAction1), result.getSimulatedNetworkActions());
        assertEquals(Set.of(), result.getSelectedNetworkActions());
    }

    @Test
    void testGetFunctionalCost() {
        when(postAutoSensitivity.getFunctionalCost()).thenReturn(350.);
        assertEquals(350., result.getFunctionalCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetMostLimitingElements() {
        when(postAutoSensitivity.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec1));
        assertEquals(List.of(cnec2, cnec1), result.getMostLimitingElements(100));
    }

    @Test
    void testGetVirtualCost() {
        when(postAutoSensitivity.getVirtualCost()).thenReturn(350.);
        assertEquals(350., result.getVirtualCost(), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetVirtualCostNames() {
        when(postAutoSensitivity.getVirtualCostNames()).thenReturn(Set.of("lf", "mnec"));
        assertEquals(Set.of("lf", "mnec"), result.getVirtualCostNames());
    }

    @Test
    void testGetVirtualCostByName() {
        when(postAutoSensitivity.getVirtualCost("lf")).thenReturn(350.);
        when(postAutoSensitivity.getVirtualCost("mnec")).thenReturn(3500.);
        assertEquals(350., result.getVirtualCost("lf"), DOUBLE_TOLERANCE);
        assertEquals(3500., result.getVirtualCost("mnec"), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetCostlyElements() {
        when(postAutoSensitivity.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(cnec2));
        when(postAutoSensitivity.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(cnec2, cnec1));
        assertEquals(List.of(cnec2), result.getCostlyElements("lf", 100));
        assertEquals(List.of(cnec2, cnec1), result.getCostlyElements("mnec", 1000));
    }

    @Test
    void testGetRangeActions() {
        when(postAutoSensitivity.getRangeActions()).thenReturn(rangeActionsWithSetpoint.keySet());
        assertEquals(Set.of(pstRangeActionShifted, hvdcRangeActionShifted, unshiftedRangeAction), result.getRangeActions());
    }

    @Test
    //TODO modify
    void testGetTapsAndSetpoints() {
        when(postAutoSensitivity.getSetpoint(pstRangeActionShifted)).thenReturn(rangeActionsWithSetpoint.get(pstRangeActionShifted));
        when(postAutoSensitivity.getSetpoint(unshiftedRangeAction)).thenReturn(rangeActionsWithSetpoint.get(unshiftedRangeAction));
        when(postAutoSensitivity.getSetpoint(unshiftedRangeAction)).thenReturn(rangeActionsWithSetpoint.get(unshiftedRangeAction));
        when(postAutoSensitivity.getSetpoint(hvdcRangeActionShifted)).thenReturn(rangeActionsWithSetpoint.get(hvdcRangeActionShifted));
        when(pstRangeActionShifted.convertAngleToTap(rangeActionsWithSetpoint.get(pstRangeActionShifted))).thenReturn(55);
        assertEquals(55, result.getOptimizedTap(pstRangeActionShifted, state1));
        assertEquals(1., result.getOptimizedSetpoint(pstRangeActionShifted, state1), DOUBLE_TOLERANCE);
        assertEquals(2., result.getOptimizedSetpoint(hvdcRangeActionShifted, state1), DOUBLE_TOLERANCE);
        assertEquals(3., result.getOptimizedSetpoint(unshiftedRangeAction, state1), DOUBLE_TOLERANCE);
        assertEquals(rangeActionsWithSetpoint, result.getOptimizedSetpointsOnState(state1));
        assertEquals(Map.of(pstRangeActionShifted, 55), result.getOptimizedTapsOnState(state1));
    }

    @Test
    void testWrongState() {
        State wrongState = mock(State.class);
        assertThrows(OpenRaoException.class, () -> result.getActivatedRangeActions(wrongState));
    }

    @Test
    void testGetSensitivityStatus() {
        when(postAutoSensitivity.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, result.getSensitivityStatus());
    }

    @Test
    void testGetSensitivityOnRangeAction() {
        RangeAction<?> rangeAction = mock(RangeAction.class);
        when(postAutoSensitivity.getSensitivityValue(cnec1, RIGHT, rangeAction, MEGAWATT)).thenReturn(100.);
        when(postAutoSensitivity.getSensitivityValue(cnec1, RIGHT, rangeAction, AMPERE)).thenReturn(1000.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, LEFT, rangeAction, MEGAWATT)).thenReturn(200.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, LEFT, rangeAction, AMPERE)).thenReturn(2000.);
        assertEquals(100., result.getSensitivityValue(cnec1, RIGHT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., result.getSensitivityValue(cnec1, RIGHT, rangeAction, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200., result.getSensitivityValue(cnec2, LEFT, rangeAction, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2000., result.getSensitivityValue(cnec2, LEFT, rangeAction, AMPERE), DOUBLE_TOLERANCE);
    }

    @Test
    void testGetSensitivityOnLinearGlsk() {
        SensitivityVariableSet linearGlsk = mock(SensitivityVariableSet.class);
        when(postAutoSensitivity.getSensitivityValue(cnec1, RIGHT, linearGlsk, MEGAWATT)).thenReturn(100.);
        when(postAutoSensitivity.getSensitivityValue(cnec1, RIGHT, linearGlsk, AMPERE)).thenReturn(1000.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, LEFT, linearGlsk, MEGAWATT)).thenReturn(200.);
        when(postAutoSensitivity.getSensitivityValue(cnec2, LEFT, linearGlsk, AMPERE)).thenReturn(2000.);
        assertEquals(100., result.getSensitivityValue(cnec1, RIGHT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1000., result.getSensitivityValue(cnec1, RIGHT, linearGlsk, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(200., result.getSensitivityValue(cnec2, LEFT, linearGlsk, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(2000., result.getSensitivityValue(cnec2, LEFT, linearGlsk, AMPERE), DOUBLE_TOLERANCE);
    }
}
