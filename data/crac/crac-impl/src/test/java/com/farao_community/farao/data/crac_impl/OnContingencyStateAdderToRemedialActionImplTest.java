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

    private Crac crac;
    private Contingency contingency;
    private RemedialAction<?> remedialAction = null;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        crac.newInstant("curative", InstantKind.CURATIVE, "auto");
        ((CracImpl) crac).addPreventiveState("preventive");

        contingency = crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("networkElementId")
            .add();

        ((CracImpl) crac).addState(contingency, "curative");

        remedialAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add()
            .add();
    }

    @Test
    void testOk() {
        remedialAction.newOnStateUsageRule().withState(crac.getState(contingency, "curative")).withUsageMethod(UsageMethod.FORCED).add();

        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();
        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals("curative", ((OnContingencyState) usageRule).getState().getInstant().getId());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testOkPreventive() {
        remedialAction.newOnStateUsageRule().withState(crac.getPreventiveState()).withUsageMethod(UsageMethod.FORCED).add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals("preventive", ((OnContingencyState) usageRule).getState().getInstant().getId());
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testNoState() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withUsageMethod(UsageMethod.FORCED);
        FaraoException exception = assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
        assertEquals("Cannot add OnContingencyState without a state. Please use withState() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getState(contingency, "curative"));
        FaraoException exception = assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
        assertEquals("Cannot add OnContingencyState without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testPreventiveInstantNotForced() {
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(crac.getPreventiveState())
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
        assertEquals("OnContingencyState usage rules are not allowed for PREVENTIVE instant except when FORCED. Please use newOnInstantUsageRule() instead.", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        State outageState = ((CracImpl) crac).addState(contingency, "outage");
        OnContingencyStateAdderToRemedialAction<?> onStateAdderToRemedialAction = remedialAction.newOnStateUsageRule()
            .withState(outageState)
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onStateAdderToRemedialAction::add);
        assertEquals("OnContingencyState usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
