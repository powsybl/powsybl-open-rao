/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation.createCracWithRemedialActions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class NetworkActionImplTest {

    private ElementaryAction mockedElementaryAction1;
    private ElementaryAction mockedElementaryAction2;
    private TriggerCondition mockedTriggerCondition1;
    private TriggerCondition mockedTriggerCondition2;

    @BeforeEach
    public void setUp() {
        mockedTriggerCondition1 = Mockito.mock(TriggerCondition.class);
        mockedTriggerCondition2 = Mockito.mock(TriggerCondition.class);
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
            new HashSet<>(Collections.singleton(mockedTriggerCondition1)),
            Collections.singleton(mockedElementaryAction1),
                10);

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(1, networkAction.getTriggerConditions().size());
        assertEquals(1, networkAction.getElementaryActions().size());
        assertEquals("ne1", networkAction.getElementaryActions().iterator().next().getNetworkElements().iterator().next().getId());
    }

    @Test
    void networkActionWithTwoElementaryActions() {
        NetworkAction networkAction = new NetworkActionImpl(
            "id",
            "name",
            "operator",
                new HashSet<>(Arrays.asList(mockedTriggerCondition1, mockedTriggerCondition2)),
                new HashSet<>(Arrays.asList(mockedElementaryAction1, mockedElementaryAction2)),
                10);

        assertEquals("id", networkAction.getId());
        assertEquals("name", networkAction.getName());
        assertEquals("operator", networkAction.getOperator());
        assertEquals(2, networkAction.getTriggerConditions().size());
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
            new HashSet<>(List.of(mockedTriggerCondition1, mockedTriggerCondition2)),
            Set.of(mockedElementaryAction1, mockedElementaryAction2),
                10);

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
            new HashSet<>(List.of(mockedTriggerCondition1, mockedTriggerCondition2)),
            Set.of(mockedElementaryAction1, mockedElementaryAction2),
                10);

        Mockito.when(mockedElementaryAction1.hasImpactOnNetwork(Mockito.any())).thenReturn(true);
        Mockito.when(mockedElementaryAction2.hasImpactOnNetwork(Mockito.any())).thenReturn(false);
        assertTrue(networkAction.hasImpactOnNetwork(network));

        Mockito.when(mockedElementaryAction1.hasImpactOnNetwork(Mockito.any())).thenReturn(false);
        assertFalse(networkAction.hasImpactOnNetwork(network));
    }

    @Test
    void compatibility() {
        Crac crac = createCracWithRemedialActions();

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
}
