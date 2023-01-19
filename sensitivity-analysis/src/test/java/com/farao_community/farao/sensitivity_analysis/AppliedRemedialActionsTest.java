/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_creation.util.iidm.IidmPstHelper;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class AppliedRemedialActionsTest {

    private Network network;
    private Crac crac;
    private NetworkAction networkAction;
    private PstRangeAction pstRangeAction;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithCurativePstRange();
        pstRangeAction = crac.getPstRangeAction("pst");
        networkAction = crac.newNetworkAction()
                .withId("na-id")
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("BBE2AA1  FFR3AA1  1").add()
                .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.CURATIVE).add()
                .add();
    }

    @Test
    public void testAppliedRemedialActionOnOneState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), pstRangeAction, 3.1);

        // check object
        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(1, appliedRemedialActions.getStatesWithRa(network).size());
        assertEquals("Contingency FR1 FR3", appliedRemedialActions.getStatesWithRa(network).iterator().next().getContingency().orElseThrow().getId());
        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-3);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), network);

        assertFalse(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(8, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
        assertEquals(3.1, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getCurrentStep().getAlpha(), 1e-1);
    }

    @Test
    public void testAppliedRemedialActionOnTwoStates() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), networkAction);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", Instant.CURATIVE), pstRangeAction, 3.2);

        assertFalse(appliedRemedialActions.isEmpty(network));
        assertEquals(2, appliedRemedialActions.getStatesWithRa(network).size());

        // apply remedial actions on network
        assertTrue(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);

        appliedRemedialActions.applyOnNetwork(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), network);

        assertFalse(network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().isConnected());
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition(), 0);
    }

    @Test
    public void testEmptyAppliedRemedialActions() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

        assertTrue(appliedRemedialActions.isEmpty(network));
        assertEquals(0, appliedRemedialActions.getStatesWithRa(network).size());

    }

    @Test
    public void testAppliedRangeActionWithSetpointEqualToInitialNetwork() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", Instant.CURATIVE), pstRangeAction, 0.0);
        // should not be taken into account, as PST setpoint is the same as in the initial network

        assertTrue(appliedRemedialActions.isEmpty(network));
        assertEquals(0, appliedRemedialActions.getStatesWithRa(network).size());

    }

    @Test (expected = FaraoException.class)
    public void testAppliedRemedialActionOnPreventiveState() {
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedNetworkAction(crac.getPreventiveState(), networkAction);
    }

    @Test
    public void testCopy() {
        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", Instant.CURATIVE), pstRangeAction, 3.2);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copy();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", Instant.CURATIVE)).size());
        assertEquals(1, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", Instant.CURATIVE)).size());

        // reset the original one
        originalAra = new AppliedRemedialActions();
        assertTrue(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
    }

    @Test
    public void testCopyNetworkActions() {
        AppliedRemedialActions originalAra = new AppliedRemedialActions();
        originalAra.addAppliedNetworkAction(crac.getState("Contingency FR1 FR3", Instant.CURATIVE), networkAction);
        originalAra.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", Instant.CURATIVE), pstRangeAction, 3.2);

        // make a copy
        AppliedRemedialActions copyAra = originalAra.copyNetworkActions();
        assertFalse(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
        assertEquals(1, copyAra.getAppliedNetworkActions(crac.getState("Contingency FR1 FR3", Instant.CURATIVE)).size());
        assertEquals(0, copyAra.getAppliedRangeActions(crac.getState("Contingency FR1 FR2", Instant.CURATIVE)).size());

        // reset the original one
        originalAra = new AppliedRemedialActions();
        assertTrue(originalAra.isEmpty(network));
        assertFalse(copyAra.isEmpty(network));
    }

    @Test
    public void testGetAppliedRemedialActionsAtAGivenInstant() {
        // Setup specific to this test to verify that method only takes RAs from a given instant and no other
        // Creates a fake_pst with an AUTO instant because addAppliedRemedialAction method only allows instant Curative or Auto
        IidmPstHelper pstHelper = new IidmPstHelper("BBE2AA1  BBE3AA1  1", network);
        crac.newPstRangeAction()
                .withId("fake_pst")
                .withNetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1 name")
                .withOperator("operator1")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newOnStateUsageRule().withInstant(Instant.AUTO).withContingency("Contingency FR1 FR3").withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange()
                .withRangeType(RangeType.ABSOLUTE)
                .withMinTap(-16)
                .withMaxTap(16)
                .add()
                .withInitialTap(pstHelper.getInitialTap())
                .withTapToAngleConversionMap(pstHelper.getTapToAngleConversionMap())
                .withSpeed(1)
                .add();

        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR2", Instant.CURATIVE), pstRangeAction, 3.2);
        appliedRemedialActions.addAppliedRangeAction(crac.getState("Contingency FR1 FR3", Instant.AUTO), pstRangeAction, 3.2);

        assertEquals(2, appliedRemedialActions.getStatesWithRa(network).size());
        assertEquals(1, appliedRemedialActions.getAppliedRemedialActionsAtAGivenInstant(Instant.AUTO).getStatesWithRa(network).size());
        assertEquals(1, appliedRemedialActions.getAppliedRemedialActionsAtAGivenInstant(Instant.CURATIVE).getStatesWithRa(network).size());
        assertThrows(FaraoException.class, () -> appliedRemedialActions.getAppliedRemedialActionsAtAGivenInstant(Instant.PREVENTIVE));
        assertThrows(FaraoException.class, () -> appliedRemedialActions.getAppliedRemedialActionsAtAGivenInstant(Instant.OUTAGE));
    }
}
