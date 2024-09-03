/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultjson;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultimpl.RaoResultImpl;
import com.powsybl.openrao.data.raoresultimpl.utils.ExhaustiveRaoResultCreation;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
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
    void explicitJsonRoundTripTest() {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultJsonExporter().exportData(raoResult, crac, Set.of(MEGAWATT, AMPERE), outputStream);

        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        crac.write("JSON", outputStream2);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultJsonImporter().importData(inputStream, crac);
        checkContent(importedRaoResult, crac);
    }

    @Test
    void implicitJsonRoundTripTest() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(MEGAWATT, AMPERE), outputStream);

        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        crac.write("JSON", outputStream2);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = RaoResult.read(inputStream, crac);
        checkContent(importedRaoResult, crac);
    }

    private void checkContent(RaoResult raoResult, Crac crac) {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency1Id", outageInstant)));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency1Id", curativeInstant)));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency2Id", autoInstant)));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency2Id", curativeInstant)));

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), raoResult.getVirtualCostNames());

        assertEquals(100., raoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., raoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(80., raoResult.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(preventiveInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(preventiveInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(80., raoResult.getCost(preventiveInstant), DOUBLE_TOLERANCE);

        assertEquals(-20., raoResult.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(15., raoResult.getVirtualCost(autoInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., raoResult.getVirtualCost(autoInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., raoResult.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(15., raoResult.getCost(autoInstant), DOUBLE_TOLERANCE);

        assertEquals(-50., raoResult.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(10., raoResult.getVirtualCost(curativeInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., raoResult.getVirtualCost(curativeInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., raoResult.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(-38, raoResult.getCost(curativeInstant), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., raoResult.getFlow(null, cnecP, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, TwoSides.TWO, MEGAWATT)));
        assertEquals(4111., raoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., raoResult.getRelativeMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecP, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecP, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecP, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecP, TwoSides.TWO, MEGAWATT)));

        assertEquals(4220., raoResult.getFlow(preventiveInstant, cnecP, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(preventiveInstant, cnecP, TwoSides.TWO, AMPERE)));
        assertEquals(4221., raoResult.getMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., raoResult.getRelativeMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecP, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecP, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecP, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecP, TwoSides.TWO, MEGAWATT)));

        assertEquals(0.4, raoResult.getPtdfZonalSum(preventiveInstant, cnecP, TwoSides.ONE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(preventiveInstant, cnecP, TwoSides.TWO)));

        assertEquals(raoResult.getFlow(autoInstant, cnecP, ONE, AMPERE), raoResult.getFlow(preventiveInstant, cnecP, ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(raoResult.getFlow(autoInstant, cnecP, TWO, AMPERE), raoResult.getFlow(preventiveInstant, cnecP, TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(raoResult.getFlow(curativeInstant, cnecP, ONE, AMPERE), raoResult.getFlow(preventiveInstant, cnecP, ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(raoResult.getFlow(curativeInstant, cnecP, TWO, AMPERE), raoResult.getFlow(preventiveInstant, cnecP, TWO, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecO, TwoSides.ONE, AMPERE)));
        assertEquals(1120.5, raoResult.getFlow(null, cnecO, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., raoResult.getMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., raoResult.getRelativeMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecO, TwoSides.ONE, AMPERE)));
        assertEquals(1123.5, raoResult.getLoopFlow(null, cnecO, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecO, TwoSides.ONE, AMPERE)));
        assertEquals(1124.5, raoResult.getCommercialFlow(null, cnecO, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(raoResult.getFlow(preventiveInstant, cnecO, TwoSides.ONE, MEGAWATT)));
        assertEquals(1210.5, raoResult.getFlow(preventiveInstant, cnecO, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., raoResult.getMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., raoResult.getRelativeMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getLoopFlow(preventiveInstant, cnecO, TwoSides.ONE, MEGAWATT)));
        assertEquals(1213.5, raoResult.getLoopFlow(preventiveInstant, cnecO, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(preventiveInstant, cnecO, TwoSides.ONE, MEGAWATT)));
        assertEquals(1214.5, raoResult.getCommercialFlow(preventiveInstant, cnecO, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(preventiveInstant, cnecO, TwoSides.ONE)));
        assertEquals(0.6, raoResult.getPtdfZonalSum(preventiveInstant, cnecO, TwoSides.TWO), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(raoResult.getFlow(autoInstant, cnecO, TwoSides.ONE, MEGAWATT)));
        assertEquals(raoResult.getFlow(autoInstant, cnecO, TWO, MEGAWATT), raoResult.getFlow(preventiveInstant, cnecO, TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(curativeInstant, cnecO, TwoSides.ONE, MEGAWATT)));
        assertEquals(raoResult.getFlow(curativeInstant, cnecO, TWO, MEGAWATT), raoResult.getFlow(preventiveInstant, cnecO, TWO, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO. Results in CURATIVE are the same as AUTO because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., raoResult.getFlow(null, cnecA, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, raoResult.getFlow(null, cnecA, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., raoResult.getMargin(null, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getRelativeMargin(null, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecA, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecA, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecA, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecA, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(null, cnecA, TwoSides.ONE)));
        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(null, cnecA, TwoSides.TWO)));

        assertEquals(3220., raoResult.getFlow(preventiveInstant, cnecA, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, raoResult.getFlow(preventiveInstant, cnecA, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., raoResult.getMargin(preventiveInstant, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., raoResult.getFlow(autoInstant, cnecA, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, raoResult.getFlow(autoInstant, cnecA, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., raoResult.getMargin(autoInstant, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(raoResult.getFlow(curativeInstant, cnecA, ONE, MEGAWATT), raoResult.getFlow(autoInstant, cnecA, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(raoResult.getFlow(curativeInstant, cnecA, TWO, MEGAWATT), raoResult.getFlow(autoInstant, cnecA, TWO, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO and in CURATIVE
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., raoResult.getFlow(null, cnecC, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3110.5, raoResult.getFlow(null, cnecC, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., raoResult.getMargin(null, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getRelativeMargin(null, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecC, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getLoopFlow(null, cnecC, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecC, TwoSides.ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(null, cnecC, TwoSides.TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(null, cnecC, TwoSides.ONE)));
        assertTrue(Double.isNaN(raoResult.getPtdfZonalSum(null, cnecC, TwoSides.TWO)));

        assertEquals(3220., raoResult.getFlow(preventiveInstant, cnecC, TwoSides.ONE, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, raoResult.getFlow(preventiveInstant, cnecC, TwoSides.TWO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., raoResult.getMargin(preventiveInstant, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., raoResult.getFlow(autoInstant, cnecC, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, raoResult.getFlow(autoInstant, cnecC, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., raoResult.getMargin(autoInstant, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., raoResult.getFlow(curativeInstant, cnecC, TwoSides.ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, raoResult.getFlow(curativeInstant, cnecC, TwoSides.TWO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., raoResult.getMargin(curativeInstant, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

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
        assertTrue(raoResult.isActivatedDuringState(pState, naP));
        assertTrue(raoResult.isActivated(pState, naP));
        assertFalse(raoResult.isActivatedDuringState(oState2, naP));
        assertFalse(raoResult.isActivatedDuringState(aState2, naP));
        assertFalse(raoResult.isActivatedDuringState(cState1, naP));
        assertFalse(raoResult.isActivatedDuringState(cState2, naP));
        assertTrue(raoResult.isActivated(cState1, naP));
        assertTrue(raoResult.isActivated(cState2, naP));

        /*
        injectionSetpointRaId, activated in auto
        */
        NetworkAction naA = crac.getNetworkAction("injectionSetpointRaId");
        assertFalse(raoResult.isActivatedDuringState(pState, naA));
        assertFalse(raoResult.isActivated(pState, naA));
        assertFalse(raoResult.isActivatedDuringState(oState2, naA));
        assertTrue(raoResult.isActivatedDuringState(aState2, naA));
        assertFalse(raoResult.isActivatedDuringState(cState1, naA));
        assertFalse(raoResult.isActivatedDuringState(cState2, naA));
        assertFalse(raoResult.isActivated(cState1, naA));
        assertTrue(raoResult.isActivated(cState2, naA));

        /*
        pstSetpointRaId, activated curative1
        */
        NetworkAction naC = crac.getNetworkAction("pstSetpointRaId");
        assertFalse(raoResult.isActivatedDuringState(pState, naC));
        assertFalse(raoResult.isActivated(pState, naC));
        assertFalse(raoResult.isActivatedDuringState(oState2, naC));
        assertFalse(raoResult.isActivatedDuringState(aState2, naC));
        assertTrue(raoResult.isActivatedDuringState(cState1, naC));
        assertFalse(raoResult.isActivatedDuringState(cState2, naC));
        assertTrue(raoResult.isActivated(cState1, naC));
        assertFalse(raoResult.isActivated(cState2, naC));

        /*
        switchPairRaId, never activated
        */
        NetworkAction naN = crac.getNetworkAction("switchPairRaId");
        assertFalse(raoResult.isActivatedDuringState(pState, naN));
        assertFalse(raoResult.isActivated(pState, naN));
        assertFalse(raoResult.isActivatedDuringState(oState2, naN));
        assertFalse(raoResult.isActivatedDuringState(aState2, naN));
        assertFalse(raoResult.isActivatedDuringState(cState1, naN));
        assertFalse(raoResult.isActivatedDuringState(cState2, naN));
        assertFalse(raoResult.isActivated(cState1, naN));
        assertFalse(raoResult.isActivated(cState2, naN));

        // ------------------------------
        // --- PstRangeAction results ---
        // ------------------------------

        /*
        pstRange1Id, activated in preventive
        */
        PstRangeAction pstP = crac.getPstRangeAction("pstRange1Id");
        assertTrue(raoResult.isActivatedDuringState(pState, pstP));
        assertFalse(raoResult.isActivatedDuringState(cState1, pstP));
        assertFalse(raoResult.isActivatedDuringState(cState2, pstP));
        assertEquals(-3, raoResult.getPreOptimizationTapOnState(pState, pstP));
        assertEquals(0., raoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, raoResult.getOptimizedTapOnState(pState, pstP));
        assertEquals(3., raoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        assertEquals(3., raoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        assertEquals(3, raoResult.getPreOptimizationTapOnState(cState1, pstP));
        assertEquals(3, raoResult.getOptimizedTapOnState(cState1, pstP));
        assertEquals(3, raoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(raoResult.isActivatedDuringState(pState, pstN));
        assertFalse(raoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(raoResult.isActivatedDuringState(cState2, pstN));
        assertEquals(0, raoResult.getPreOptimizationTapOnState(pState, pstN));
        assertEquals(0, raoResult.getOptimizedTapOnState(pState, pstN));
        assertEquals(0, raoResult.getOptimizedTapOnState(cState1, pstN));
        assertEquals(0, raoResult.getOptimizedTapOnState(cState2, pstN));

        // ---------------------------
        // --- RangeAction results ---
        // ---------------------------

        /*
        hvdcRange2Id, two different activations in the two curative states
        */
        HvdcRangeAction hvdcC = crac.getHvdcRangeAction("hvdcRange2Id");
        assertFalse(raoResult.isActivatedDuringState(pState, hvdcC));
        assertTrue(raoResult.isActivatedDuringState(cState1, hvdcC));
        assertTrue(raoResult.isActivatedDuringState(cState2, hvdcC));
        assertEquals(-100, raoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, raoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(-100, raoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(100, raoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        assertEquals(400, raoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);

        /*
        InjectionRange1Id, one activation in curative
        */
        InjectionRangeAction injectionC = crac.getInjectionRangeAction("injectionRange1Id");
        assertFalse(raoResult.isActivatedDuringState(pState, injectionC));
        assertTrue(raoResult.isActivatedDuringState(cState1, injectionC));
        assertFalse(raoResult.isActivatedDuringState(cState2, injectionC));
        assertEquals(100, raoResult.getPreOptimizationSetPointOnState(pState, injectionC), DOUBLE_TOLERANCE);
        assertEquals(100, raoResult.getPreOptimizationSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);
        assertEquals(-300, raoResult.getOptimizedSetPointOnState(cState1, injectionC), DOUBLE_TOLERANCE);

        /*
        AngleCnec
        */
        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., raoResult.getAngle(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., raoResult.getMargin(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., raoResult.getAngle(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., raoResult.getMargin(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., raoResult.getAngle(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., raoResult.getMargin(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., raoResult.getAngle(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., raoResult.getMargin(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., raoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., raoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., raoResult.getVoltage(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., raoResult.getMargin(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., raoResult.getVoltage(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., raoResult.getMargin(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., raoResult.getVoltage(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., raoResult.getMargin(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void testExplicitRoundTripRangeActionsCrossResults() {
        Crac crac = initCrac();
        RaoResult raoResult = initRaoResult(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultJsonExporter().exportData(raoResult, crac, Set.of(MEGAWATT, AMPERE), outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = new RaoResultJsonImporter().importData(inputStream, crac);

        checkContentRangeActionCrossResult(importedRaoResult, crac);
    }

    @Test
    void testImplicitRoundTripRangeActionsCrossResults() throws IOException {
        Crac crac = initCrac();
        RaoResult raoResult = initRaoResult(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(MEGAWATT, AMPERE), outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult importedRaoResult = RaoResult.read(inputStream, crac);

        checkContentRangeActionCrossResult(importedRaoResult, crac);
    }

    private Crac initCrac() {
        Crac crac = CracFactory.findDefault().create("crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        crac.newPstRangeAction().withId("pst-prev").withNetworkElement("pst").withInitialTap(-1)
            .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
            .withSpeed(1)
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();
        crac.newPstRangeAction().withId("pst-auto").withNetworkElement("pst").withInitialTap(-1)
            .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
            .withSpeed(1)
            .newOnInstantUsageRule().withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .add();
        crac.newPstRangeAction().withId("pst-cur").withNetworkElement("pst").withInitialTap(-1)
            .withTapToAngleConversionMap(Map.of(-1, -10., 0, 0., 1, 10., 2, 20., 3, 30.))
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        // dummy flow cnecs
        crac.newContingency().withId("contingency").withContingencyElement("co-ne", ContingencyElementType.LINE).add();
        crac.newFlowCnec().withId("dummy-preventive").withInstant(PREVENTIVE_INSTANT_ID).withNetworkElement("ne")
            .newThreshold().withMax(1.).withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).add()
            .add();
        crac.newFlowCnec().withId("dummy-outage").withContingency("contingency").withInstant(OUTAGE_INSTANT_ID).withNetworkElement("ne")
            .newThreshold().withMax(1.).withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).add()
            .add();
        crac.newFlowCnec().withId("dummy-auto").withContingency("contingency").withInstant(AUTO_INSTANT_ID).withNetworkElement("ne")
            .newThreshold().withMax(1.).withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).add()
            .add();
        crac.newFlowCnec().withId("dummy-cur").withContingency("contingency").withInstant(CURATIVE_INSTANT_ID).withNetworkElement("ne")
            .newThreshold().withMax(1.).withSide(TwoSides.ONE).withUnit(Unit.MEGAWATT).add()
            .add();

        return crac;
    }

    private RaoResult initRaoResult(Crac crac) {
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        PstRangeAction pstPrev = crac.getPstRangeAction("pst-prev");
        PstRangeAction pstAuto = crac.getPstRangeAction("pst-auto");
        PstRangeAction pstCur = crac.getPstRangeAction("pst-cur");

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

        return raoResult;
    }

    private void checkContentRangeActionCrossResult(RaoResult importedRaoResult, Crac crac) {
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        PstRangeAction pstPrev = crac.getPstRangeAction("pst-prev");
        PstRangeAction pstAuto = crac.getPstRangeAction("pst-auto");
        PstRangeAction pstCur = crac.getPstRangeAction("pst-cur");

        State outageState = crac.getState("contingency", outageInstant);
        State autoState = crac.getState("contingency", autoInstant);
        State curativeState = crac.getState("contingency", curativeInstant);

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
        RaoResultJsonExporter raoResultExporter = new RaoResultJsonExporter();

        // Empty set
        Set<Unit> emptySet = Collections.emptySet();
        Exception exception = assertThrows(OpenRaoException.class, () -> raoResultExporter.exportData(raoResult, crac, emptySet, outputStream));
        assertEquals("At least one flow unit should be defined", exception.getMessage());

        // "TAP" unit
        Set<Unit> tapSingleton = Set.of(TAP);
        exception = assertThrows(OpenRaoException.class, () -> raoResultExporter.exportData(raoResult, crac, tapSingleton, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());

        // "DEGREE" unit
        Set<Unit> degreeSingleton = Set.of(DEGREE);
        exception = assertThrows(OpenRaoException.class, () -> raoResultExporter.exportData(raoResult, crac, degreeSingleton, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());

        // "KILOVOLT" + "AMPERE" units
        Set<Unit> kvAndAmp = Set.of(KILOVOLT, AMPERE);
        exception = assertThrows(OpenRaoException.class, () -> raoResultExporter.exportData(raoResult, crac, kvAndAmp, outputStream));
        assertEquals("Flow unit should be AMPERE and/or MEGAWATT", exception.getMessage());
    }

    @Test
    void testExplicitRoundTripWithUnits() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // RoundTrip with Ampere only
        ByteArrayOutputStream outputStreamAmpere = new ByteArrayOutputStream();
        new RaoResultJsonExporter().exportData(raoResult, crac, Set.of(AMPERE), outputStreamAmpere);
        ByteArrayInputStream inputStreamAmpere = new ByteArrayInputStream(outputStreamAmpere.toByteArray());
        RaoResult importedRaoResultAmpere = new RaoResultJsonImporter().importData(inputStreamAmpere, crac);

        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        checkContentAmpere(importedRaoResultAmpere, cnecP);

        // RoundTrip with MW only
        ByteArrayOutputStream outputStreamMegawatt = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(MEGAWATT), outputStreamMegawatt);
        ByteArrayInputStream inputStreamMegawatt = new ByteArrayInputStream(outputStreamMegawatt.toByteArray());
        RaoResult importedRaoResultMegawatt = RaoResult.read(inputStreamMegawatt, crac);

        checkContentMegawatt(importedRaoResultMegawatt, cnecP);
    }

    @Test
    void testImplicitRoundTripWithUnits() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // RoundTrip with Ampere only
        ByteArrayOutputStream outputStreamAmpere = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(AMPERE), outputStreamAmpere);
        ByteArrayInputStream inputStreamAmpere = new ByteArrayInputStream(outputStreamAmpere.toByteArray());
        RaoResult importedRaoResultAmpere = RaoResult.read(inputStreamAmpere, crac);

        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        checkContentAmpere(importedRaoResultAmpere, cnecP);

        // RoundTrip with MW only
        ByteArrayOutputStream outputStreamMegawatt = new ByteArrayOutputStream();
        raoResult.write("JSON", crac, Set.of(MEGAWATT), outputStreamMegawatt);
        ByteArrayInputStream inputStreamMegawatt = new ByteArrayInputStream(outputStreamMegawatt.toByteArray());
        RaoResult importedRaoResultMegawatt = RaoResult.read(inputStreamMegawatt, crac);

        checkContentMegawatt(importedRaoResultMegawatt, cnecP);
    }

    private void checkContentAmpere(RaoResult raoResult, FlowCnec cnecP) {
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, ONE, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, TWO, MEGAWATT)));
        assertTrue(Double.isNaN(raoResult.getMargin(null, cnecP, MEGAWATT)));
        assertEquals(4120, raoResult.getFlow(null, cnecP, ONE, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, TWO, AMPERE)));
        assertEquals(4121, raoResult.getMargin(null, cnecP, AMPERE), DOUBLE_TOLERANCE);
    }

    private void checkContentMegawatt(RaoResult raoResult, FlowCnec cnecP) {
        assertEquals(4110, raoResult.getFlow(null, cnecP, ONE, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, TWO, MEGAWATT)));
        assertEquals(4111, raoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, ONE, AMPERE)));
        assertTrue(Double.isNaN(raoResult.getFlow(null, cnecP, TWO, AMPERE)));
        assertTrue(Double.isNaN(raoResult.getMargin(null, cnecP, AMPERE)));
    }
}
