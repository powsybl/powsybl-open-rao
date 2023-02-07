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
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.cnec.Side.*;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.AFTER_CRA;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class ImporterRetrocompatibilityTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;

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
    public void importV1Point0Test() {

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
    public void importV1Point1Test() {

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
    public void importAfterV1Point1FieldDeprecationTest() {

        // unused field should throw an exception

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
    }

    @Test
    public void importV1Point2Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
    }

    @Test
    public void importV1Point2FieldDeprecationTest() {
        // RaoResult copied from v1.1 but version set to v1.2
        // Should not be imported because CNEC side is not defined properly
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));
    }

    @Test
    public void importV1Point3Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");

        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FALLBACK, raoResult.getComputationStatus(crac.getState("contingency1Id", OUTAGE)));
        assertEquals(ComputationStatus.FAILURE, raoResult.getComputationStatus(crac.getState("contingency1Id", CURATIVE)));
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("contingency2Id", AUTO)));
        assertEquals(ComputationStatus.FALLBACK, raoResult.getComputationStatus(crac.getState("contingency2Id", CURATIVE)));

    }

    @Test
    public void importV1Point3TestFieldDeprecationTest() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");
        Crac crac = new JsonImport().importCrac(cracFile);
        RaoResultImporter importer = new RaoResultImporter();

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error1.json");
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile, crac));

        InputStream raoResultFile2 = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error2.json");
        assertThrows(FaraoException.class, () -> importer.importRaoResult(raoResultFile2, crac));
    }

    private void testBaseContentOfV1RaoResult(RaoResult importedRaoResult, Crac crac) {

        // --------------------------
        // --- Computation status ---
        // --------------------------
        Assert.assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());

        // --------------------------
        // --- test Costs results ---
        // --------------------------
        Assert.assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        Assert.assertEquals(100., importedRaoResult.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "loopFlow"), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "MNEC"), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        Assert.assertEquals(100., importedRaoResult.getCost(INITIAL), DOUBLE_TOLERANCE);

        Assert.assertEquals(80., importedRaoResult.getFunctionalCost(AFTER_PRA), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA, "loopFlow"), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA, "MNEC"), DOUBLE_TOLERANCE);
        Assert.assertEquals(0., importedRaoResult.getVirtualCost(AFTER_PRA), DOUBLE_TOLERANCE);
        Assert.assertEquals(80., importedRaoResult.getCost(AFTER_PRA), DOUBLE_TOLERANCE);

        Assert.assertEquals(-20., importedRaoResult.getFunctionalCost(AFTER_ARA), DOUBLE_TOLERANCE);
        Assert.assertEquals(15., importedRaoResult.getVirtualCost(AFTER_ARA, "loopFlow"), DOUBLE_TOLERANCE);
        Assert.assertEquals(20., importedRaoResult.getVirtualCost(AFTER_ARA, "MNEC"), DOUBLE_TOLERANCE);
        Assert.assertEquals(35., importedRaoResult.getVirtualCost(AFTER_ARA), DOUBLE_TOLERANCE);
        Assert.assertEquals(15., importedRaoResult.getCost(AFTER_ARA), DOUBLE_TOLERANCE);

        Assert.assertEquals(-50., importedRaoResult.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);
        Assert.assertEquals(10., importedRaoResult.getVirtualCost(AFTER_CRA, "loopFlow"), DOUBLE_TOLERANCE);
        Assert.assertEquals(2., importedRaoResult.getVirtualCost(AFTER_CRA, "MNEC"), DOUBLE_TOLERANCE);
        Assert.assertEquals(12., importedRaoResult.getVirtualCost(AFTER_CRA), DOUBLE_TOLERANCE);
        Assert.assertEquals(-38, importedRaoResult.getCost(AFTER_CRA), DOUBLE_TOLERANCE);

        // -----------------------------
        // --- test FlowCnec results ---
        // -----------------------------

        /*
        cnec4prevId: preventive, no loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA, no result in AFTER_ARA and AFTER_CRA
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        Assert.assertEquals(4110., importedRaoResult.getFlow(INITIAL, cnecP, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(4111., importedRaoResult.getMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(4112., importedRaoResult.getRelativeMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, LEFT, MEGAWATT)));

        Assert.assertEquals(4220., importedRaoResult.getFlow(AFTER_PRA, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(4221., importedRaoResult.getMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(4222., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, LEFT, MEGAWATT)));

        Assert.assertEquals(0.4, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecP, LEFT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecP, LEFT, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecP, LEFT, AMPERE)));

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA, no result in AFTER_ARA and AFTER_CRA
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        Assert.assertEquals(1120., importedRaoResult.getFlow(INITIAL, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1121., importedRaoResult.getMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1122., importedRaoResult.getRelativeMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1123., importedRaoResult.getLoopFlow(INITIAL, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1124., importedRaoResult.getCommercialFlow(INITIAL, cnecO, LEFT, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(1210., importedRaoResult.getFlow(AFTER_PRA, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1211., importedRaoResult.getMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1212., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1213., importedRaoResult.getLoopFlow(AFTER_PRA, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1214., importedRaoResult.getCommercialFlow(AFTER_PRA, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        Assert.assertEquals(0.1, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecO, LEFT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecO, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, LEFT, MEGAWATT)));

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA, but not in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        Assert.assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecA, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecA, LEFT)));

        Assert.assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecA, LEFT, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecA, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, LEFT, MEGAWATT)));

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA and in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        Assert.assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecC, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecC, LEFT)));

        Assert.assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecC, LEFT, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecC, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        Assert.assertEquals(3410., importedRaoResult.getFlow(AFTER_CRA, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3411., importedRaoResult.getMargin(AFTER_CRA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

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
        Assert.assertEquals(-3, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        Assert.assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        Assert.assertEquals(3., importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(3., importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        Assert.assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        Assert.assertEquals(0, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        Assert.assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        Assert.assertEquals(0, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

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
        Assert.assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        Assert.assertEquals(-100, importedRaoResult.getOptimizedSetPointOnState(pState, hvdcC), DOUBLE_TOLERANCE);
        Assert.assertEquals(-100, importedRaoResult.getPreOptimizationSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        Assert.assertEquals(100, importedRaoResult.getOptimizedSetPointOnState(cState1, hvdcC), DOUBLE_TOLERANCE);
        Assert.assertEquals(400, importedRaoResult.getOptimizedSetPointOnState(cState2, hvdcC), DOUBLE_TOLERANCE);
    }

    private void testExtraContentOfV1Point1RaoResult(RaoResult importedRaoResult, Crac crac) {
        assertEquals(-1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pstRange3Id")));

        InjectionRangeAction rangeAction = crac.getInjectionRangeAction("injectionRange1Id");
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction), DOUBLE_TOLERANCE);
        assertEquals(-300., importedRaoResult.getOptimizedSetPointOnState(crac.getState("contingency1Id", Instant.CURATIVE), rangeAction), DOUBLE_TOLERANCE);

        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(INITIAL, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(INITIAL, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(AFTER_PRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(AFTER_PRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(AFTER_ARA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(AFTER_ARA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(AFTER_CRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(AFTER_CRA, angleCnec, DEGREE), DOUBLE_TOLERANCE);

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

    private void testBaseContentOfV1Point2RaoResult(RaoResult importedRaoResult, Crac crac) {
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
}
