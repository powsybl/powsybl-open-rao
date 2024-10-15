/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.ShuntCompensatorPositionActionBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
class ShuntCompensatorPositionActionImplTest {
    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction injectionSetpoint = crac.newNetworkAction()
            .withId("injectionSetpoint")
            .newShuntCompensatorPositionAction()
                .withNetworkElement("element")
                .withSectionCount(10)
                .add()
            .add();
        assertEquals(1, injectionSetpoint.getNetworkElements().size());
        assertEquals("element", injectionSetpoint.getNetworkElements().iterator().next().getId());
    }

    @Test
    void hasImpactOnNetwork() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(0)
            .add()
            .add();
        assertTrue(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetwork() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(1)
            .add()
            .add();
        assertFalse(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void apply() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(2)
            .add()
            .add();
        assertEquals(1., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
        shuntCompensatorSetpoint.apply(network);
        assertEquals(2., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
    }

    @Test
    void canNotBeApplied() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(3)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertFalse(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 3
    }

    @Test
    void canBeApplied() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(1)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 1
    }

    @Test
    void canMaxBeApplied() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("SC1")
            .withSectionCount(2)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 2
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("wrong_name")
            .withSectionCount(3)
            .add()
            .add();
        assertFalse(dummy.canBeApplied(network));
        assertFalse(dummy.hasImpactOnNetwork(network));
    }

    @Test
    void equals() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withSectionCount(10)
            .add()
            .add();
        assertEquals(1, dummy.getElementaryActions().size());

        NetworkAction dummy2 = crac.newNetworkAction()
            .withId("dummy2")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withSectionCount(12)
            .add()
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withSectionCount(12)
            .add()
            .add();
        assertEquals(1, dummy2.getElementaryActions().size());

        NetworkAction dummy3 = crac.newNetworkAction()
            .withId("dummy3")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withSectionCount(10)
            .add()
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withSectionCount(12)
            .add()
            .add();
        assertEquals(2, dummy3.getElementaryActions().size());

        NetworkAction dummy4 = crac.newNetworkAction()
            .withId("dummy4")
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  4")
            .withSectionCount(10)
            .add()
            .newShuntCompensatorPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  5")
            .withSectionCount(10)
            .add()
            .add();
        assertEquals(2, dummy4.getElementaryActions().size());

        ShuntCompensatorPositionAction shuntCompensatorPositionAction = new ShuntCompensatorPositionActionBuilder().withId("id").withShuntCompensatorId("SC1").withSectionCount(10).build();
        ShuntCompensatorPositionAction sameShuntCompensatorPositionAction = new ShuntCompensatorPositionActionBuilder().withId("id").withShuntCompensatorId("SC1").withSectionCount(10).build();
        assertEquals(shuntCompensatorPositionAction, sameShuntCompensatorPositionAction);
        NetworkAction dummy5 = new NetworkActionImpl("id", "name", "operator", null,
            new HashSet<>(List.of(shuntCompensatorPositionAction, sameShuntCompensatorPositionAction)), 0, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }
}
