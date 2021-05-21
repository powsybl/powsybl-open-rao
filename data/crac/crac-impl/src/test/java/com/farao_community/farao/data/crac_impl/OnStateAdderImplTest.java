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
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.OnState;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class OnStateAdderImplTest {

    private Crac crac;
    private Contingency contingency;
    private NetworkActionAdder remedialActionAdder;

    @Before
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
        assertEquals(1, crac.getStates().size());
        assertNotNull(crac.getState("contingencyId", Instant.CURATIVE));
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
