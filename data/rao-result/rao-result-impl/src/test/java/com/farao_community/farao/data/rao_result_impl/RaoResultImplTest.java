/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

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
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.AFTER_CRA;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.INITIAL;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private RaoResultImpl raoResult;
    private Crac crac;
    private FlowCnec cnec;
    private PstRangeAction pst;
    private NetworkAction na;

    private void setUp() {
        crac = CommonCracCreation.createWithCurativePstRange();
        cnec = crac.getFlowCnec("cnec1basecase");
        pst = crac.getPstRangeAction("pst");
        na = crac.newNetworkAction().withId("na-id")
                .newTopologicalAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .newOnStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.UNAVAILABLE).add()
                .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        raoResult = new RaoResultImpl();

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

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

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_CRA);
        elementaryFlowCnecResult = flowCnecResult.getResult(AFTER_CRA);

        elementaryFlowCnecResult.setFlow(200., MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(203., MEGAWATT);

        elementaryFlowCnecResult.setFlow(210., AMPERE);
        elementaryFlowCnecResult.setMargin(211., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(213., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(0.1);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", Instant.AUTO));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", Instant.CURATIVE));

        PstRangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentPstRangeActionResult(pst);
        pstRangeActionResult.setPreOptimTap(3);
        pstRangeActionResult.setPreOptimSetPoint(2.3);
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -7, -3.2);

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(INITIAL);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        costResult = raoResult.getAndCreateIfAbsentCostResult(AFTER_CRA);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
    }

    @Test
    public void testCurativeCnecResults() {
        setUp();

        assertEquals(100., raoResult.getFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(101., raoResult.getMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(102., raoResult.getRelativeMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(103., raoResult.getLoopFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(104., raoResult.getCommercialFlow(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(110., raoResult.getFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(111., raoResult.getMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(112., raoResult.getRelativeMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(113., raoResult.getLoopFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(114., raoResult.getCommercialFlow(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(0.1, raoResult.getPtdfZonalSum(INITIAL, cnec), DOUBLE_TOLERANCE);

        // should have after cra results
        assertEquals(200., raoResult.getFlow(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(201., raoResult.getMargin(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(202., raoResult.getRelativeMargin(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(203., raoResult.getLoopFlow(AFTER_CRA, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(AFTER_CRA, cnec, MEGAWATT)));

        assertEquals(210., raoResult.getFlow(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(211., raoResult.getMargin(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(212., raoResult.getRelativeMargin(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(213., raoResult.getLoopFlow(AFTER_CRA, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(AFTER_CRA, cnec, AMPERE)));

        assertEquals(0.1, raoResult.getPtdfZonalSum(AFTER_CRA, cnec), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPstRangeActionResults() {
        setUp();
        assertEquals(3, raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(2.3, raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertTrue(raoResult.isActivatedDuringState(crac.getPreventiveState(), pst));
        assertEquals(-7, raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        assertEquals(Map.of(pst, -7), raoResult.getOptimizedTapsOnState(crac.getPreventiveState()));
        assertEquals(-3.2, raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pst, -3.2), raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()));
        assertEquals(Set.of(pst), raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(), raoResult.getActivatedRangeActionsDuringState(crac.getState("Contingency FR1 FR3", Instant.AUTO)));
    }

    @Test
    public void testNetworkActionResults() {
        setUp();
        assertFalse(raoResult.wasActivatedBeforeState(crac.getPreventiveState(), na));
        assertFalse(raoResult.isActivatedDuringState(crac.getPreventiveState(), na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));

        State state = crac.getState("Contingency FR1 FR3", Instant.AUTO);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        assertTrue(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));

        state = crac.getState("Contingency FR1 FR2", Instant.AUTO);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertFalse(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
    }

    @Test
    public void testCostResults() {
        setUp();

        assertEquals(Set.of("loopFlow", "MNEC"), raoResult.getVirtualCostNames());

        assertEquals(100., raoResult.getFunctionalCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(INITIAL, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(INITIAL, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(INITIAL), DOUBLE_TOLERANCE);
        assertEquals(100., raoResult.getCost(INITIAL), DOUBLE_TOLERANCE);

        assertEquals(-50., raoResult.getFunctionalCost(AFTER_CRA), DOUBLE_TOLERANCE);
        assertEquals(10., raoResult.getVirtualCost(AFTER_CRA, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., raoResult.getVirtualCost(AFTER_CRA, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., raoResult.getVirtualCost(AFTER_CRA), DOUBLE_TOLERANCE);
        assertEquals(-38, raoResult.getCost(AFTER_CRA), DOUBLE_TOLERANCE);

        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
    }
}
