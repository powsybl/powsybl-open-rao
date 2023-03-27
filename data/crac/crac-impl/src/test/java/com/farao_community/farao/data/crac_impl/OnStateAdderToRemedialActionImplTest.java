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
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnStateAdderToRemedialActionImplTest {

    private Crac crac;
    private Contingency contingency;
    private NetworkAction remedialAction = null;

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
    public void testOk() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, Instant.CURATIVE)).withUsageMethod(UsageMethod.FORCED).add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnState);
        assertEquals(Instant.CURATIVE, ((OnState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(contingency, ((OnState) remedialAction.getUsageRules().get(0)).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    public void testOkPreventive() {
        remedialAction.newOnStateUsageRule().withState(crac.getPreventiveState()).withUsageMethod(UsageMethod.FORCED).add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnState);
        assertEquals(Instant.PREVENTIVE, ((OnState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    public void testNoState() {
        assertThrows(FaraoException.class, () -> remedialAction.newOnStateUsageRule().withUsageMethod(UsageMethod.FORCED).add());
    }

    @Test
    public void testNoUsageMethod() {
        assertThrows(FaraoException.class, () -> remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, Instant.CURATIVE)).add());
    }

    @Test
    public void testPreventiveInstantNotForced() {
        assertThrows(FaraoException.class, () ->
            remedialAction.newOnStateUsageRule()
                .withState(crac.getPreventiveState())
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add());
    }

    @Test
    public void testOutageInstant() {
        assertThrows(FaraoException.class, () -> {
            State outageState = ((CracImpl) crac).addState(contingency, Instant.OUTAGE);
            remedialAction.newOnStateUsageRule()
                .withState(outageState)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add();
        });
    }
}
