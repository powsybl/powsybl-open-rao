/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.GeneratorAction;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation.createCracWithRemedialActions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
class GeneratorActionImplTest {
    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction injectionSetpoint = crac.newNetworkAction()
            .withId("injectionSetpoint")
            .newGeneratorAction()
                .withNetworkElement("element")
                .withActivePowerValue(10.0)
                .add()
            .add();
        assertEquals(1, injectionSetpoint.getNetworkElements().size());
        assertEquals("element", injectionSetpoint.getNetworkElements().iterator().next().getId());
    }

    @Test
    void hasImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(2000)
            .add()
            .add();
        assertFalse(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(100.0)
            .add()
            .add();
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    void canBeApplied() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newGeneratorAction()
            .withNetworkElement("FFR1AA1 _generator")
            .withActivePowerValue(100)
            .add()
            .add();
        assertTrue(generatorSetpoint.canBeApplied(network)); // for now always true
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newGeneratorAction()
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
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(1, dummy.getElementaryActions().size());

        NetworkAction dummy2 = crac.newNetworkAction()
            .withId("dummy2")
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(1, dummy2.getElementaryActions().size());

        NetworkAction dummy3 = crac.newNetworkAction()
            .withId("dummy3")
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(10.)
            .add()
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withActivePowerValue(12.)
            .add()
            .add();
        assertEquals(2, dummy3.getElementaryActions().size());

        NetworkAction dummy4 = crac.newNetworkAction()
            .withId("dummy4")
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  4")
            .withActivePowerValue(10.)
            .add()
            .newGeneratorAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  5")
            .withActivePowerValue(10.)
            .add()
            .add();
        assertEquals(2, dummy4.getElementaryActions().size());

        GeneratorAction generatorAction = new GeneratorActionBuilder().withId("id").withGeneratorId("DL1").withActivePowerValue(10).withActivePowerRelativeValue(false).build();
        GeneratorAction sameGeneratorAction = new GeneratorActionBuilder().withId("id").withGeneratorId("DL1").withActivePowerValue(10).withActivePowerRelativeValue(false).build();
        assertEquals(generatorAction, sameGeneratorAction);
        NetworkAction dummy5 = new NetworkActionImpl("id", "name", "operator", null,
            new HashSet<>(List.of(generatorAction, sameGeneratorAction)), 0, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }

    @Test
    void compatibility() {
        Crac crac = createCracWithRemedialActions();
        NetworkAction injectionSetpoint = crac.getNetworkAction("generator-1-75-mw");

        assertTrue(injectionSetpoint.isCompatibleWith(injectionSetpoint));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-1")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-2")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw")));
        assertFalse(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2")));
    }
}
