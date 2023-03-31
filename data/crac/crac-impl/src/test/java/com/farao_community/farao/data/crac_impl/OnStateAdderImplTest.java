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
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.OnStateAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class OnStateAdderImplTest {

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
        RemedialAction<?> remedialAction = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnState);
        assertEquals(Instant.CURATIVE, ((OnState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(contingency, ((OnState) remedialAction.getUsageRules().get(0)).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().get(0).getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", Instant.CURATIVE));
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnState);
        assertEquals(Instant.PREVENTIVE, ((OnState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdder::add);
    }

    @Test
    void testNoContingency() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdder::add);
    }

    @Test
    void testNoUsageMethod() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId");
        assertThrows(FaraoException.class, onStateAdder::add);
    }

    @Test
    void testUnknownContingency() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdder::add);
    }

    @Test
    void testPreventiveInstant() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdder::add);
    }

    @Test
    void testOutageInstant() {
        OnStateAdder<NetworkActionAdder> onStateAdder = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onStateAdder::add);
    }
}
