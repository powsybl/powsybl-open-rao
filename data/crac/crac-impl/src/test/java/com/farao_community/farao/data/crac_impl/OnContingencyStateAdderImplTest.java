/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.RemedialAction;
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
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        crac.newInstant("curative", InstantKind.CURATIVE, "auto");

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
            .withInstant("curative")
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals("curative", ((OnContingencyState) usageRule).getState().getInstant().getId());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", "curative"));
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("preventive")
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals("preventive", ((OnContingencyState) usageRule).getState().getInstant().getId());
        assertEquals(UsageMethod.FORCED, usageRule.getUsageMethod());
    }

    @Test
    void testNoInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Cannot add OnContingencyState without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("curative")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Cannot add OnContingencyState without a contingency. Please use withContingency() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("curative")
            .withContingency("contingencyId");
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Cannot add OnContingencyState without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testUnknownContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("curative")
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Contingency unknownContingencyId of OnContingencyState usage rule does not exist in the crac. Use crac.newContingency() first.", exception.getMessage());
    }

    @Test
    void testPreventiveInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("preventive")
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("OnContingencyState usage rules are not allowed for PREVENTIVE instant, except when FORCED. Please use newOnInstantUsageRule() instead.", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant("outage")
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("OnContingencyState usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
