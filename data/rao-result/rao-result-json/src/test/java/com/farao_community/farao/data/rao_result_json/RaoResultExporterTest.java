/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_impl.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoResultExporterTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private RaoResult importedRaoResult;
    private Crac crac;
    private FlowCnec cnec;
    private PstRangeAction pst;
    private NetworkAction na;

    private void setUp() {
        setUp("cnec1basecase");
    }

    private void setUp(String cnecId) {
        crac = CommonCracCreation.createWithCurativePstRange();
        cnec = crac.getFlowCnec(cnecId);
        pst = crac.getPstRangeAction("pst");
        na = crac.newNetworkAction().withId("na-id")
            .newTopologicalAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .newOnStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        RaoResultImpl raoResult = new RaoResultImpl();

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

        // FlowCnec result at INITIAL state
        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(INITIAL);
        ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(INITIAL);

        elementaryFlowCnecResult.setFlow(100., MEGAWATT);
        elementaryFlowCnecResult.setMargin(101., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(103., MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(104., MEGAWATT);

        elementaryFlowCnecResult.setFlow(110., AMPERE);
        elementaryFlowCnecResult.setMargin(111., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(112., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(113., AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(114., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        // FlowCnec result after ARA
        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_ARA);
        elementaryFlowCnecResult = flowCnecResult.getResult(AFTER_ARA);

        elementaryFlowCnecResult.setFlow(200., MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(203., MEGAWATT);

        elementaryFlowCnecResult.setFlow(210., AMPERE);
        elementaryFlowCnecResult.setMargin(211., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(213., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        // FlowCnec result after CRA
        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_CRA);
        elementaryFlowCnecResult = flowCnecResult.getResult(AFTER_CRA);

        elementaryFlowCnecResult.setFlow(300., MEGAWATT);
        elementaryFlowCnecResult.setMargin(301., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(302., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(303., MEGAWATT);

        elementaryFlowCnecResult.setFlow(310., AMPERE);
        elementaryFlowCnecResult.setMargin(311., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(312., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(313., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        // NetworkAction activations
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", Instant.AUTO));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", Instant.CURATIVE));

        // PST result
        PstRangeActionResult pstRangeActionResult = (PstRangeActionResult) raoResult.getAndCreateIfAbsentRangeActionResult(pst);
        pstRangeActionResult.setPreOptimTap(3);
        pstRangeActionResult.setPreOptimSetPoint(2.3);
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -7, -3.2);

        // CostResult at initial state
        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(INITIAL);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        // CostResult after ARA
        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_ARA);
        costResult.setFunctionalCost(-20.);
        costResult.setVirtualCost("loopFlow", 15.);
        costResult.setVirtualCost("MNEC", 20.);

        // CostResult after CRA
        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_CRA);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new RaoResultExporter().export(raoResult, crac, outputStream);

        // import RaoResult
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        importedRaoResult = new RaoResultImporter().importRaoResult(inputStream, crac);
    }

    private void checkInitialCnecResults() {
        assertEquals(100., importedRaoResult.getFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(101., importedRaoResult.getMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(102., importedRaoResult.getRelativeMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(103., importedRaoResult.getLoopFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(104., importedRaoResult.getCommercialFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(110., importedRaoResult.getFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(111., importedRaoResult.getMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(112., importedRaoResult.getRelativeMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(113., importedRaoResult.getLoopFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(114., importedRaoResult.getCommercialFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(INITIAL, cnec), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPreventiveCnecResults() {
        setUp("cnec1basecase");

        checkInitialCnecResults();

        // shouldn't have after cra results
        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnec, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getMargin(AFTER_CRA, cnec, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(AFTER_CRA, cnec, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(AFTER_CRA, cnec, MEGAWATT)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_CRA, cnec, MEGAWATT)));

        assertTrue(Double.isNaN(importedRaoResult.getFlow(AFTER_CRA, cnec, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getMargin(AFTER_CRA, cnec, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getRelativeMargin(AFTER_CRA, cnec, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getLoopFlow(AFTER_CRA, cnec, AMPERE)));
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_CRA, cnec, AMPERE)));

        assertTrue(Double.isNaN(importedRaoResult.getPtdfZonalSum(AFTER_CRA, cnec)));
    }

    @Test
    public void testCurativeCnecResults() {
        setUp("cnec1stateCurativeContingency1");

        checkInitialCnecResults();

        // should have after cra results
        assertEquals(300., importedRaoResult.getFlow(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(301., importedRaoResult.getMargin(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(302., importedRaoResult.getRelativeMargin(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(303., importedRaoResult.getLoopFlow(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_CRA, cnec, MEGAWATT)));

        assertEquals(310., importedRaoResult.getFlow(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(311., importedRaoResult.getMargin(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(312., importedRaoResult.getRelativeMargin(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(313., importedRaoResult.getLoopFlow(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_CRA, cnec, AMPERE)));

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(AFTER_CRA, cnec), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAutoCnecResults() {
        setUp("cnec1stateCurativeContingency1");

        checkInitialCnecResults();

        // should have after cra results
        assertEquals(200., importedRaoResult.getFlow(AFTER_ARA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(201., importedRaoResult.getMargin(AFTER_ARA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(202., importedRaoResult.getRelativeMargin(AFTER_ARA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(203., importedRaoResult.getLoopFlow(AFTER_ARA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_ARA, cnec, MEGAWATT)));

        assertEquals(210., importedRaoResult.getFlow(AFTER_ARA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(211., importedRaoResult.getMargin(AFTER_ARA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(212., importedRaoResult.getRelativeMargin(AFTER_ARA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(213., importedRaoResult.getLoopFlow(AFTER_ARA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(importedRaoResult.getCommercialFlow(AFTER_ARA, cnec, AMPERE)));

        assertEquals(0.1, importedRaoResult.getPtdfZonalSum(AFTER_ARA, cnec), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPstRangeActionResults() {
        setUp();
        assertEquals(3, importedRaoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(2.3, importedRaoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertTrue(importedRaoResult.isActivatedDuringState(crac.getPreventiveState(), pst));
        assertEquals(-7, importedRaoResult.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        assertEquals(Map.of(pst, -7), importedRaoResult.getOptimizedTapsOnState(crac.getPreventiveState()));
        assertEquals(-3.2, importedRaoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pst, -3.2), importedRaoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()));
        assertEquals(Set.of(), importedRaoResult.getActivatedRangeActionsDuringState(crac.getState("Contingency FR1 FR3", Instant.AUTO)));
    }

    @Test
    public void testNetworkActionResults() {
        setUp();
        assertFalse(importedRaoResult.wasActivatedBeforeState(crac.getPreventiveState(), na));
        assertFalse(importedRaoResult.isActivatedDuringState(crac.getPreventiveState(), na));
        assertEquals(Set.of(), importedRaoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));

        State state = crac.getState("Contingency FR1 FR3", Instant.AUTO);
        assertFalse(importedRaoResult.wasActivatedBeforeState(state, na));
        assertTrue(importedRaoResult.isActivated(state, na));
        assertTrue(importedRaoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), importedRaoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        assertTrue(importedRaoResult.wasActivatedBeforeState(state, na));
        assertTrue(importedRaoResult.isActivated(state, na));
        assertFalse(importedRaoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), importedRaoResult.getActivatedNetworkActionsDuringState(state));

        state = crac.getState("Contingency FR1 FR2", Instant.AUTO);
        assertFalse(importedRaoResult.wasActivatedBeforeState(state, na));
        assertFalse(importedRaoResult.isActivated(state, na));
        assertFalse(importedRaoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), importedRaoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);
        assertFalse(importedRaoResult.wasActivatedBeforeState(state, na));
        assertTrue(importedRaoResult.isActivated(state, na));
        assertTrue(importedRaoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), importedRaoResult.getActivatedNetworkActionsDuringState(state));
    }

    @Test
    public void testCostResults() {
        setUp();

        assertEquals(Set.of("loopFlow", "MNEC"), importedRaoResult.getVirtualCostNames());

        assertEquals(100., importedRaoResult.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., importedRaoResult.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(100., importedRaoResult.getCost(INITIAL), DOUBLE_TOLERANCE);

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

        assertEquals(ComputationStatus.DEFAULT, importedRaoResult.getComputationStatus());
    }
}
