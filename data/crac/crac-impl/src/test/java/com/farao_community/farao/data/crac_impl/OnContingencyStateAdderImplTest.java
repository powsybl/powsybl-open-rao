/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyState;
import com.farao_community.farao.data.crac_api.usage_rule.OnContingencyStateAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class OnContingencyStateAdderImplTest {

    private Crac crac;
    private Contingency contingency;
    private NetworkActionAdder remedialActionAdder;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");

        contingency = crac.newContingency()
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
    void testOk() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(Instant.CURATIVE, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", Instant.CURATIVE));
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(Instant.PREVENTIVE, ((OnContingencyState) usageRule).getState().getInstant());
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testNoContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId");
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testUnknownContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testPreventiveInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testOutageInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }
}
