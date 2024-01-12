/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
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

    private ElementaryAction mockedElementaryAction1;
    private ElementaryAction mockedElementaryAction2;
    private UsageRule mockedUsageRule1;
    private UsageRule mockedUsageRule2;

    @BeforeEach
    public void setUp() {
        mockedUsageRule1 = Mockito.mock(UsageRule.class);
        mockedUsageRule2 = Mockito.mock(UsageRule.class);
        mockedElementaryAction1 = Mockito.mock(ElementaryAction.class);
        mockedElementaryAction2 = Mockito.mock(ElementaryAction.class);
        Mockito.when(mockedElementaryAction1.getNetworkElements()).thenReturn(Set.of(new NetworkElementImpl("ne1")));
        Mockito.when(mockedElementaryAction2.getNetworkElements()).thenReturn(Set.of(new NetworkElementImpl("ne2"), new NetworkElementImpl("ne3")));
    }

    @Test
    void networkActionWithOneElementaryAction() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
            new HashSet<>(Collections.singleton(mockedUsageRule1)),
            Collections.singleton(mockedElementaryAction1),
                10
        );

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getUsageRules().size());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals("ne1", networkAction.getElementaryActions().iterator().next().getNetworkElements().iterator().next().getId());
    }

    @Test
    void networkActionWithTwoElementaryActions() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
                new HashSet<>(Arrays.asList(mockedUsageRule1, mockedUsageRule2)),
                new HashSet<>(Arrays.asList(mockedElementaryAction1, mockedElementaryAction2)),
                10
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
            Set.of(mockedElementaryAction1, mockedElementaryAction2),
                10
        );

        Mockito.when(mockedElementaryAction1.canBeApplied(Mockito.any())).thenReturn(false);
        Mockito.when(mockedElementaryAction2.canBeApplied(Mockito.any())).thenReturn(false);
        assertFalse(networkAction.apply(network));

        Mockito.when(mockedElementaryAction1.canBeApplied(Mockito.any())).thenReturn(true);
        assertFalse(networkAction.apply(network));

        Mockito.when(mockedElementaryAction2.canBeApplied(Mockito.any())).thenReturn(true);
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
            Set.of(mockedElementaryAction1, mockedElementaryAction2),
                10
        );

        Mockito.when(mockedElementaryAction1.hasImpactOnNetwork(Mockito.any())).thenReturn(true);
        Mockito.when(mockedElementaryAction2.hasImpactOnNetwork(Mockito.any())).thenReturn(false);
        assertTrue(networkAction.hasImpactOnNetwork(network));

        Mockito.when(mockedElementaryAction1.hasImpactOnNetwork(Mockito.any())).thenReturn(false);
        assertFalse(networkAction.hasImpactOnNetwork(network));
    }
}
