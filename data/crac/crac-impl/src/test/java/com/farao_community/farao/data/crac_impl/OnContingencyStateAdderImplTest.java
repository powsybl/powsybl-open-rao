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
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnContingencyState);
        assertEquals(crac.getInstant(Instant.Kind.CURATIVE), ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(contingency, ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, remedialAction.getUsageRules().get(0).getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", crac.getInstant(Instant.Kind.CURATIVE)));
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnContingencyState);
        assertEquals(crac.getInstant(Instant.Kind.PREVENTIVE), ((OnContingencyState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(UsageMethod.FORCED, remedialAction.getUsageRules().get(0).getUsageMethod());
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
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withContingency("contingencyId");
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testUnknownContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.CURATIVE))
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testPreventiveInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.PREVENTIVE))
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }

    @Test
    void testOutageInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(crac.getInstant(Instant.Kind.OUTAGE))
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        assertThrows(FaraoException.class, onContingencyStateAdder::add);
    }
}
