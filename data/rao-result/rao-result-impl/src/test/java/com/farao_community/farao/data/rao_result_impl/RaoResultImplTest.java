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
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.AMPERE;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private RaoResultImpl raoResult;
    private Crac crac;
    private FlowCnec cnec;
    private PstRangeAction pst;
    private NetworkAction na;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private void setUp() {
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        cnec = crac.getFlowCnec("cnec1basecase");
        pst = crac.getPstRangeAction("pst");
        na = (NetworkAction) crac.newNetworkAction().withId("na-id")
            .newTopologicalAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(autoInstant).withUsageMethod(UsageMethod.FORCED).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(autoInstant).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnInstantUsageRule().withInstant(curativeInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        raoResult = new RaoResultImpl(crac);

        FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(cnec);

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(null);
        ElementaryFlowCnecResult elementaryFlowCnecResult = flowCnecResult.getResult(null);

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

        flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(preventiveInstant);
        elementaryFlowCnecResult = flowCnecResult.getResult(preventiveInstant);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 200., MEGAWATT);
        elementaryFlowCnecResult.setMargin(201., MEGAWATT);
        elementaryFlowCnecResult.setRelativeMargin(202., MEGAWATT);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 203., MEGAWATT);

        elementaryFlowCnecResult.setFlow(Side.LEFT, 210., AMPERE);
        elementaryFlowCnecResult.setMargin(211., AMPERE);
        elementaryFlowCnecResult.setRelativeMargin(212., AMPERE);
        elementaryFlowCnecResult.setLoopFlow(Side.LEFT, 213., AMPERE);

        elementaryFlowCnecResult.setPtdfZonalSum(Side.LEFT, 0.1);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", autoInstant));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant));

        RangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(pst);
        pstRangeActionResult.setInitialSetpoint(2.3); // tap = 6
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -3.1); // tap = -8

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(null);
        costResult.setFunctionalCost(100.);
        costResult.setVirtualCost("loopFlow", 0.);
        costResult.setVirtualCost("MNEC", 0.);

        costResult = raoResult.getAndCreateIfAbsentCostResult(CURATIVE_INSTANT_ID);
        costResult.setFunctionalCost(-50.);
        costResult.setVirtualCost("loopFlow", 10.);
        costResult.setVirtualCost("MNEC", 2.);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
    }

    private void getResultAtAGivenState(Instant optimizedInstant) {
        assertEquals(200., raoResult.getFlow(optimizedInstant, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(201., raoResult.getMargin(optimizedInstant, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(202., raoResult.getRelativeMargin(optimizedInstant, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(203., raoResult.getLoopFlow(optimizedInstant, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizedInstant, cnec, Side.LEFT, MEGAWATT)));

        assertEquals(210., raoResult.getFlow(optimizedInstant, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(211., raoResult.getMargin(optimizedInstant, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(212., raoResult.getRelativeMargin(optimizedInstant, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(213., raoResult.getLoopFlow(optimizedInstant, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertTrue(Double.isNaN(raoResult.getCommercialFlow(optimizedInstant, cnec, Side.LEFT, AMPERE)));

        assertEquals(0.1, raoResult.getPtdfZonalSum(optimizedInstant, cnec, Side.LEFT), DOUBLE_TOLERANCE);
    }

    @Test
    void testPreventiveCnecResults() {
        setUp();

        assertEquals(100., raoResult.getFlow(null, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(101., raoResult.getMargin(null, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(102., raoResult.getRelativeMargin(null, cnec, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(103., raoResult.getLoopFlow(null, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(104., raoResult.getCommercialFlow(null, cnec, Side.LEFT, MEGAWATT), DOUBLE_TOLERANCE);

        assertEquals(110., raoResult.getFlow(null, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(111., raoResult.getMargin(null, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(112., raoResult.getRelativeMargin(null, cnec, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(113., raoResult.getLoopFlow(null, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);
        assertEquals(114., raoResult.getCommercialFlow(null, cnec, Side.LEFT, AMPERE), DOUBLE_TOLERANCE);

        assertEquals(0.1, raoResult.getPtdfZonalSum(null, cnec, Side.LEFT), DOUBLE_TOLERANCE);

        // should always return after pra results because the cnec is Preventive
        getResultAtAGivenState(preventiveInstant);
        getResultAtAGivenState(autoInstant);
        getResultAtAGivenState(curativeInstant);
    }

    @Test
    void testPstRangeActionResults() {
        setUp();
        assertEquals(6, raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(2.3, raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertTrue(raoResult.isActivatedDuringState(crac.getPreventiveState(), pst));
        assertEquals(-8, raoResult.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        assertEquals(Map.of(pst, -8), raoResult.getOptimizedTapsOnState(crac.getPreventiveState()));
        assertEquals(-3.1, raoResult.getOptimizedSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
        assertEquals(Map.of(pst, -3.1), raoResult.getOptimizedSetPointsOnState(crac.getPreventiveState()));
        assertEquals(Set.of(pst), raoResult.getActivatedRangeActionsDuringState(crac.getPreventiveState()));
        assertEquals(Set.of(), raoResult.getActivatedRangeActionsDuringState(crac.getState("Contingency FR1 FR3", autoInstant)));
    }

    @Test
    void testNetworkActionResults() {
        setUp();
        assertFalse(raoResult.wasActivatedBeforeState(crac.getPreventiveState(), na));
        assertFalse(raoResult.isActivatedDuringState(crac.getPreventiveState(), na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(crac.getPreventiveState()));

        State state = crac.getState("Contingency FR1 FR3", autoInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR3", curativeInstant);
        assertTrue(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));

        state = crac.getState("Contingency FR1 FR2", autoInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertFalse(raoResult.isActivated(state, na));
        assertFalse(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(), raoResult.getActivatedNetworkActionsDuringState(state));
        state = crac.getState("Contingency FR1 FR2", curativeInstant);
        assertFalse(raoResult.wasActivatedBeforeState(state, na));
        assertTrue(raoResult.isActivated(state, na));
        assertTrue(raoResult.isActivatedDuringState(state, na));
        assertEquals(Set.of(na), raoResult.getActivatedNetworkActionsDuringState(state));
    }

    @Test
    void testCostResults() {
        setUp();

        assertEquals(Set.of("loopFlow", "MNEC"), raoResult.getVirtualCostNames());

        assertEquals(100., raoResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., raoResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., raoResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(-50., raoResult.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(10., raoResult.getVirtualCost(curativeInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., raoResult.getVirtualCost(curativeInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., raoResult.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(-38, raoResult.getCost(curativeInstant), DOUBLE_TOLERANCE);

        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
    }

    @Test
    void testOptimizedStepsExecuted() {
        setUp();
        assertFalse(raoResult.getOptimizationStepsExecuted().hasRunSecondPreventive());
        raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getOptimizationStepsExecuted());
        FaraoException exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> raoResult.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("The RaoResult object should not be modified outside of its usual routine", exception.getMessage());
    }

    @Test
    void testSensitivityStatus() {
        setUp();
        raoResult.setComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant), ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant)));
    }
}
