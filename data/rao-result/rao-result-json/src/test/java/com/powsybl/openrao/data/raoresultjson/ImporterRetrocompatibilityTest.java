/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultjson;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.craciojson.JsonImport;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.openrao.data.cracapi.cnec.Side.LEFT;
import static com.powsybl.openrao.data.cracapi.cnec.Side.RIGHT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class ImporterRetrocompatibilityTest {

    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    /*
    The goal of this test class is to ensure that former JSON RaoResult files are
    still importable, even when modifications are brought to the JSON importer.
     */

    /*
    CARE: the existing json file used in this test case SHOULD NOT BE MODIFIED. If
    the current tests do not pass, it means that formerly generated JSON RaoResult
    will not be compatible anymore with the next version of open-rao -> This is NOT
    desirable.

    Instead, we need to ensure that the JSON RaoResult files used in this class can
    still be imported as is. Using versioning of the importer if needed.
     */

    private static Network getMockedNetwork() {
        Network network = Mockito.mock(Network.class);
        Identifiable ne = Mockito.mock(Identifiable.class);
        Mockito.when(ne.getType()).thenReturn(IdentifiableType.SHUNT_COMPENSATOR);
        Mockito.when(network.getIdentifiable("injection")).thenReturn(ne);
        for (String lineId : List.of("ne1Id", "ne2Id", "ne3Id")) {
            Branch l = Mockito.mock(Line.class);
            Mockito.when(l.getId()).thenReturn(lineId);
            Mockito.when(network.getIdentifiable(lineId)).thenReturn(l);
        }
        return network;
    }

    @Test
    void importV1Point0Test() {

        // JSON file of open-rao v3.4.3
        /*
         versioning was not yet in place, and version number does not explicitly appear
         in v1.0 files
         */
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/rao-result-v1.0.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.0/crac-for-rao-result-v1.0.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point1Test() {

        // JSON file of open-rao v3.5.0
        /*
         addition of versioning, no changes apart from the fact that version numbers
         are now added in the first lines of the json
         */

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.1.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1RaoResult(raoResult, crac);
        testExtraContentOfV1Point1RaoResult(raoResult, crac);
    }

    @Test
    void importAfterV1Point1FieldDeprecationTest() {

        // unused field should throw an exception

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.1/crac-for-rao-result-v1.1.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResultImporter importer = new RaoResultImporter();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("Cannot deserialize RaoResult: field flow in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", exception.getMessage());
    }

    @Test
    void importV1Point2Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point2RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point2FieldDeprecationTest() {
        // RaoResult copied from v1.1 but version set to v1.2
        // Should not be imported because CNEC side is not defined properly
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/rao-result-v1.2-error.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.2/crac-for-rao-result-v1.2.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResultImporter importer = new RaoResultImporter();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("Cannot deserialize RaoResult: field flow in flowCnecResults in not supported in file version 1.2 (last supported in version 1.1)", exception.getMessage());
    }

    @Test
    void importV1Point3Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point3RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point4Test() {
        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.4/rao-result-v1.4.json");
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.4/crac-for-rao-result-v1.4.json");

        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResult raoResult = new RaoResultImporter().importRaoResult(raoResultFile, crac);

        testBaseContentOfV1Point3RaoResult(raoResult, crac);
    }

    @Test
    void importV1Point3TestFieldDeprecationTest() {
        InputStream cracFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/crac-for-rao-result-v1.3.json");
        Crac crac = new JsonImport().importCrac(cracFile, getMockedNetwork());
        RaoResultImporter importer = new RaoResultImporter();

        InputStream raoResultFile = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error1.json");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> importer.importRaoResult(raoResultFile, crac));
        assertEquals("Cannot deserialize RaoResult: field pstRangeActionId in RAO_RESULT in not supported in file version 1.3 (last supported in version 1.2)", exception.getMessage());

        InputStream raoResultFile2 = getClass().getResourceAsStream("/retrocompatibility/v1.3/rao-result-v1.3-error2.json");
        exception = assertThrows(OpenRaoException.class, () -> importer.importRaoResult(raoResultFile2, crac));
        assertEquals("Cannot deserialize RaoResult: field afterPraTap in rangeActionResults in not supported in file version 1.3 (last supported in version 1.2)", exception.getMessage());
    }

    private void testBaseContentOfV1RaoResult(RaoResult importedRaoResult, Crac crac) {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

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
        - contains result in null and in PREVENTIVE. Results in AUTO and CURATIVE are the same as PREVENTIVE because the CNEC is preventive
        - contains result relative margin and PTDF sum but not for loop and commercial flows
         */
        FlowCnec cnecP = crac.getFlowCnec("cnec4prevId");
        assertEquals(4110., importedRaoResult.getFlow(null, cnecP, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4111., importedRaoResult.getMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(4112., importedRaoResult.getRelativeMargin(null, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)));

        assertEquals(4220., importedRaoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4221., importedRaoResult.getMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(4222., importedRaoResult.getRelativeMargin(preventiveInstant, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(null, cnecP, LEFT, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(null, cnecP, LEFT, MEGAWATT)));

        assertEquals(0.4, importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecP, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(autoInstant, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecP, LEFT, AMPERE), importedRaoResult.getFlow(preventiveInstant, cnecP, LEFT, AMPERE), DOUBLE_TOLERANCE);

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

        assertEquals(1210., importedRaoResult.getFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1211., importedRaoResult.getMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1212., importedRaoResult.getRelativeMargin(preventiveInstant, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1213., importedRaoResult.getLoopFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(1214., importedRaoResult.getCommercialFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(preventiveInstant, cnecO, LEFT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(autoInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

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

        assertEquals(3220., importedRaoResult.getFlow(preventiveInstant, cnecA, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(preventiveInstant, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(autoInstant, cnecA, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(autoInstant, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(autoInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);

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

        assertEquals(3220., importedRaoResult.getFlow(preventiveInstant, cnecC, LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(preventiveInstant, cnecC, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(autoInstant, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(autoInstant, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(3410., importedRaoResult.getFlow(curativeInstant, cnecC, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
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
    }

    private void testExtraContentOfV1Point1RaoResult(RaoResult importedRaoResult, Crac crac) {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        assertEquals(-1, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), crac.getPstRangeAction("pstRange3Id")));

        InjectionRangeAction rangeAction = crac.getInjectionRangeAction("injectionRange1Id");
        assertEquals(100., importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), rangeAction), DOUBLE_TOLERANCE);
        assertEquals(-300., importedRaoResult.getOptimizedSetPointOnState(crac.getState("contingency1Id", curativeInstant), rangeAction), DOUBLE_TOLERANCE);

        AngleCnec angleCnec = crac.getAngleCnec("angleCnecId");
        assertEquals(3135., importedRaoResult.getAngle(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3131., importedRaoResult.getMargin(null, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3235., importedRaoResult.getAngle(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    private void testBaseContentOfV1Point2RaoResult(RaoResult importedRaoResult, Crac crac) {
        Instant preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        Instant outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

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

        assertEquals(importedRaoResult.getFlow(autoInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(autoInstant, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(preventiveInstant, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

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

        assertEquals(3220., importedRaoResult.getFlow(preventiveInstant, cnecA, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3220.5, importedRaoResult.getFlow(preventiveInstant, cnecA, Side.RIGHT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(3221., importedRaoResult.getMargin(preventiveInstant, cnecA, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(3310., importedRaoResult.getFlow(autoInstant, cnecA, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3310.5, importedRaoResult.getFlow(autoInstant, cnecA, Side.RIGHT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(3311., importedRaoResult.getMargin(autoInstant, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, LEFT, MEGAWATT), importedRaoResult.getFlow(autoInstant, cnecO, LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(importedRaoResult.getFlow(curativeInstant, cnecO, RIGHT, MEGAWATT), importedRaoResult.getFlow(autoInstant, cnecO, RIGHT, MEGAWATT), DOUBLE_TOLERANCE);

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
        assertEquals(3235., importedRaoResult.getAngle(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3231., importedRaoResult.getMargin(preventiveInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3335., importedRaoResult.getAngle(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3331., importedRaoResult.getMargin(autoInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3435., importedRaoResult.getAngle(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3431., importedRaoResult.getMargin(curativeInstant, angleCnec, DEGREE), DOUBLE_TOLERANCE);

        /*
        VoltageCnec
        */
        VoltageCnec voltageCnec = crac.getVoltageCnec("voltageCnecId");
        assertEquals(4146., importedRaoResult.getVoltage(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4141., importedRaoResult.getMargin(null, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4246., importedRaoResult.getVoltage(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4241., importedRaoResult.getMargin(preventiveInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4346., importedRaoResult.getVoltage(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4341., importedRaoResult.getMargin(autoInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4446., importedRaoResult.getVoltage(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(4441., importedRaoResult.getMargin(curativeInstant, voltageCnec, KILOVOLT), DOUBLE_TOLERANCE);
    }

    private void testBaseContentOfV1Point3RaoResult(RaoResult importedRaoResult, Crac crac) {
        Instant autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        Instant curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

        testBaseContentOfV1Point2RaoResult(importedRaoResult, crac);
        // Test computation status map
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getPreventiveState()));
        assertEquals(ComputationStatus.FAILURE, importedRaoResult.getComputationStatus(crac.getState("contingency1Id", curativeInstant)));
        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus(crac.getState("contingency2Id", autoInstant)));
    }
}
