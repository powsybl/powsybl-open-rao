/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionAdderImplTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        crac.newInstant("outage", InstantKind.OUTAGE, "preventive");
        crac.newInstant("auto", InstantKind.AUTO, "outage");
        crac.newInstant("curative", InstantKind.CURATIVE, "auto");

        crac.newContingency()
            .withId("contingencyId")
            .withNetworkElement("coNetworkElementId")
            .add();
    }

    @Test
    void testOk() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
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
    void testOkWithTwoElementaryActions() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .newPstSetPoint()
            .withNetworkElement("anotherPstNetworkElementId")
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
    void testOkWithTwoUsageRules() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .newOnInstantUsageRule()
            .withInstantId("preventive")
            .withUsageMethod(UsageMethod.AVAILABLE)
            .add()
            .newOnContingencyStateUsageRule()
            .withInstantId("curative")
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
    void testOkWithoutName() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
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
    void testOkWithoutOperator() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertNull(networkAction.getOperator());
        assertEquals(1, networkAction.getElementaryActions().size());
    }

    @Test
    void testNokWithoutId() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add();
        FaraoException exception = assertThrows(FaraoException.class, networkActionAdder::add);
        assertEquals("Cannot add a NetworkAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newPstRangeAction()
            .withId("sameId")
            .withOperator("BE")
            .withNetworkElement("networkElementId")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-2, -20., -1, -10., 0, 0., 1, 10., 2, 20.))
            .add();
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId("sameId")
            .withOperator("BE");
        FaraoException exception = assertThrows(FaraoException.class, networkActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", exception.getMessage());
    }

    @Test
    void testNokWithoutElementaryAction() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId("networkActionName")
            .withName("networkActionName")
            .withOperator("operator");
        FaraoException exception = assertThrows(FaraoException.class, networkActionAdder::add);
        assertEquals("NetworkAction networkActionName has to have at least one ElementaryAction.", exception.getMessage());
    }

    @Test
    void testOkWithoutSpeed() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .add();

        assertEquals(Optional.empty(), networkAction.getSpeed());
    }

    @Test
    void testOkWithSpeed() {
        NetworkAction networkAction = (NetworkAction) crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .withSpeed(123)
            .newPstSetPoint()
            .withNetworkElement("pstNetworkElementId")
            .withSetpoint(6)
            .add()
            .add();

        assertEquals(123, networkAction.getSpeed().orElseThrow().intValue());
    }

}
