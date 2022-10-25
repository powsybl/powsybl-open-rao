/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.PstRangeActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.farao_community.farao.data.rao_result_impl.utils.ExhaustiveRaoResultCreation;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.commons.Unit.KILOVOLT;
import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoResultRoundTripTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;

    @Test
    public void roundTripTest() {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create();

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultImporter().importRaoResult(inputStream, crac);

        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(INITIAL), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(AFTER_PRA), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(AFTER_ARA, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(AFTER_ARA, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(AFTER_ARA), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(AFTER_ARA), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(AFTER_CRA, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(AFTER_CRA, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(AFTER_CRA), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(AFTER_CRA), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA, no result in AFTER_ARA and AFTER_CRA
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(INITIAL, cnecP, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(INITIAL, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., importedRaoResult.getMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(AFTER_PRA, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_PRA, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecP, Side.LEFT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecP, Side.RIGHT)));

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecP, Side.LEFT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecP, Side.RIGHT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecP, Side.LEFT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecP, Side.RIGHT, AMPERE)));

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA, no result in AFTER_ARA and AFTER_CRA
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(importedRaoResult.getFlow(INITIAL, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, importedRaoResult.getFlow(INITIAL, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, importedRaoResult.getLoopFlow(INITIAL, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, importedRaoResult.getCommercialFlow(INITIAL, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_PRA, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(AFTER_PRA, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(AFTER_PRA, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(AFTER_PRA, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_PRA, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(AFTER_PRA, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecO, Side.RIGHT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecO, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecO, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, Side.RIGHT, MEGAWATT)));

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA, but not in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(INITIAL, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecA, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecA, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(AFTER_PRA, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(AFTER_ARA, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, Side.RIGHT, MEGAWATT)));

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA and in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(INITIAL, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecC, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecC, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecC, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(AFTER_PRA, cnecC, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(AFTER_ARA, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(AFTER_CRA, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, importedRaoResult.getFlow(AFTER_CRA, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(AFTER_CRA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

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
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naP));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naP));
        assertTrue(importedRaoResult.isActivated(cState1, naP));
        assertTrue(importedRaoResult.isActivated(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naA));
        assertFalse(importedRaoResult.isActivated(pState, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naA));
        assertTrue(importedRaoResult.isActivatedDuringState(aState2, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, naA));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naA));
        assertFalse(importedRaoResult.isActivated(cState1, naA));
        assertTrue(importedRaoResult.isActivated(cState2, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, naC));
        assertFalse(importedRaoResult.isActivated(pState, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(oState2, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(aState2, naC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, naC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, naC));
        assertTrue(importedRaoResult.isActivated(cState1, naC));
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
        assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(-7, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(-3.2, importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(-3.2, importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(-7, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(-7, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(-7, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, hvdcC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState2, hvdcC));
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(400, importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, injectionC));
        assertTrue(importedRaoResult.isActivatedDuringState(cState1, injectionC));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, injectionC));
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(pState, injectionC), DOUBLE_TOLERANCE);
        assertEquals(100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);
        assertEquals(-300, importedRaoResult.getOptimizedSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);

        /*
        AngleCnec
        */
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(INITIAL, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(INITIAL, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(AFTER_PRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(AFTER_PRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(AFTER_ARA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(AFTER_ARA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(AFTER_CRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(AFTER_CRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(INITIAL, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(INITIAL, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(AFTER_PRA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(AFTER_PRA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(AFTER_ARA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(AFTER_ARA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(AFTER_CRA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(AFTER_CRA, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testRoundTripPostAraResultsForCurativeRas() {
        Crac crac = CracFactory.findDefault().create("crac");
        PstRangeAction pstAuto = crac.newPstRangeAction().withId("pst-auto").withNetworkElement("pst").withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(0, 0., 1, 1.))
                .withSpeed(1)
                .newFreeToUseUsageRule().withInstant(AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .add();
        PstRangeAction pstCur = crac.newPstRangeAction().withId("pst-cur").withNetworkElement("pst").withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(0, 0., 1, 1.))
                .newFreeToUseUsageRule().withInstant(CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        HvdcRangeAction hvdcAuto = crac.newHvdcRangeAction().withId("hvdc-auto").withNetworkElement("hvdc")
                .withSpeed(1)
                .newFreeToUseUsageRule().withInstant(AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .newRange().withMin(0).withMax(10).add()
                .add();
        HvdcRangeAction hvdcCur = crac.newHvdcRangeAction().withId("hvdc-cur").withNetworkElement("hvdc")
                .newFreeToUseUsageRule().withInstant(CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newRange().withMin(0).withMax(10).add()
                .add();

        // dummy flow cnecs
        crac.newContingency().withId("contingency").withNetworkElement("co-ne").add();
        crac.newFlowCnec().withId("dummy-preventive").withInstant(PREVENTIVE).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec().withId("dummy-auto").withContingency("contingency").withInstant(AUTO).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec().withId("dummy-cur").withContingency("contingency").withInstant(CURATIVE).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();

        State autoState = crac.getState("contingency", AUTO);
        State curativeState = crac.getState("contingency", CURATIVE);

        RaoResultImpl raoResult = new RaoResultImpl();
        ((PstRangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pstAuto)).addActivationForState(autoState, 1, 10.);
        ((PstRangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pstCur)).addActivationForState(curativeState, 2, 20.);
        raoResult.getAndCreateIfAbsentRangeActionResult(hvdcAuto).addActivationForState(autoState, 100);
        raoResult.getAndCreateIfAbsentRangeActionResult(hvdcCur).addActivationForState(curativeState, 200);
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultImporter().importRaoResult(inputStream, crac);

        // PST
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(autoState, pstAuto));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(autoState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(autoState, pstCur));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(autoState, pstCur), DOUBLE_TOLERANCE);
        assertEquals(2, importedRaoResult.getOptimizedTapOnState(curativeState, pstCur));
        assertEquals(20., importedRaoResult.getOptimizedSetPointOnState(curativeState, pstCur), DOUBLE_TOLERANCE);

        // HVDC
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(autoState, hvdcAuto), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(autoState, hvdcCur), DOUBLE_TOLERANCE);
        assertEquals(200., importedRaoResult.getOptimizedSetPointOnState(curativeState, hvdcCur), DOUBLE_TOLERANCE);
    }
}
