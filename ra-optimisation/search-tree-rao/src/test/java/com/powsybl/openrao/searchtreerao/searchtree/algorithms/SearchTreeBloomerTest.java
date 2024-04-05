/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.P_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SearchTreeBloomerTest {
    @Test
    void testDoNotRemoveCombinationDetectedInRao() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = new SearchTreeBloomer(NetworkActionCombinationsUtils.NETWORK, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), false, 0, List.of(new NetworkActionCombination(Set.of(na2), true)), P_STATE);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Map<NetworkActionCombination, Boolean> result = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(2, result.size());
        assertTrue(result.keySet().stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na1)));
        assertTrue(result.keySet().stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na2)));
    }

    @Test
    void testFilterIdenticalCombinations() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = new SearchTreeBloomer(NetworkActionCombinationsUtils.NETWORK, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), false, 0, List.of(new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), true)), P_STATE);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Map<NetworkActionCombination, Boolean> result = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(4, result.size());
    }
}
