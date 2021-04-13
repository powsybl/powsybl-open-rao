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
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.CracImplFactory;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkActionAdderImplTest {

    private Crac crac;

    @Before
    public void setUp() {
        crac = new CracImplFactory().create("cracId");

        crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("coNetworkElementId")
            .add();
    }

    @Test
    public void testOk() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals(1, networkAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    public void testOkWithTwoElementaryActions() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .newPstSetPoint()
                .withNetworkElement("anotherPstNetworkElementId")
                .withTapConvention(TapConvention.STARTS_AT_ONE)
                .withSetpoint(4)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(2, networkAction.getElementaryActions().size());
        assertEquals(0, networkAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    public void testOkWithTwoUsageRules() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .newFreeToUseUsageRule()
                .withInstant(Instant.PREVENTIVE)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newOnStateUsageRule()
                .withInstant(Instant.CURATIVE)
                .withContingency("contingencyId")
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals(2, networkAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    public void testOkWithoutName() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionId", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    public void testOkWithoutOperator() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertNull(networkAction.getOperator());
        assertEquals(1, networkAction.getElementaryActions().size());
    }

    @Test (expected = FaraoException.class)
    public void testNokWithoutId() {
        crac.newNetworkAction()
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
                .withNetworkElement("pstNetworkElementId")
                .withTapConvention(TapConvention.CENTERED_ON_ZERO)
                .withSetpoint(6)
                .add()
            .add();
    }
}
