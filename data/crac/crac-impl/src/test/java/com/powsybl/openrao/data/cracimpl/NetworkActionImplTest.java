/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Switch;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.NetworkElement;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class NetworkActionImplTest {

    private ElementaryAction elementaryAction1;
    private ElementaryAction elementaryAction2;
    private UsageRule mockedUsageRule1;
    private UsageRule mockedUsageRule2;
    NetworkElement ne1;
    NetworkElement ne2;
    NetworkElement ne3;

    @BeforeEach
    public void setUp() {
        mockedUsageRule1 = Mockito.mock(UsageRule.class);
        mockedUsageRule2 = Mockito.mock(UsageRule.class);
        ne1 = new NetworkElementImpl("ne1");
        ne2 = new NetworkElementImpl("ne2");
        ne3 = new NetworkElementImpl("ne3");
        elementaryAction1 = new InjectionSetpointImpl(ne1, 10.0, null);
        elementaryAction2 = new SwitchPairImpl(ne2, ne3);
    }

    @Test
    void networkActionWithOneElementaryAction() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            new HashSet<>(Collections.singleton(mockedUsageRule1)),
            Collections.singleton(elementaryAction1),
                10,
            Collections.singleton(ne1)
        );

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getUsageRules().size());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals("ne1", networkAction.getNetworkElements().iterator().next().getId());
    }

    @Test
    void networkActionWithTwoElementaryActions() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
                new HashSet<>(Arrays.asList(mockedUsageRule1, mockedUsageRule2)),
                new HashSet<>(Arrays.asList(elementaryAction1, elementaryAction2)),
                10,
            new HashSet<>(Arrays.asList(ne1, ne2, ne3))
        );

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(2, networkAction.getUsageRules().size());
        assertEquals(2, networkAction.getElementaryActions().size());
        assertEquals(Set.of("ne1", "ne2", "ne3"), networkAction.getNetworkElements().stream().map(Identifiable::getId).collect(Collectors.toSet()));
    }

    @Test
    void testApply() {
        Network network = Mockito.mock(Network.class);
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            new HashSet<>(List.of(mockedUsageRule1, mockedUsageRule2)),
            Set.of(elementaryAction1, elementaryAction2),
                10,
            new HashSet<>(Arrays.asList(ne1, ne2, ne3))

        );

        Generator generator = Mockito.mock(Generator.class);
        Mockito.when(network.getIdentifiable("ne1")).thenReturn((com.powsybl.iidm.network.Identifiable) generator);
        Switch switchToOpen = Mockito.mock(Switch.class);
        Switch switchToClose = Mockito.mock(Switch.class);
        Mockito.when(network.getSwitch("ne2")).thenReturn(switchToOpen);
        Mockito.when(network.getSwitch("ne3")).thenReturn(switchToClose);

        // elementaryAction1 always true for Generator
        Mockito.when(switchToOpen.isOpen()).thenReturn(true);
        Mockito.when(switchToClose.isOpen()).thenReturn(true);
        // elementaryAction2, both switch open equals -> false (? strange that no more precise than an inequality)
        assertFalse(networkAction.apply(network));

        Mockito.when(switchToOpen.isOpen()).thenReturn(false);
        // elementaryAction2, both switch open not equals -> true
        assertTrue(networkAction.apply(network));
    }

    @Test
    void testHasImpactOnNetworkAction() {
        Network network = Mockito.mock(Network.class);
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            new HashSet<>(List.of(mockedUsageRule1, mockedUsageRule2)),
            Set.of(elementaryAction1, elementaryAction2),
                10,
            new HashSet<>(Arrays.asList(ne1, ne2, ne3))

        );

        Generator generator = Mockito.mock(Generator.class);
        Mockito.when(network.getIdentifiable("ne1")).thenReturn((com.powsybl.iidm.network.Identifiable) generator);
        Switch switchToOpen = Mockito.mock(Switch.class);
        Switch switchToClose = Mockito.mock(Switch.class);
        Mockito.when(network.getSwitch("ne2")).thenReturn(switchToOpen);
        Mockito.when(network.getSwitch("ne3")).thenReturn(switchToClose);

        Mockito.when(generator.getTargetP()).thenReturn(5.0); // impact on network yes
        Mockito.when(switchToOpen.isOpen()).thenReturn(true); // impact on network no
        Mockito.when(switchToClose.isOpen()).thenReturn(false); // impact on network no
        assertTrue(networkAction.hasImpactOnNetwork(network)); // elementaryAction1 yes and elementaryAction2 no

        Mockito.when(generator.getTargetP()).thenReturn(10.0); // impact on network no
        assertFalse(networkAction.hasImpactOnNetwork(network)); // elementaryAction1 no and elementaryAction2 no
    }
}
