/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class AppliedRemedialActionsTest {
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Network network;
    private Crac crac;
    private NetworkAction networkAction;
    private NetworkAction autoNetworkAction;
    private PstRangeAction pstRangeAction;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithCurativePstRange();
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
        pstRangeAction = crac.getPstRangeAction("pst");
        networkAction = crac.newNetworkAction()
                .withId("na-id")
                .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
                .newOnInstantUsageRule().withInstant(CURATIVE_INSTANT_ID).add()
                .add();
        autoNetworkAction = crac.newNetworkAction()
            .withId("na-auto-id")
            .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
            .newOnInstantUsageRule().withInstant(AUTO_INSTANT_ID).add()
            .add();
    }

    @Test
    void testAppliedRemedialActionOnOneState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", curativeInstant), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", curativeInstant), pstRangeAction, 3.1);

        // check object
        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(1, appliedRemedialActions.getStatesWithRa(network).size());
        assertEquals("Contingency FR1 FR3", appliedRemedialActions.getStatesWithRa(network).iterator().next().getContingency().orElseThrow().getId());

        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-3);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", curativeInstant), network);

        assertFalse(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(8, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(3.1, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-1);
    }

    @Test
    void testAppliedRemedialActionOnTwoStates() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", curativeInstant), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", curativeInstant), pstRangeAction, 3.2);

        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(2, appliedRemedialActions.getStatesWithRa(network).size());

        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", curativeInstant), network);

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
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", curativeInstant), pstRangeAction, 0.0);
        // should not be taken into account, as PST setpoint is the same as in the initial network

        assertTrue(appliedRemedialActions.isEmpty(network));
        assertEquals(0, appliedRemedialActions.getStatesWithRa(network).size());
    }

    @Test
    void testAppliedRemedialActionOnPreventiveState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        State state = crac.getPreventiveState();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> appliedRemedialActions.addAppliedNetworkAction(state, networkAction));
        assertEquals("Sensitivity analysis with applied remedial actions only work with CURATIVE and AUTO remedial actions.", exception.getMessage());
    }

    @Test
    void testCopy() {
        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", curativeInstant), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", curativeInstant), pstRangeAction, 3.2);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copy();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", curativeInstant)).size());
        assertEquals(1, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", curativeInstant)).size());

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
            .withInstant(AUTO_INSTANT_ID)
            .withContingency("Contingency FR1 FR3")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .add();
        crac.newFlowCnec()
            .withId("autoCnec2")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(AUTO_INSTANT_ID)
            .withContingency("Contingency FR1 FR2")
            .withOptimized(true)
            .withOperator("operator1")
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(TwoSides.ONE)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .add();

        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", curativeInstant), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", curativeInstant), pstRangeAction, 3.2);
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", autoInstant), autoNetworkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", autoInstant), pstRangeAction, 2.3);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copyNetworkActionsAndAutomaticRangeActions();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", curativeInstant)).size());
        assertEquals(0, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", curativeInstant)).size());
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", autoInstant)).size());
        assertEquals(1, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", autoInstant)).size());

        // reset the original one
        originalAra = new AppliedRemedialActions();
        assertTrue(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
    }

    @Test
    void testToActions() {
        NetworkAction switchPair = crac.newNetworkAction()
            .withId("switch-pair")
            .newSwitchPair().withSwitchToOpen("NNL3AA11 NNL3AA12 1").withSwitchToClose("NNL3AA13 NNL3AA14 1").add()
            .add();
        State state = crac.getState("Contingency FR1 FR3", curativeInstant);
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(state, networkAction);
        appliedRemedialActions.addAppliedNetworkAction(state, switchPair);
        appliedRemedialActions.addAppliedRangeAction(state, pstRangeAction, 3.2);

        List<Action> actions = appliedRemedialActions.toActions(state, network);
        assertEquals(4, actions.size());
        assertEquals(1, actions.stream().filter(TerminalsConnectionAction.class::isInstance).count());
        assertEquals(1, actions.stream().filter(PhaseTapChangerTapPositionAction.class::isInstance).count());
        // the switch pair is decomposed into its two switch actions
        List<SwitchAction> switchActions = actions.stream().filter(SwitchAction.class::isInstance).map(SwitchAction.class::cast).toList();
        assertEquals(2, switchActions.size());
        assertEquals(Set.of("NNL3AA11 NNL3AA12 1", "NNL3AA13 NNL3AA14 1"), switchActions.stream().map(SwitchAction::getSwitchId).collect(Collectors.toSet()));
        assertTrue(switchActions.stream().filter(a -> a.getSwitchId().equals("NNL3AA11 NNL3AA12 1")).allMatch(SwitchAction::isOpen));
        assertTrue(switchActions.stream().filter(a -> a.getSwitchId().equals("NNL3AA13 NNL3AA14 1")).noneMatch(SwitchAction::isOpen));

        assertTrue(appliedRemedialActions.toActions(crac.getState("Contingency FR1 FR2", curativeInstant), network).isEmpty());
    }
}
