/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private RaoResultImpl raoResult;
    private Crac crac;
    private PstRangeAction pst;
    private NetworkAction na;
    private Instant autoInstant;
    private Instant curativeInstant;

    private void setUp() {
        crac = CommonCracCreation.createWithPreventiveAndCurativePstRange();
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        pst = crac.getPstRangeAction("pst");
        na = crac.newNetworkAction().withId("na-id")
            .newSwitchAction().withNetworkElement("any").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR3").withInstant(AUTO_INSTANT_ID).add()
            .newOnContingencyStateUsageRule().withContingency("Contingency FR1 FR2").withInstant(AUTO_INSTANT_ID).add()
            .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).add()
            .add();

        raoResult = new RaoResultImpl(crac);

        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR3", autoInstant));
        raoResult.getAndCreateIfAbsentNetworkActionResult(na).addActivationForState(crac.getState("Contingency FR1 FR2", curativeInstant));

        RangeActionResult pstRangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(pst);
        pstRangeActionResult.setInitialSetpoint(2.3); // tap = 6
        pstRangeActionResult.addActivationForState(crac.getPreventiveState(), -3.1); // tap = -8

        CostResult costResult = new CostResult();
        costResult.addFunctionalCostResult(null, 100.0);
        costResult.addVirtualCostResult(null, "loopFlow", 0.0);
        costResult.addVirtualCostResult(null, "MNEC", 0.0);

        costResult.addFunctionalCostResult(crac.getInstant(CURATIVE_INSTANT_ID), -50.0);
        costResult.addVirtualCostResult(crac.getInstant(CURATIVE_INSTANT_ID), "loopFlow", 10.0);
        costResult.addVirtualCostResult(crac.getInstant(CURATIVE_INSTANT_ID), "MNEC", 2.0);

        raoResult.addExtension(CostResult.class, costResult);

        raoResult.setComputationStatus(ComputationStatus.DEFAULT);
    }

    @Test
    void testPstRangeActionResults() {
        setUp();
        assertEquals(0, raoResult.getPreOptimizationTapOnState(crac.getPreventiveState(), pst));
        assertEquals(0.0, raoResult.getPreOptimizationSetPointOnState(crac.getPreventiveState(), pst), DOUBLE_TOLERANCE);
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

        CostResult costResult = raoResult.getExtension(CostResult.class);
        assertNotNull(costResult);

        assertEquals(Set.of("loopFlow", "MNEC"), costResult.getVirtualCostNames());

        assertEquals(100., costResult.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(0., costResult.getVirtualCost(null, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(0., costResult.getVirtualCost(null, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(0., costResult.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(100., costResult.getCost(null), DOUBLE_TOLERANCE);

        assertEquals(-50., costResult.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(10., costResult.getVirtualCost(curativeInstant, "loopFlow"), DOUBLE_TOLERANCE);
        assertEquals(2., costResult.getVirtualCost(curativeInstant, "MNEC"), DOUBLE_TOLERANCE);
        assertEquals(12., costResult.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
        assertEquals(-38, costResult.getCost(curativeInstant), DOUBLE_TOLERANCE);

        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus());
    }

    @Test
    void testExecutionDetails() {
        setUp();
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, raoResult.getExecutionDetails());
        raoResult.setExecutionDetails(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, raoResult.getExecutionDetails());
    }

    @Test
    void testSensitivityStatus() {
        setUp();
        raoResult.setComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant), ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, raoResult.getComputationStatus(crac.getState("Contingency FR1 FR3", autoInstant)));
    }
}
