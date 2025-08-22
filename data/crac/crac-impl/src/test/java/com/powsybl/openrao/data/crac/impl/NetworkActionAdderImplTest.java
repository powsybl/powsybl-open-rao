/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NetworkActionAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("cracId")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);

        crac.newContingency()
            .withId("contingencyId")
            .withContingencyElement("coNetworkElementId", ContingencyElementType.LINE)
            .add();
    }

    @Test
    void testOk() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .withActivationCost(50d)
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
                .add()
            .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(Optional.of(50d), networkAction.getActivationCost());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals(1, networkAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    void testOkWithTwoElementaryActions() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
                .add()
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("anotherPstNetworkElementId")
                .withTapPosition(4)
                .add()
            .add();

        assertEquals("networkActionId", networkAction.getId());
        assertEquals("networkActionName", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertTrue(networkAction.getActivationCost().isEmpty());
        assertEquals(2, networkAction.getElementaryActions().size());
        assertEquals(0, networkAction.getUsageRules().size());
        assertEquals(1, crac.getNetworkActions().size());
    }

    @Test
    void testOkWithTwoUsageRules() {
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .withOperator("operator")
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
                .add()
            .newOnInstantUsageRule()
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
            .newOnContingencyStateUsageRule()
                .withInstant(CURATIVE_INSTANT_ID)
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
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withOperator("operator")
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
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
        NetworkAction networkAction = crac.newNetworkAction()
            .withId("networkActionId")
            .withName("networkActionName")
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
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
                .newPhaseTapChangerTapPositionAction()
                    .withNetworkElement("pstNetworkElementId")
                    .withTapPosition(6)
                    .add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, networkActionAdder::add);
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, networkActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", exception.getMessage());
    }

    @Test
    void testNokWithoutElementaryAction() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction()
            .withId("networkActionName")
            .withName("networkActionName")
            .withOperator("operator");
        OpenRaoException exception = assertThrows(OpenRaoException.class, networkActionAdder::add);
        assertEquals("NetworkAction networkActionName has to have at least one ElementaryAction.", exception.getMessage());
    }

    @Test
    void testOkWithoutSpeed() {
        NetworkAction networkAction = crac.newNetworkAction()
                .withId("networkActionId")
                .withName("networkActionName")
                .withOperator("operator")
                .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
                .add()
                .add();

        assertEquals(Optional.empty(), networkAction.getSpeed());
    }

    @Test
    void testOkWithSpeed() {
        NetworkAction networkAction = crac.newNetworkAction()
                .withId("networkActionId")
                .withName("networkActionName")
                .withOperator("operator")
                .withSpeed(123)
                .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("pstNetworkElementId")
                .withTapPosition(6)
                .add()
                .add();

        assertEquals(123, networkAction.getSpeed().get().intValue());
    }

}
