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
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FreeToUseAdderImplTest {

    private Crac crac;
    private NetworkActionAdder remedialActionAdder;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.newContingency()
                .withId("contingencyId")
                .withNetworkElement("networkElementId")
                .add();
        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOkPreventive() {
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
    public void testOkCurative() {
        RemedialAction remedialAction = remedialActionAdder.newFreeToUseUsageRule()
                .withInstant(Instant.CURATIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof FreeToUse);
        assertEquals(Instant.CURATIVE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getInstant());
        assertEquals(UsageMethod.AVAILABLE, ((FreeToUse) remedialAction.getUsageRules().get(0)).getUsageMethod());
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", Instant.CURATIVE));
    }

    @Test (expected = FaraoException.class)
    public void testNoInstant() {
        remedialActionAdder.newFreeToUseUsageRule()
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoUsageMethod() {
        remedialActionAdder.newFreeToUseUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testOutageInstant() {
        remedialActionAdder.newFreeToUseUsageRule()
            .withInstant(Instant.OUTAGE)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }
}
