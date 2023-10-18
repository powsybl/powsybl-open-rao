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
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class OnContingencyStateAdderToRemedialActionImplTest {

    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);
    private Crac crac;
    private Contingency contingency;
    private RemedialAction<?> remedialAction = null;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        ((CracImpl) crac).addPreventiveState(INSTANT_PREV);

        contingency = crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("networkElementId")
            .add();

        ((CracImpl) crac).addState(contingency, INSTANT_CURATIVE);

        remedialAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
    }

    @Test
    void testOk() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, INSTANT_CURATIVE)).withUsageMethod(UsageMethod.FORCED).add();

        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();
        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(INSTANT_CURATIVE, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testOkPreventive() {
        remedialAction.newOnStateUsageRule().withState(crac.getPreventiveState()).withUsageMethod(UsageMethod.FORCED).add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(INSTANT_PREV, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
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
            .withState(crac.getState(contingency, INSTANT_CURATIVE));
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
        State outageState = ((CracImpl) crac).addState(contingency, INSTANT_OUTAGE);
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(outageState)
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
    }
}
