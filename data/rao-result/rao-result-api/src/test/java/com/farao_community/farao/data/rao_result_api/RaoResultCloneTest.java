/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.commons.Unit.KILOVOLT;
import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class RaoResultCloneTest {

    @Test
    void testAbstractRaoResultClone() {
        RaoResult importedRaoResult = mock(RaoResult.class);
        Crac crac = Mockito.mock(Crac.class);

        when(importedRaoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(importedRaoResult.getVirtualCostNames()).thenReturn(Set.of("loopFlow", "MNEC"));

        // Mocking costs results
        when(importedRaoResult.getFunctionalCost(null)).thenReturn(100.0);
        when(importedRaoResult.getFunctionalCost(PREVENTIVE)).thenReturn(80.);
        when(importedRaoResult.getFunctionalCost(AUTO)).thenReturn(-20.0);
        when(importedRaoResult.getFunctionalCost(CURATIVE)).thenReturn(-50.0);

        when(importedRaoResult.getVirtualCost(null, "loopFlow")).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(PREVENTIVE, "loopFlow")).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(AUTO, "loopFlow")).thenReturn(15.0);
        when(importedRaoResult.getVirtualCost(CURATIVE, "loopFlow")).thenReturn(10.0);

        when(importedRaoResult.getVirtualCost(null, "MNEC")).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(PREVENTIVE, "MNEC")).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(AUTO, "MNEC")).thenReturn(20.0);
        when(importedRaoResult.getVirtualCost(CURATIVE, "MNEC")).thenReturn(2.0);

        when(importedRaoResult.getVirtualCost(null)).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(PREVENTIVE)).thenReturn(0.0);
        when(importedRaoResult.getVirtualCost(AUTO)).thenReturn(35.0);
        when(importedRaoResult.getVirtualCost(CURATIVE)).thenReturn(12.0);

        when(importedRaoResult.getCost(null)).thenReturn(100.0);
        when(importedRaoResult.getCost(PREVENTIVE)).thenReturn(80.);
        when(importedRaoResult.getCost(AUTO)).thenReturn(15.0);
        when(importedRaoResult.getCost(CURATIVE)).thenReturn(-38.0);

        // Mocking flowCnec results
        FlowCnec cnecP = mock(FlowCnec.class);
        when(crac.getFlowCnec("cnec4prevId")).thenReturn(cnecP);
        Mockito.when(importedRaoResult.getFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(4110.0);

        Mockito.when(importedRaoResult.getFlow(PREVENTIVE, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(PREVENTIVE, cnecP, LEFT, AMPERE)).thenReturn(4220.);
        Mockito.when(importedRaoResult.getFlow(AUTO, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(AUTO, cnecP, LEFT, AMPERE)).thenReturn(4220.);
        Mockito.when(importedRaoResult.getFlow(CURATIVE, cnecP, RIGHT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(CURATIVE, cnecP, LEFT, AMPERE)).thenReturn(4220.);

        Mockito.when(importedRaoResult.getMargin(null, cnecP, MEGAWATT)).thenReturn(4111.);
        Mockito.when(importedRaoResult.getMargin(PREVENTIVE, cnecP, AMPERE)).thenReturn(4221.);

        Mockito.when(importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT)).thenReturn(4112.);
        Mockito.when(importedRaoResult.getRelativeMargin(null, cnecP, AMPERE)).thenReturn(4221.);
        Mockito.when(importedRaoResult.getRelativeMargin(PREVENTIVE, cnecP, AMPERE)).thenReturn(4222.);

        Mockito.when(importedRaoResult.getLoopFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(Double.NaN);

        Mockito.when(importedRaoResult.getCommercialFlow(null, cnecP, RIGHT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)).thenReturn(Double.NaN);

        Mockito.when(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, LEFT)).thenReturn(0.4);
        Mockito.when(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, RIGHT)).thenReturn(Double.NaN);

        FlowCnec cnecO = Mockito.mock(FlowCnec.class);
        Mockito.when(cnecO.getId()).thenReturn("cnec1outageId");
        Mockito.when(crac.getFlowCnec("cnec1outageId")).thenReturn(cnecO);
        Mockito.when(importedRaoResult.getFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1120.5);
        Mockito.when(importedRaoResult.getMargin(null, cnecO, AMPERE)).thenReturn(1121.);
        Mockito.when(importedRaoResult.getRelativeMargin(null, cnecO, AMPERE)).thenReturn(1122.);
        Mockito.when(importedRaoResult.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1123.5);
        Mockito.when(importedRaoResult.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE)).thenReturn(1124.5);
        Mockito.when(importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1210.5);
        Mockito.when(importedRaoResult.getMargin(PREVENTIVE, cnecO, MEGAWATT)).thenReturn(1211.);
        Mockito.when(importedRaoResult.getRelativeMargin(PREVENTIVE, cnecO, MEGAWATT)).thenReturn(1212.);
        Mockito.when(importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1213.5);
        Mockito.when(importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT)).thenReturn(1214.5);
        Mockito.when(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.LEFT)).thenReturn(Double.NaN);
        Mockito.when(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.RIGHT)).thenReturn(0.6);

        // Mocking networkAction results
        State pState = mock(State.class);
        State cState1 = mock(State.class);
        State cState2 = mock(State.class);
        NetworkAction naP = mock(NetworkAction.class);
        NetworkAction naA = mock(NetworkAction.class);
        NetworkAction naC = mock(NetworkAction.class);
        NetworkAction naN = mock(NetworkAction.class);

        when(crac.getPreventiveState()).thenReturn(pState);
        when(crac.getState("contingency1Id", Instant.CURATIVE)).thenReturn(cState1);
        when(crac.getState("contingency2Id", Instant.CURATIVE)).thenReturn(cState2);
        when(crac.getNetworkAction("complexNetworkActionId")).thenReturn(naP);
        when(crac.getNetworkAction("injectionSetpointRaId")).thenReturn(naA);
        when(crac.getNetworkAction("pstSetpointRaId")).thenReturn(naC);
        when(crac.getNetworkAction("switchPairRaId")).thenReturn(naN);

        when(importedRaoResult.isActivatedDuringState(eq(pState), eq(naP))).thenReturn(true);
        when(importedRaoResult.isActivated(eq(pState), eq(naP))).thenReturn(true);
        // Mock other methods for NetworkAction as needed

        // Mocking pstRangeAction results
        PstRangeAction pstP = mock(PstRangeAction.class);
        PstRangeAction pstN = mock(PstRangeAction.class);

        when(crac.getPstRangeAction("pstRange1Id")).thenReturn(pstP);
        when(crac.getPstRangeAction("pstRange2Id")).thenReturn(pstN);

        when(importedRaoResult.isActivatedDuringState(eq(pState), eq(pstP))).thenReturn(true);
        // Mock other methods for PstRangeAction as needed

        // Mocking hvdcRangeAction results
        HvdcRangeAction hvdcC = mock(HvdcRangeAction.class);
        when(crac.getHvdcRangeAction("hvdcRange2Id")).thenReturn(hvdcC);

        when(importedRaoResult.isActivatedDuringState(eq(pState), eq(hvdcC))).thenReturn(false);
        // Mock other methods for HvdcRangeAction as needed

        // Mocking injectionRangeAction results
        InjectionRangeAction injectionC = mock(InjectionRangeAction.class);
        when(crac.getInjectionRangeAction("injectionRange1Id")).thenReturn(injectionC);

        when(importedRaoResult.isActivatedDuringState(eq(pState), eq(injectionC))).thenReturn(false);
        // Mock other methods for InjectionRangeAction as needed

        // Mocking voltageCnec results
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        when(crac.getVoltageCnec("voltageCnecId")).thenReturn(voltageCnec);

        when(importedRaoResult.getVoltage(eq(CURATIVE), eq(voltageCnec), any())).thenReturn(144.38);
        // Mock other methods for VoltageCnec as needed

        // Mocking computation status map
        when(importedRaoResult.getComputationStatus(eq(pState))).thenReturn(ComputationStatus.DEFAULT);
        when(importedRaoResult.getComputationStatus(eq(cState1))).thenReturn(ComputationStatus.FAILURE);
        when(importedRaoResult.getComputationStatus(eq(cState2))).thenReturn(ComputationStatus.DEFAULT);

        when(importedRaoResult.getComputationStatus(crac.getPreventiveState())).thenReturn(ComputationStatus.DEFAULT);
        when(importedRaoResult.getComputationStatus(crac.getState("contingency1Id", CURATIVE))).thenReturn(ComputationStatus.FAILURE);
        when(importedRaoResult.getComputationStatus(crac.getState("contingency2Id", AUTO))).thenReturn(ComputationStatus.DEFAULT);

        testRaoResultClone(importedRaoResult, crac);

    }

    void testRaoResultClone(RaoResult importedRaoResult, Crac crac) {

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(null), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "loopFlow"), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "MNEC"), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(null), 0.001);
        assertEquals(100., importedRaoResult.getCost(null), 0.001);

        assertEquals(80., importedRaoResult.getFunctionalCost(PREVENTIVE), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE, "loopFlow"), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE, "MNEC"), 0.001);
        assertEquals(0., importedRaoResult.getVirtualCost(PREVENTIVE), 0.001);
        assertEquals(80., importedRaoResult.getCost(PREVENTIVE), 0.001);

        assertEquals(-20., importedRaoResult.getFunctionalCost(AUTO), 0.001);
        assertEquals(15., importedRaoResult.getVirtualCost(AUTO, "loopFlow"), 0.001);
        assertEquals(20., importedRaoResult.getVirtualCost(AUTO, "MNEC"), 0.001);
        assertEquals(35., importedRaoResult.getVirtualCost(AUTO), 0.001);
        assertEquals(15., importedRaoResult.getCost(AUTO), 0.001);

        assertEquals(-50., importedRaoResult.getFunctionalCost(CURATIVE), 0.001);
        assertEquals(10., importedRaoResult.getVirtualCost(CURATIVE, "loopFlow"), 0.001);
        assertEquals(2., importedRaoResult.getVirtualCost(CURATIVE, "MNEC"), 0.001);
        assertEquals(12., importedRaoResult.getVirtualCost(CURATIVE), 0.001);
        assertEquals(-38, importedRaoResult.getCost(CURATIVE), 0.001);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(null, cnecP, Side.LEFT, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., importedRaoResult.getMargin(null, cnecP, MEGAWATT), 0.001);
        assertEquals(4112., importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(PREVENTIVE, cnecP, Side.LEFT, AMPERE), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(PREVENTIVE, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(PREVENTIVE, cnecP, AMPERE), 0.001);
        assertEquals(4222., importedRaoResult.getRelativeMargin(PREVENTIVE, cnecP, AMPERE), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, Side.LEFT), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecP, Side.RIGHT)));

        assertEquals(importedRaoResult.getFlow(AUTO, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, LEFT, AMPERE), 0.001);
        assertEquals(importedRaoResult.getFlow(AUTO, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, RIGHT, AMPERE), 0.001);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, LEFT, AMPERE), 0.001);
        assertEquals(importedRaoResult.getFlow(CURATIVE, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(PREVENTIVE, cnecP, RIGHT, AMPERE), 0.001);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, importedRaoResult.getFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);
        assertEquals(1121., importedRaoResult.getMargin(null, cnecO, AMPERE), 0.001);
        assertEquals(1122., importedRaoResult.getRelativeMargin(null, cnecO, AMPERE), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, importedRaoResult.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, importedRaoResult.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE), 0.001);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), 0.001);
        assertEquals(1211., importedRaoResult.getMargin(PREVENTIVE, cnecO, MEGAWATT), 0.001);
        assertEquals(1212., importedRaoResult.getRelativeMargin(PREVENTIVE, cnecO, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), 0.001);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(PREVENTIVE, cnecO, Side.RIGHT, MEGAWATT), 0.001);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(PREVENTIVE, cnecO, Side.RIGHT), 0.001);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", Instant.OUTAGE);
        State aState2 = crac.getState("contingency2Id", Instant.AUTO);
        State cState1 = crac.getState("contingency1Id", Instant.CURATIVE);
        State cState2 = crac.getState("contingency2Id", Instant.CURATIVE);

        /*
        complexNetworkActionId, activated in preventive
        */
        NetworkAction naP = crac.getNetworkAction("complexNetworkActionId");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, naP));
        assertTrue(importedRaoResult.isActivated(pState, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naA));
        assertFalse(importedRaoResult.isActivated(pState, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naA));
        assertFalse(importedRaoResult.isActivated(cState1, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naC));
        assertFalse(importedRaoResult.isActivated(pState, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naC));
        assertFalse(importedRaoResult.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naN));
        assertFalse(importedRaoResult.isActivated(pState, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naN));
        assertFalse(importedRaoResult.isActivated(cState1, naN));
        assertFalse(importedRaoResult.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(importedRaoResult.isActivatedDuringState(pState, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), 0.001);
        assertEquals(0., importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), 0.001);
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), 0.001);
        assertEquals(0., importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(0., importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(0., importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, hvdcC));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), 0.001);
        assertEquals(0., importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), 0.001);
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), 0.001);
        assertEquals(0., importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), 0.001);
        assertEquals(0., importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), 0.001);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, injectionC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, injectionC));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, injectionC), 0.001);
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(cState1, injectionC), 0.001);
        assertEquals(0., importedRaoResult.getOptimizedSetPointOnState(cState1, injectionC), 0.001);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(144.38, importedRaoResult.getVoltage(CURATIVE, voltageCnec, KILOVOLT), 0.001);
        assertEquals(0., importedRaoResult.getMargin(CURATIVE, voltageCnec, KILOVOLT), 0.001);

        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", CURATIVE)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", AUTO)));
    }
}
