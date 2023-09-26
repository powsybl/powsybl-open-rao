/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyState;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyStateAdderToRemedialAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnContingencyStateAdderToRemedialActionImplTest {

    private Crac crac;
    private Contingency contingency;
    private RemedialAction<?> remedialAction = null;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        ((CracImpl) crac).addPreventiveState();

        contingency = crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("networkElementId")
            .add();

        ((CracImpl) crac).addState(contingency, Instant.CURATIVE);

        remedialAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
    }

    @Test
    void testOk() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, Instant.CURATIVE)).withUsageMethod(UsageMethod.FORCED).add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnContingencyState);
        assertEquals(Instant.CURATIVE, ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(contingency, ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    void testOkPreventive() {
        remedialAction.newOnStateUsageRule().withState(crac.getPreventiveState()).withUsageMethod(UsageMethod.FORCED).add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnContingencyState);
        assertEquals(Instant.PREVENTIVE, ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    void testNoState() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withUsageMethod(UsageMethod.FORCED);
        assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getState(contingency, Instant.CURATIVE));
        assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
    }

    @Test
    void testPreventiveInstantNotForced() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getPreventiveState())
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
    }

    @Test
    void testOutageInstant() {
        State outageState = ((CracImpl) crac).addState(contingency, Instant.OUTAGE);
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(outageState)
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
    }
}
