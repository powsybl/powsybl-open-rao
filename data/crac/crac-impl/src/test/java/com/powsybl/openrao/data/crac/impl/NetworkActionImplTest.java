/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class NetworkActionImplTest {

    private Network network;
    private Switch switchToOpen;
    private Switch switchToClose;
    private Generator generator;
    private NetworkAction networkAction1;
    private NetworkAction networkAction2;

    @BeforeEach
    public void setUp() {
        network = Network.read("TestCase12NodesWith2Switches.uct", getClass().getResourceAsStream("/TestCase12NodesWith2Switches.uct"));
        generator = network.getGenerator("BBE1AA1 _generator");
        switchToOpen = network.getSwitch("NNL3AA11 NNL3AA12 1");
        switchToClose = network.getSwitch("NNL3AA13 NNL3AA14 1");

        Crac crac = new CracImplFactory().create("cracId");
        crac.newInstant("now", InstantKind.PREVENTIVE);
        crac.newInstant("after", InstantKind.OUTAGE);
        crac.newInstant("then", InstantKind.AUTO);

        NetworkActionAdder networkActionAdder1 = crac.newNetworkAction().withId("id1").withName("name").withOperator("operator").withSpeed(10);
        networkActionAdder1.newOnInstantUsageRule().withInstant("now").add();
        networkActionAdder1.newGeneratorAction().withNetworkElement(generator.getId()).withActivePowerValue(0).add();
        networkAction1 = networkActionAdder1.add();

        NetworkActionAdder networkActionAdder2 = crac.newNetworkAction().withId("id2").withName("name").withOperator("operator").withSpeed(10);
        networkActionAdder2.newOnInstantUsageRule().withInstant("now").add();
        networkActionAdder2.newOnInstantUsageRule().withInstant("then").add();
        networkActionAdder2.newGeneratorAction().withNetworkElement(generator.getId()).withActivePowerValue(10.0).add();
        networkActionAdder2.newSwitchPair().withSwitchToOpen(switchToOpen.getId()).withSwitchToClose(switchToClose.getId()).add();
        networkAction2 = networkActionAdder2.add();
    }

    @Test
    void networkActionWithOneElementaryAction() {
        assertEquals("id1", networkAction1.getId());
        assertEquals("name", networkAction1.getName());
        assertEquals("operator", networkAction1.getOperator());
        assertEquals(1, networkAction1.getUsageRules().size());
        assertEquals(1, networkAction1.getElementaryActions().size());
        assertEquals(generator.getId(), networkAction1.getNetworkElements().iterator().next().getId());
    }

    @Test
    void networkActionWithTwoElementaryActions() {
        assertEquals("id2", networkAction2.getId());
        assertEquals("name", networkAction2.getName());
        assertEquals("operator", networkAction2.getOperator());
        assertEquals(2, networkAction2.getUsageRules().size());
        assertEquals(2, networkAction2.getElementaryActions().size());
        assertEquals(Set.of(generator.getId(), switchToOpen.getId(), switchToClose.getId()), networkAction2.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
    }

    @Test
    void testCanBeApplied() {
        assertTrue(networkAction1.canBeApplied(network)); // true for generator
        switchToOpen.setOpen(true);
        switchToClose.setOpen(true);
        assertFalse(networkAction2.canBeApplied(network)); // true for generator but false for switch pair

        switchToOpen.setOpen(false); // now it is true for switch pair
        assertTrue(networkAction2.canBeApplied(network));
    }

    @Test
    void testHasImpactOnNetworkAction() {
        // generator action set target P to 10.0
        generator.setTargetP(5.0); // impact on network yes
        switchToOpen.setOpen(true); // impact on network no
        switchToClose.setOpen(false); // impact on network no
        assertTrue(networkAction2.hasImpactOnNetwork(network)); // generatorAction yes and switchAction no

        generator.setTargetP(10.0); // impact on network no
        assertFalse(networkAction2.hasImpactOnNetwork(network)); // generatorAction no and switchAction no
    }

    @Test
    void compatibility() {
        Crac crac = CommonCracCreation.createCracWithRemedialActions();

        assertTrue(crac.getNetworkAction("hvdc-fr-es-200-mw").isCompatibleWith(crac.getNetworkAction("hvdc-fr-es-200-mw")));
        assertFalse(crac.getNetworkAction("hvdc-fr-es-200-mw").isCompatibleWith(crac.getNetworkAction("hvdc-es-fr-200-mw")));
        assertTrue(crac.getNetworkAction("hvdc-fr-es-200-mw").isCompatibleWith(crac.getNetworkAction("aligned-psts")));
        assertTrue(crac.getNetworkAction("hvdc-fr-es-200-mw").isCompatibleWith(crac.getNetworkAction("switch-pair-and-pst")));
        assertTrue(crac.getNetworkAction("hvdc-es-fr-200-mw").isCompatibleWith(crac.getNetworkAction("hvdc-es-fr-200-mw")));
        assertTrue(crac.getNetworkAction("hvdc-es-fr-200-mw").isCompatibleWith(crac.getNetworkAction("aligned-psts")));
        assertTrue(crac.getNetworkAction("hvdc-es-fr-200-mw").isCompatibleWith(crac.getNetworkAction("switch-pair-and-pst")));
        assertTrue(crac.getNetworkAction("aligned-psts").isCompatibleWith(crac.getNetworkAction("aligned-psts")));
        assertFalse(crac.getNetworkAction("aligned-psts").isCompatibleWith(crac.getNetworkAction("switch-pair-and-pst")));
        assertTrue(crac.getNetworkAction("switch-pair-and-pst").isCompatibleWith(crac.getNetworkAction("switch-pair-and-pst")));
    }

    @Test
    void testCopyWithNewId() {
        NetworkAction duplicate = NetworkActionImpl.copyWithNewId(networkAction1, "copy");
        assertEquals("copy", duplicate.getId());
        assertEquals("name", duplicate.getName());
        assertEquals("operator", duplicate.getOperator());
        assertEquals(Optional.of(10), duplicate.getSpeed());
        assertEquals(1, duplicate.getUsageRules().size());
        assertEquals(1, duplicate.getElementaryActions().size());
        assertEquals(generator.getId(), duplicate.getNetworkElements().iterator().next().getId());
    }
}
