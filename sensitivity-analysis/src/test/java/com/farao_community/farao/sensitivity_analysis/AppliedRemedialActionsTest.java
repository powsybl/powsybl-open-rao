/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class AppliedRemedialActionsTest {

    private Network network;
    private Crac crac;
    private NetworkAction networkAction;
    private NetworkAction autoNetworkAction;
    private PstRangeAction pstRangeAction;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithCurativePstRange();
        pstRangeAction = crac.getPstRangeAction("pst");
        networkAction = crac.newNetworkAction()
                .withId("na-id")
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
                .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(crac.getInstant(Instant.Kind.CURATIVE)).add()
                .add();
        autoNetworkAction = crac.newNetworkAction()
            .withId("na-auto-id")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(crac.getInstant(Instant.Kind.AUTO)).add()
            .add();
    }

    @Test
    void testAppliedRemedialActionOnOneState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), pstRangeAction, 3.1);

        // check object
        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(1, appliedRemedialActions.getStatesWithRa(network).size());
        assertEquals("Contingency FR1 FR3", appliedRemedialActions.getStatesWithRa(network).iterator().next().getContingency().orElseThrow().getId());

        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-3);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), network);

        assertFalse(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(8, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(3.1, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-1);
    }

    @Test
    void testAppliedRemedialActionOnTwoStates() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE)), pstRangeAction, 3.2);

        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(2, appliedRemedialActions.getStatesWithRa(network).size());

        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), network);

        assertFalse(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
    }

    @Test
    void testEmptyAppliedRemedialActions() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

        assertTrue(appliedRemedialActions.isEmpty(network));
        assertEquals(0, appliedRemedialActions.getStatesWithRa(network).size());
    }

    @Test
    void testAppliedRangeActionWithSetpointEqualToInitialNetwork() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE)), pstRangeAction, 0.0);
        // should not be taken into account, as PST setpoint is the same as in the initial network

        assertTrue(appliedRemedialActions.isEmpty(network));
        assertEquals(0, appliedRemedialActions.getStatesWithRa(network).size());
    }

    @Test
    void testAppliedRemedialActionOnPreventiveState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        State state = crac.getPreventiveState();
        assertThrows(FaraoException.class, () -> appliedRemedialActions.addAppliedNetworkAction(state, networkAction));
    }

    @Test
    void testCopy() {
        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE)), pstRangeAction, 3.2);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copy();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE))).size());
        assertEquals(1, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE))).size());

        // reset the original one
        originalAra = new AppliedRemedialActions();
        assertTrue(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
    }

    @Test
    void testCopyNetworkActionsAndAutomaticRangeActions() {
        // Add cnecs to create auto states
        crac.newFlowCnec()
            .withId("autoCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(crac.getInstant(Instant.Kind.AUTO))
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .add();
        crac.newFlowCnec()
            .withId("autoCnec2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(crac.getInstant(Instant.Kind.AUTO))
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .add();

        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE)), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE)), pstRangeAction, 3.2);
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.AUTO)), autoNetworkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.AUTO)), pstRangeAction, 2.3);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copyNetworkActionsAndAutomaticRangeActions();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.CURATIVE))).size());
        assertEquals(0, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.CURATIVE))).size());
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", crac.getInstant(Instant.Kind.AUTO))).size());
        assertEquals(1, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", crac.getInstant(Instant.Kind.AUTO))).size());

        // reset the original one
        originalAra = new AppliedRemedialActions();
        assertTrue(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
    }
}
