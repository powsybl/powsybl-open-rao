/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.DEGREE;
import static com.farao_community.farao.commons.Unit.KILOVOLT;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.crac_api.cnec.Side.LEFT;
import static com.farao_community.farao.data.crac_api.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ImporterRetrocompatibilityTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);

    /*
    The goal of this test class is to ensure that former JSON RaoResult files are
    still importable, even when modifications are brought to the JSON importer.
     */

    /*
    CARE: the existing json file used in this test case SHOULD NOT BE MODIFIED. If
    the current tests do not pass, it means that formerly generated JSON RaoResult
    will not be compatible anymore with the next version of farao-core -> This is NOT
    desirable.

    Instead, we need to ensure that the JSON RaoResult files used in this class can
    still be imported as is. Using versioning of the importer if needed.
     */

    @Test
    void importV1Point0Test() {

        // JSON file of farao-core v3.4.3
        /*
         versioning was not yet in place, and version number does not explicitly appear
         in v1.0 files
         */
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/rao-result-v1.0.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/crac-for-rao-result-v1.0.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point1Test() {

        // JSON file of farao-core v3.5.0
        /*
         addition of versioning, no changes apart from the fact that version numbers
         are now added in the first lines of the json
         */

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.1.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
        testExtraContentOfV1Point1RaoResult(raoResult, crac);
    }

    @Test
    void importAfterV1Point1FieldDeprecationTest() {

        // unused field should throw an exception

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        FaraoException exception = assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("", exception.getMessage());
    }

    @Test
    void importV1Point2Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point2FieldDeprecationTest() {
        // RaoResult copied from v1.1 but version set to v1.2
        // Should not be imported because CNEC side is not defined properly
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        FaraoException exception = assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("", exception.getMessage());
    }

    @Test
    void importV1Point3Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point3RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point4Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.4/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.4/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point3RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point3TestFieldDeprecationTest() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");
        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error1.json");
        FaraoException exception = assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("", exception.getMessage());

        InputStream raoResultFile2 = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error2.json");
        exception = assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile2, crac));
        assertEquals("", exception.getMessage());
    }

    private void testBaseContentOfV1RaoResult(RaoResult importedRaoResult, Crac crac) {

        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(INSTANT_PREV), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(INSTANT_AUTO, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(INSTANT_AUTO, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(INSTANT_AUTO), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(INSTANT_CURATIVE, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(INSTANT_CURATIVE, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(null, cnecP, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4111., importedRaoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(INSTANT_PREV, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4221., importedRaoResult.getMargin(INSTANT_PREV, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(INSTANT_PREV, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecP, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        assertEquals(1120., importedRaoResult.getFlow(null, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1121., importedRaoResult.getMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1122., importedRaoResult.getRelativeMargin(null, cnecO, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1123., importedRaoResult.getLoopFlow(null, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(1124., importedRaoResult.getCommercialFlow(null, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(1210., importedRaoResult.getFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(INSTANT_PREV, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(INSTANT_PREV, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1213., importedRaoResult.getLoopFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1214., importedRaoResult.getCommercialFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecO, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO. Results in CURATIVE are the same as AUTO because the CNEC is auto
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecA, LEFT)));

        assertEquals(3220., importedRaoResult.getFlow(INSTANT_PREV, cnecA, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(INSTANT_PREV, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(INSTANT_AUTO, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(INSTANT_AUTO, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_AUTO, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO and in CURATIVE
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        assertEquals(3110., importedRaoResult.getFlow(null, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3111., importedRaoResult.getMargin(null, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(null, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(null, cnecC, LEFT)));

        assertEquals(3220., importedRaoResult.getFlow(INSTANT_PREV, cnecC, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(INSTANT_PREV, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(INSTANT_AUTO, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(INSTANT_AUTO, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(INSTANT_CURATIVE, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(INSTANT_CURATIVE, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", "outage");
        State aState2 = crac.getState("contingency2Id", "auto");
        State cState1 = crac.getState("contingency1Id", "curative");
        State cState2 = crac.getState("contingency2Id", "curative");

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
    }

    private void testExtraContentOfV1Point1RaoResult(RaoResult importedRaoResult, Crac crac) {
        assertEquals(-1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pstRange3Id")));

        InjectionRangeAction rangeAction = crac.getInjectionRangeAction("injectionRange1Id");
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction), DOUBLE_TOLERANCE);
        assertEquals(-300., importedRaoResult.getOptimizedSetPointOnState(crac.getState("contingency1Id", "curative"), rangeAction), DOUBLE_TOLERANCE);

        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(INSTANT_PREV, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(INSTANT_PREV, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(INSTANT_AUTO, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(INSTANT_AUTO, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(INSTANT_CURATIVE, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(INSTANT_CURATIVE, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(INSTANT_PREV, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(INSTANT_PREV, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(INSTANT_AUTO, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(INSTANT_AUTO, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(INSTANT_CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(INSTANT_CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    private void testBaseContentOfV1Point2RaoResult(RaoResult importedRaoResult, Crac crac) {
        // --------------------------
        // --- Computation status ---
        // --------------------------
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(80., importedRaoResult.getFunctionalCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INSTANT_PREV), DOUBLE_TOLERANCE);
        assertEquals(80., importedRaoResult.getCost(INSTANT_PREV), DOUBLE_TOLERANCE);

        assertEquals(-20., importedRaoResult.getFunctionalCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getVirtualCost(INSTANT_AUTO, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(20., importedRaoResult.getVirtualCost(INSTANT_AUTO, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(35., importedRaoResult.getVirtualCost(INSTANT_AUTO), DOUBLE_TOLERANCE);
        assertEquals(15., importedRaoResult.getCost(INSTANT_AUTO), DOUBLE_TOLERANCE);

        assertEquals(-50., importedRaoResult.getFunctionalCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(10., importedRaoResult.getVirtualCost(INSTANT_CURATIVE, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., importedRaoResult.getVirtualCost(INSTANT_CURATIVE, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., importedRaoResult.getVirtualCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);
        assertEquals(-38, importedRaoResult.getCost(INSTANT_CURATIVE), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
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

        assertEquals(4220., importedRaoResult.getFlow(INSTANT_PREV, cnecP, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getFlow(INSTANT_PREV, cnecP, Side.RIGHT, AMPERE)));
        assertEquals(4221., importedRaoResult.getMargin(INSTANT_PREV, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(INSTANT_PREV, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, Side.RIGHT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, Side.RIGHT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecP, Side.LEFT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecP, Side.RIGHT)));

        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecP, RIGHT, AMPERE), importedRaoResult.getFlow(INSTANT_PREV, cnecP, RIGHT, AMPERE), DOUBLE_TOLERANCE);

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
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

        assertTrue(Double.isNaN(importedRaoResult.getFlow(INSTANT_PREV, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1210.5, importedRaoResult.getFlow(INSTANT_PREV, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(INSTANT_PREV, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(INSTANT_PREV, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INSTANT_PREV, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1213.5, importedRaoResult.getLoopFlow(INSTANT_PREV, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INSTANT_PREV, cnecO, Side.LEFT, MEGAWATT)));
        assertEquals(1214.5, importedRaoResult.getCommercialFlow(INSTANT_PREV, cnecO, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecO, Side.LEFT)));
        assertEquals(0.6, importedRaoResult.getPtdfZonalSum(INSTANT_PREV, cnecO, Side.RIGHT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_AUTO, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(INSTANT_PREV, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO. Results in CURATIVE are the same as AUTO because the CNEC is auto
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

        assertEquals(3220., importedRaoResult.getFlow(INSTANT_PREV, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(INSTANT_PREV, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(INSTANT_PREV, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(INSTANT_AUTO, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(INSTANT_AUTO, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(INSTANT_AUTO, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(INSTANT_AUTO, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(INSTANT_CURATIVE, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(INSTANT_AUTO, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in null, PREVENTIVE, and AUTO and in CURATIVE
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

        assertEquals(3220., importedRaoResult.getFlow(INSTANT_PREV, cnecC, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(INSTANT_PREV, cnecC, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(INSTANT_PREV, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(INSTANT_AUTO, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(INSTANT_AUTO, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(INSTANT_AUTO, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(INSTANT_CURATIVE, cnecC, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3410.5, importedRaoResult.getFlow(INSTANT_CURATIVE, cnecC, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3411., importedRaoResult.getMargin(INSTANT_CURATIVE, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- NetworkAction results ---
        // -----------------------------

        State pState = crac.getPreventiveState();
        State oState2 = crac.getState("contingency2Id", "outage");
        State aState2 = crac.getState("contingency2Id", "auto");
        State cState1 = crac.getState("contingency1Id", "curative");
        State cState2 = crac.getState("contingency2Id", "curative");

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
        assertEquals(3235., importedRaoResult.getAngle(INSTANT_PREV, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(INSTANT_PREV, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(INSTANT_AUTO, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(INSTANT_AUTO, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(INSTANT_CURATIVE, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(INSTANT_CURATIVE, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(INSTANT_PREV, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(INSTANT_PREV, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(INSTANT_AUTO, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(INSTANT_AUTO, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(INSTANT_CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(INSTANT_CURATIVE, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    private void testBaseContentOfV1Point3RaoResult(RaoResult importedRaoResult, Crac crac) {
        testBaseContentOfV1Point2RaoResult(importedRaoResult, crac);
        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", "curative")));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", "auto")));
    }
}
