/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
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
            .withInstant(Instant.PREVENTIVE)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals(Instant.PREVENTIVE, usageRule.getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newOnInstantUsageRule()
                .withInstant(Instant.CURATIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        UsageRule usageRule = (UsageRule) remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnInstant);
        assertEquals(Instant.CURATIVE, usageRule.getInstant());
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onInstantAdder::add);
    }

    @Test
    void testNoUsageMethod() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant(Instant.PREVENTIVE);
        assertThrows(FaraoException.class, onInstantAdder::add);
    }

    @Test
    void testOutageInstant() {
        OnInstantAdder<NetworkActionAdder> onInstantAdder = remedialActionAdder.newOnInstantUsageRule()
            .withInstant(Instant.OUTAGE)
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onInstantAdder::add);
    }
}
