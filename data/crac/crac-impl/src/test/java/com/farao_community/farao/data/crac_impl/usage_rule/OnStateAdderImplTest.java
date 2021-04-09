/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.usage_rule;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.SimpleCracFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnStateAdderImplTest {

    private Contingency contingency;
    private NetworkActionAdder remedialActionAdder;

    @Before
    public void setUp() {
        Crac crac = new SimpleCracFactory().create("cracId");

        contingency = crac.newContingency()
            .withId("contingencyId")
            .newNetworkElement().withId("networkElementId").add()
            .add();

        remedialActionAdder = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {
        RemedialAction remedialAction = remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .add();

        assertEquals(1, remedialAction.getUsageRules().size());
        assertTrue(remedialAction.getUsageRules().get(0) instanceof OnState);
        assertEquals(Instant.CURATIVE, ((OnState) remedialAction.getUsageRules().get(0)).getState().getInstant());
        assertEquals(contingency, ((OnState) remedialAction.getUsageRules().get(0)).getState().getContingency().orElse(null));
        assertEquals(UsageMethod.AVAILABLE, ((OnState) remedialAction.getUsageRules().get(0)).getUsageMethod());
    }

    @Test (expected = FaraoException.class)
    public void testNoInstant() {
        remedialActionAdder.newOnStateUsageRule()
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoContingency() {
        remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoUsageMethod() {
        remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingencyId")
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testUnknownContingency() {
        remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.CURATIVE)
            .withContingency("unknownContingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testPreventiveInstant() {
        remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.PREVENTIVE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testOutageInstant() {
        remedialActionAdder.newOnStateUsageRule()
            .withInstant(Instant.OUTAGE)
            .withContingency("contingencyId")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add();
    }
}
