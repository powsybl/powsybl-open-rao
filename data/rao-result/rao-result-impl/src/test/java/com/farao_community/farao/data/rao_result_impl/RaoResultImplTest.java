/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static org.junit.jupiter.api.Assertions.*;

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
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        cnec = crac.getFlowCnec("cnec1basecase");
        pst = crac.getPstRangeAction("pst");
        na = crac.newNetworkAction().withId("na-id")
            .newTopologicalAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
            .newOnStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(Instant.AUTO).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newFreeToUseUsageRule().withInstant(Instant.CURATIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoResult = new RaoResultImpl(crac);

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(INITIAL);
        ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(INITIAL);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 100., MEGAWATT);
        elementaryFlowCnecResult.setMargin(101., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(102., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 103., MEGAWATT);
        elementaryFlowCnecResult.setCommercialFlow(Side.LEFT, 104., MEGAWATT);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 110., AMPERE);
        elementaryFlowCnecResult.setMargin(111., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(112., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 113., AMPERE);
        elementaryFlowCnecResult.setCommercialFlow(Side.LEFT, 114., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(Side.LEFT, 0.1);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(AFTER_PRA);
        elementaryFlowCnecResult = flowCnecResult.getResult(AFTER_PRA);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 200., MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 203., MEGAWATT);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 210., AMPERE);
        elementaryFlowCnecResult.setMargin(211., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 213., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(Side.LEFT, 0.1);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", Instant.AUTO));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", Instant.CURATIVE));

        RangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(pst);
        pstRangeActionResult.setInitialSetpoint(2.3); // tap = 6
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -3.1); // tap = -8

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

    private void getResultAtAGivenState(OptimizationState optimizationState) {
        assertEquals(200., raoResult.getFlow(optimizationState, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(201., raoResult.getMargin(optimizationState, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(202., raoResult.getRelativeMargin(optimizationState, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(203., raoResult.getLoopFlow(optimizationState, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizationState, cnec, Side.LEFT, MEGAWATT)));

        assertEquals(210., raoResult.getFlow(optimizationState, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(211., raoResult.getMargin(optimizationState, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(212., raoResult.getRelativeMargin(optimizationState, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(213., raoResult.getLoopFlow(optimizationState, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizationState, cnec, Side.LEFT, AMPERE)));

        assertEquals(0.1, raoResult.getPtdfZonalSum(optimizationState, cnec, Side.LEFT), DOUBLE_TOLERANCE);
    }

    @Test
    public void testPreventiveCnecResults() {
        setUp();

        assertEquals(100., raoResult.getFlow(INITIAL, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(101., raoResult.getMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(102., raoResult.getRelativeMargin(INITIAL, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(103., raoResult.getLoopFlow(INITIAL, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(104., raoResult.getCommercialFlow(INITIAL, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(110., raoResult.getFlow(INITIAL, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(111., raoResult.getMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(112., raoResult.getRelativeMargin(INITIAL, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(113., raoResult.getLoopFlow(INITIAL, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(114., raoResult.getCommercialFlow(INITIAL, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(0.1, raoResult.getPtdfZonalSum(INITIAL, cnec, Side.LEFT), DOUBLE_TOLERANCE);

        // should always return after pra results because the cnec is Preventive
        getResultAtAGivenState(AFTER_PRA);
        getResultAtAGivenState(AFTER_ARA);
        getResultAtAGivenState(AFTER_CRA);
    }

    @Test
    public void testPstRangeActionResults() {
        setUp();
        assertEquals(6, raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(2.3, raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertTrue(raoResult.isActivatedDuringState(crac.getPreventiveState(), pst));
        assertEquals(-8, raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        assertEquals(Map.of(pst, -8), raoResult.getOptimizedTapsOnState(crac.getPreventiveState()));
        assertEquals(-3.1, raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pst, -3.1), raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()));
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

    @Test
    public void testOptimizedStepsExecuted() {
        setUp();
        assertFalse(raoResult.getOptimizationStepsExecuted().hasRunSecondPreventive());
        raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getOptimizationStepsExecuted());
        assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    public void testSensitivityStatus() {
        setUp();
        raoResult.setComputationStatus(crac.getState("Contingency FR1 FR3", Instant.AUTO), ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("Contingency FR1 FR3", Instant.AUTO)));
    }
}
