/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnStateAdderToRemedialActionImplTest {

    private Crac crac;
    private Contingency contingency;
    private NetworkAction remedialAction = null;

    @Before
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

    @Test (expected = FaraoException.class)
    public void testNoState() {
        remedialAction.newOnStateUsageRule().withUsageMethod(UsageMethod.FORCED).add();
    }

    @Test (expected = FaraoException.class)
    public void testNoUsageMethod() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, Instant.CURATIVE)).add();
    }

    @Test (expected = FaraoException.class)
    public void testPreventiveInstantNotForced() {
        remedialAction.newOnStateUsageRule()
            .withState(crac.getPreventiveState())
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testOutageInstant() {
        State outageState = ((CracImpl) crac).addState(contingency, Instant.OUTAGE);
        remedialAction.newOnStateUsageRule()
            .withState(outageState)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }
}
