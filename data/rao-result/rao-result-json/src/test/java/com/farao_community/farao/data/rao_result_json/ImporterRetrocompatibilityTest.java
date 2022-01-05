/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.data.crac_io_json.JsonImport;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
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
        Assert.assertEquals(4110., importedRaoResult.getFlow(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(4111., importedRaoResult.getMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(4112., importedRaoResult.getRelativeMargin(INITIAL, cnecP, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, MEGAWATT)));

        Assert.assertEquals(4220., importedRaoResult.getFlow(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(4221., importedRaoResult.getMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(4222., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecP, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecP, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecP, MEGAWATT)));

        Assert.assertEquals(0.4, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecP), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecP, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecP, AMPERE)));

        /*
        cnec1outageId: outage, with loop-flows, optimized
        - contains result in INITIAL and in AFTER_PRA, no result in AFTER_ARA and AFTER_CRA
        - contains result for loop-flows, commercial flows, relative margin and PTDF sum
         */

        FlowCnec cnecO = crac.getFlowCnec("cnec1outageId");
        Assert.assertEquals(1120., importedRaoResult.getFlow(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1121., importedRaoResult.getMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1122., importedRaoResult.getRelativeMargin(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1123., importedRaoResult.getLoopFlow(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(1124., importedRaoResult.getCommercialFlow(INITIAL, cnecO, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(1210., importedRaoResult.getFlow(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1211., importedRaoResult.getMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1212., importedRaoResult.getRelativeMargin(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1213., importedRaoResult.getLoopFlow(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(1214., importedRaoResult.getCommercialFlow(AFTER_PRA, cnecO, MEGAWATT), DOUBLE_TOLERANCE);

        Assert.assertEquals(0.1, importedRaoResult.getPtdfZonalSum(AFTER_PRA, cnecO), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_ARA, cnecO, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, MEGAWATT)));

        /*
        cnec3autoId: auto, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA, but not in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecA = crac.getFlowCnec("cnec3autoId");
        Assert.assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecA, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecA)));

        Assert.assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecA, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecA, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecA, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecA, MEGAWATT), DOUBLE_TOLERANCE);

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnecO, MEGAWATT)));

         /*
        cnec3curId: curative, without loop-flows, pureMNEC
        - contains result in INITIAL, AFTER_PRA, and AFTER_ARA and in AFTER_CRA
        - do not contain results for loop-flows, or relative margin
         */

        FlowCnec cnecC = crac.getFlowCnec("cnec3curId");
        Assert.assertEquals(3110., importedRaoResult.getFlow(INITIAL, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3111., importedRaoResult.getMargin(INITIAL, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(INITIAL, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(INITIAL, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(INITIAL, cnecC, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(INITIAL, cnecC)));

        Assert.assertEquals(3220., importedRaoResult.getFlow(AFTER_PRA, cnecC, AMPERE), DOUBLE_TOLERANCE);
        Assert.assertEquals(3221., importedRaoResult.getMargin(AFTER_PRA, cnecC, AMPERE), DOUBLE_TOLERANCE);

        Assert.assertEquals(3310., importedRaoResult.getFlow(AFTER_ARA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
        Assert.assertEquals(3311., importedRaoResult.getMargin(AFTER_ARA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);

        Assert.assertEquals(3410., importedRaoResult.getFlow(AFTER_CRA, cnecC, MEGAWATT), DOUBLE_TOLERANCE);
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
        Assert.assertEquals(0, importedRaoResult.getPreOptimizationTapOnState(pState, pstP));
        Assert.assertEquals(0., importedRaoResult.getPreOptimizationSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(-7, importedRaoResult.getOptimizedTapOnState(pState, pstP));
        Assert.assertEquals(-3.2, importedRaoResult.getOptimizedSetPointOnState(pState, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(-3.2, importedRaoResult.getPreOptimizationSetPointOnState(cState1, pstP), DOUBLE_TOLERANCE);
        Assert.assertEquals(-7, importedRaoResult.getPreOptimizationTapOnState(cState1, pstP));
        Assert.assertEquals(-7, importedRaoResult.getOptimizedTapOnState(cState1, pstP));
        Assert.assertEquals(-7, importedRaoResult.getOptimizedTapOnState(cState2, pstP));

        /*
        pstRange2Id, not activated
        */
        PstRangeAction pstN = crac.getPstRangeAction("pstRange2Id");
        assertFalse(importedRaoResult.isActivatedDuringState(pState, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState1, pstN));
        assertFalse(importedRaoResult.isActivatedDuringState(cState2, pstN));
        Assert.assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(pState, pstN));
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(pState, pstN));
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState1, pstN));
        Assert.assertEquals(3, importedRaoResult.getOptimizedTapOnState(cState2, pstN));

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
}
