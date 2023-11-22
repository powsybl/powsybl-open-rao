/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
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
import com.farao_community.farao.data.crac_io_json.JsonExport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.farao_community.farao.data.rao_result_impl.utils.ExhaustiveRaoResultCreation;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultRoundTripTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void roundTripTest() {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, Set.of(MEGAWATT, AMPERE), outputStream);

        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        new JsonExport().exportCrac(crac, outputStream2);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultImporter().importRaoResult(inputStream, crac);

        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", outageInstant)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", curativeInstant)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", autoInstant)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", curativeInstant)));

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(preventiveInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(preventiveInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(preventiveInstant), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(autoInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(autoInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(autoInstant), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(curativeInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(curativeInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(curativeInstant), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in preventiveInstant. Results in autoInstant and curativeInstant are the same as preventiveInstant because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(null, cnecP, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111., importedRaoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(preventiveInstant, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(preventiveInstant, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecP, Side.LEFT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecP, Side.RIGHT)));

        assertEquals(importedRaoResult.getFlow(autoInstant, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(autoInstant, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in preventiveInstant. Results in autoInstant and curativeInstant are the same as preventiveInstant because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(importedRaoResult.getFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1120.5, importedRaoResult.getFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1123.5, importedRaoResult.getLoopFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecO, Side.LEFT, AMPERE)));
        assertEquals(1124.5, importedRaoResult.getCommercialFlow(null, cnecO, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(preventiveInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(preventiveInstant, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecO, Side.RIGHT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(autoInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(importedRaoResult.getFlow(autoInstant, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(curativeInstant, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in null, preventiveInstant, and autoInstant. Results in curativeInstant are the same as autoInstant because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(null, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecA, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecA, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecA, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecA, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(preventiveInstant, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(preventiveInstant, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(preventiveInstant, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(autoInstant, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(autoInstant, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(autoInstant, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecA, LEFT, MEGAWATT), importedRaoResult.getFlow(autoInstant, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecA, RIGHT, MEGAWATT), importedRaoResult.getFlow(autoInstant, cnecA, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in null, preventiveInstant, and autoInstant and in curativeInstant
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, importedRaoResult.getFlow(null, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecC, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecC, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecC, Side.LEFT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecC, Side.RIGHT)));

        assertEquals(3220., importedRaoResult.getFlow(preventiveInstant, cnecC, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(preventiveInstant, cnecC, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(preventiveInstant, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(autoInstant, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(autoInstant, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(autoInstant, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(curativeInstant, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, importedRaoResult.getFlow(curativeInstant, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(curativeInstant, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", outageInstant);
        State aState2 = crac.getState("contingency2Id", autoInstant);
        State cState1 = crac.getState("contingency1Id", curativeInstant);
        State cState2 = crac.getState("contingency2Id", curativeInstant);

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
        assertEquals(-3, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(3., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

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
        assertEquals(3135., importedRaoResult.getAngle(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(PREVENTIVE_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(PREVENTIVE_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(AUTO_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(AUTO_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(CURATIVE_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(CURATIVE_INSTANT_ID, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(PREVENTIVE_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(PREVENTIVE_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(AUTO_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(AUTO_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(CURATIVE_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(CURATIVE_INSTANT_ID, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void testRoundTripRangeActionsCrossResults() {
        Crac crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        PstRangeAction pstPrev = (PstRangeAction) crac.newPstRangeAction().withId("pst-prev").withNetworkElement("pst").withInitialTap(-1)
                .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
                .withSpeed(1)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        PstRangeAction pstAuto = (PstRangeAction) crac.newPstRangeAction().withId("pst-auto").withNetworkElement("pst").withInitialTap(-1)
                .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
                .withSpeed(1)
                .newOnInstantUsageRule().withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
                .add();
        PstRangeAction pstCur = (PstRangeAction) crac.newPstRangeAction().withId("pst-cur").withNetworkElement("pst").withInitialTap(-1)
                .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
                .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        // dummy flow cnecs
        crac.newContingency().withId("contingency").withNetworkElement("co-ne").add();
        crac.newFlowCnec().withId("dummy-preventive").withInstant(PREVENTIVE_INSTANT_ID).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec().withId("dummy-outage").withContingency("contingency").withInstant(OUTAGE_INSTANT_ID).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec().withId("dummy-auto").withContingency("contingency").withInstant(AUTO_INSTANT_ID).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();
        crac.newFlowCnec().withId("dummy-cur").withContingency("contingency").withInstant(CURATIVE_INSTANT_ID).withNetworkElement("ne")
                .newThreshold().withMax(1.).withSide(Side.LEFT).withUnit(Unit.MEGAWATT).add()
                .add();

        State outageState = crac.getState("contingency", outageInstant);
        State autoState = crac.getState("contingency", autoInstant);
        State curativeState = crac.getState("contingency", curativeInstant);

        RaoResultImpl raoResult = new RaoResultImpl(crac);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstPrev).setInitialSetpoint(-10.);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstAuto).setInitialSetpoint(-10.);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstCur).setInitialSetpoint(-10.);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstPrev).addActivationForState(crac.getPreventiveState(), 10.);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstAuto).addActivationForState(autoState, 20.);
        raoResult.getAndCreateIfAbsentRangeActionResult(pstCur).addActivationForState(curativeState, 30.);
        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, Set.of(MEGAWATT, AMPERE), outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultImporter().importRaoResult(inputStream, crac);

        // Before & after Preventive state
        assertEquals(-1, importedRaoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pstPrev));
        assertEquals(-10., importedRaoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pstPrev), DOUBLE_TOLERANCE);
        assertEquals(-1, importedRaoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pstAuto));
        assertEquals(-10., importedRaoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pstAuto), DOUBLE_TOLERANCE);
        assertEquals(-1, importedRaoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pstCur));
        assertEquals(-10., importedRaoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pstCur), DOUBLE_TOLERANCE);

        assertEquals(1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstPrev));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pstPrev), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstAuto));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pstAuto), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), pstCur));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pstCur), DOUBLE_TOLERANCE);

        // Before & after outage state
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(outageState, pstPrev));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(outageState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(outageState, pstAuto));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(outageState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(outageState, pstCur));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(outageState, pstCur), DOUBLE_TOLERANCE);

        assertEquals(1, importedRaoResult.getOptimizedTapOnState(outageState, pstPrev));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(outageState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(outageState, pstAuto));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(outageState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getOptimizedTapOnState(outageState, pstCur));
        assertEquals(10., importedRaoResult.getOptimizedSetPointOnState(outageState, pstCur), DOUBLE_TOLERANCE);

        // Before & After ARA
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(autoState, pstPrev));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(autoState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(autoState, pstAuto));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(autoState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(1, importedRaoResult.getPreOptimizationTapOnState(autoState, pstCur));
        assertEquals(10., importedRaoResult.getPreOptimizationSetPointOnState(autoState, pstCur), DOUBLE_TOLERANCE);

        assertEquals(2, importedRaoResult.getOptimizedTapOnState(autoState, pstPrev));
        assertEquals(20., importedRaoResult.getOptimizedSetPointOnState(autoState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(2, importedRaoResult.getOptimizedTapOnState(autoState, pstAuto));
        assertEquals(20., importedRaoResult.getOptimizedSetPointOnState(autoState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(2, importedRaoResult.getOptimizedTapOnState(autoState, pstCur));
        assertEquals(20., importedRaoResult.getOptimizedSetPointOnState(autoState, pstCur), DOUBLE_TOLERANCE);

        // Before & After CRA
        assertEquals(2, importedRaoResult.getPreOptimizationTapOnState(curativeState, pstPrev));
        assertEquals(20., importedRaoResult.getPreOptimizationSetPointOnState(curativeState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(2, importedRaoResult.getPreOptimizationTapOnState(curativeState, pstAuto));
        assertEquals(20., importedRaoResult.getPreOptimizationSetPointOnState(curativeState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(2, importedRaoResult.getPreOptimizationTapOnState(curativeState, pstCur));
        assertEquals(20., importedRaoResult.getPreOptimizationSetPointOnState(curativeState, pstCur), DOUBLE_TOLERANCE);

        assertEquals(3, importedRaoResult.getOptimizedTapOnState(curativeState, pstPrev));
        assertEquals(30., importedRaoResult.getOptimizedSetPointOnState(curativeState, pstPrev), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(curativeState, pstAuto));
        assertEquals(30., importedRaoResult.getOptimizedSetPointOnState(curativeState, pstAuto), DOUBLE_TOLERANCE);
        assertEquals(3, importedRaoResult.getOptimizedTapOnState(curativeState, pstCur));
        assertEquals(30., importedRaoResult.getOptimizedSetPointOnState(curativeState, pstCur), DOUBLE_TOLERANCE);
    }

    @Test
    void testFailWithWrongFlowUnits() {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        RaoResultExporter raoResultExporter = new RaoResultExporter();

        // Empty set
        Set<Unit> emptyUnitSet = Collections.emptySet();
        Exception exception = assertThrows(FaraoException.class, () -> raoResultExporter.export(raoResult, crac, emptyUnitSet, outputStream));
        assertEquals("At least one flow unit should be defined", exception.getMessage());

        // "TAP" unit
        Set<Unit> tapUnit = Set.of(TAP);
        exception = assertThrows(FaraoException.class, () -> raoResultExporter.export(raoResult, crac, tapUnit, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());

        // "DEGREE" unit
        Set<Unit> degreeUnit = Set.of(DEGREE);
        exception = assertThrows(FaraoException.class, () -> raoResultExporter.export(raoResult, crac, degreeUnit, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());

        // "KILOVOLT" + "AMPERE" units
        Set<Unit> powerUnits = Set.of(KILOVOLT, AMPERE);
        exception = assertThrows(FaraoException.class, () -> raoResultExporter.export(raoResult, crac, powerUnits, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());
    }

    @Test
    void testRoundTripWithUnits() {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // RoundTrip with Ampere only
        ByteArrayOutputStream outputStreamAmpere = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, Set.of(AMPERE), outputStreamAmpere);
        ByteArrayInputStream inputStreamAmpere = new ByteArrayInputStream(outputStreamAmpere.toByteArray());
        RaoResult importedRaoResultAmpere = new RaoResultImporter().importRaoResult(inputStreamAmpere, crac);

        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertTrue(Double.isNaN(importedRaoResultAmpere.getFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResultAmpere.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResultAmpere.getMargin(null, cnecP, MEGAWATT)));
        assertEquals(4120, importedRaoResultAmpere.getFlow(null, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResultAmpere.getFlow(null, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4121, importedRaoResultAmpere.getMargin(null, cnecP, AMPERE), DOUBLE_TOLERANCE);

        // RoundTrip with MW only
        ByteArrayOutputStream outputStreamMegawatt = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, Set.of(MEGAWATT), outputStreamMegawatt);
        ByteArrayInputStream inputStreamMegawatt = new ByteArrayInputStream(outputStreamMegawatt.toByteArray());
        RaoResult importedRaoResultMegawatt = new RaoResultImporter().importRaoResult(inputStreamMegawatt, crac);

        assertEquals(4110, importedRaoResultMegawatt.getFlow(null, cnecP, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResultMegawatt.getFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertEquals(4111, importedRaoResultMegawatt.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResultMegawatt.getFlow(null, cnecP, Side.LEFT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResultMegawatt.getFlow(null, cnecP, Side.RIGHT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResultMegawatt.getMargin(null, cnecP, AMPERE)));
    }
}
