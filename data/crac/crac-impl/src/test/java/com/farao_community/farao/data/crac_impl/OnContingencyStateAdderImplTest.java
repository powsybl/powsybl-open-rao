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
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;
    private Contingency contingency;
    private NetworkActionAdder remedialActionAdder;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);

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
            .withInstant(curativeInstant)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(CURATIVE_INSTANT_ID, ((OnContingencyState) usageRule).getState().getInstant().getId());
        assertEquals(contingency, ((OnContingencyState) usageRule).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, usageRule.getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", curativeInstant));
    }

    @Test
    void testOkPreventive() {
        RemedialAction<?> remedialAction = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(preventiveInstant)
            .withUsageMethod(UsageMethod.FORCED)
            .add()
            .add();
        UsageRule usageRule = remedialAction.getUsageRules().iterator().next();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(usageRule instanceof OnContingencyState);
        assertEquals(PREVENTIVE_INSTANT_ID, ((OnContingencyState) usageRule).getState().getInstant().getId());
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
            .withInstant(curativeInstant)
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Cannot add OnContingencyState without a contingency. Please use withContingency() with a non null value", exception.getMessage());
    }

    @Test
    void testNoUsageMethod() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(curativeInstant)
            .withContingency("contingencyId");
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Cannot add OnContingencyState without a usage method. Please use withUsageMethod() with a non null value", exception.getMessage());
    }

    @Test
    void testUnknownContingency() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(curativeInstant)
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("Contingency unknownContingencyId of OnContingencyState usage rule does not exist in the crac. Use crac.newContingency() first.", exception.getMessage());
    }

    @Test
    void testPreventiveInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(preventiveInstant)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("OnContingencyState usage rules are not allowed for PREVENTIVE instant, except when FORCED. Please use newOnInstantUsageRule() instead.", exception.getMessage());
    }

    @Test
    void testOutageInstant() {
        OnContingencyStateAdder<NetworkActionAdder> onContingencyStateAdder = remedialActionAdder.newOnContingencyStateUsageRule()
            .withInstant(outageInstant)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE);
        FaraoException exception = assertThrows(FaraoException.class, onContingencyStateAdder::add);
        assertEquals("OnContingencyState usage rules are not allowed for OUTAGE instant.", exception.getMessage());
    }
}
