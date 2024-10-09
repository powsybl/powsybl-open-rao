/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.DanglingLineAction;
import com.powsybl.action.DanglingLineActionBuilder;
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
class DanglingLineActionImplTest {
    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction injectionSetpoint = crac.newNetworkAction()
            .withId("injectionSetpoint")
            .newDanglingLineAction()
                .withNetworkElement("element")
                .withActivePowerValue(10.0)
                .add()
            .add();
        assertEquals(1, injectionSetpoint.getNetworkElements().size());
        assertEquals("element", injectionSetpoint.getNetworkElements().iterator().next().getId());
    }

    @Test
    void hasImpactOnNetwork() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetwork() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(0)
            .add()
            .add();
        assertFalse(danglingLineSetpoint.hasImpactOnNetwork(network));

    }

    @Test
    void apply() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(100)
            .add()
            .add();
        assertEquals(0., network.getDanglingLine("DL1").getP0(), 1e-3);
        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    void canBeApplied() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newDanglingLineAction()
            .withNetworkElement("DL1")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(danglingLineSetpoint.canBeApplied(network)); // for now always true
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newDanglingLineAction()
            .withNetworkElement("wrong_name")
            .withActivePowerValue(100)
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
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(1, dummy.getElementaryActions().size());

        NetworkAction dummy2 = crac.newNetworkAction()
            .withId("dummy2")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(1, dummy2.getElementaryActions().size());

        NetworkAction dummy3 = crac.newNetworkAction()
            .withId("dummy3")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(10.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(2, dummy3.getElementaryActions().size());

        NetworkAction dummy4 = crac.newNetworkAction()
            .withId("dummy4")
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  4")
            .withActivePowerValue(10.)
            .add()
            .newDanglingLineAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  5")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(2, dummy4.getElementaryActions().size());

        DanglingLineAction danglingLineAction = new DanglingLineActionBuilder().withId("id").withDanglingLineId("DL1").withActivePowerValue(10).withRelativeValue(false).build();
        DanglingLineAction sameDanglingLineAction = new DanglingLineActionBuilder().withId("id").withDanglingLineId("DL1").withActivePowerValue(10).withRelativeValue(false).build();
        assertEquals(danglingLineAction, sameDanglingLineAction);
        NetworkAction dummy5 = new NetworkActionImpl("id", "name", "operator", null,
            new HashSet<>(List.of(danglingLineAction, sameDanglingLineAction)), 0, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }
}
