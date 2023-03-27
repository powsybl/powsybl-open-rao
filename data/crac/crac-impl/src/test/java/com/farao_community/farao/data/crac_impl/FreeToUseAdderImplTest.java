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
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class FreeToUseAdderImplTest {

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
        RemedialAction remedialAction = remedialActionAdder.newFreeToUseUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof FreeToUse);
        assertEquals(Instant.PREVENTIVE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
    }

    @Test
    void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newFreeToUseUsageRule()
                .withInstant(Instant.CURATIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof FreeToUse);
        assertEquals(Instant.CURATIVE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getUsageMethod());
    }

    @Test
    void testNoInstant() {
        assertThrows(FaraoException.class, () ->
            remedialActionAdder.newFreeToUseUsageRule()
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add());
    }

    @Test
    void testNoUsageMethod() {
        assertThrows(FaraoException.class, () ->
            remedialActionAdder.newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .add());
    }

    @Test
    void testOutageInstant() {
        assertThrows(FaraoException.class, () ->
            remedialActionAdder.newFreeToUseUsageRule()
                .withInstant(Instant.OUTAGE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add());
    }
}
