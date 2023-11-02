/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnInstant;
import com.farao_community.farao.data.crac_api.usage_rule.OnInstantAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class OnInstantAdderImplTest {
    private Crac crac;
    private NetworkActionAdder remedialActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        crac.newInstant("curative", InstantKind.CURATIVE, "auto");
        crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("networkElementId")
            .add();
        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("action-elementId").add();
    }

    @Test
    void testOkPreventive() {
        RemedialAction remedialAction = remedialActionAdder.newOnInstantUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals("preventive", usageRule.getInstant().getId());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnInstantUsageRule()
            .withInstant("curative")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals("curative", usageRule.getInstant().getId());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onInstantAdder::add);
        assertEquals("Cannot add OnInstant without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant("preventive");
        FaraoException exception = assertThrows(FaraoException.class, onInstantAdder::add);
        assertEquals("Cannot add OnInstant without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant("outage")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onInstantAdder::add);
        assertEquals("OnInstant usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
