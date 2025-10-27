/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NETWORK;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class HasImpactOnNetworkFilterTest {
    @Test
    void testFilter() {
        NetworkAction networkAction1 = mockNetworkAction(true);
        NetworkAction networkAction2 = mockNetworkAction(false);
        NetworkAction networkAction3 = mockNetworkAction(false);
        NetworkAction networkAction4 = mockNetworkAction(false);
        NetworkActionCombination networkActionCombination1 = new NetworkActionCombination(Set.of(networkAction1, networkAction2));
        NetworkActionCombination networkActionCombination2 = new NetworkActionCombination(Set.of(networkAction3, networkAction4));

        HasImpactOnNetworkFilter hasImpactOnNetworkFilter = new HasImpactOnNetworkFilter(NETWORK);
        assertEquals(Set.of(networkActionCombination1), hasImpactOnNetworkFilter.filter(Set.of(networkActionCombination1, networkActionCombination2), null));
    }

    private static NetworkAction mockNetworkAction(boolean hasImpactOnNetwork) {
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        Mockito.when(networkAction.hasImpactOnNetwork(NETWORK)).thenReturn(hasImpactOnNetwork);
        return networkAction;
    }
}
