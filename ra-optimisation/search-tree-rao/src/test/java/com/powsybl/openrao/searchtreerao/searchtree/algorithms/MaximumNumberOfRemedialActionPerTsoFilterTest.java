/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumNumberOfRemedialActionPerTsoFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfRaPerTso() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_2, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL));

        // arrange Leaf -> naFr1 and raBe1 have already been activated in previous leaf
        // but only naFr1 should count
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));
        Mockito.when(previousLeaf.getRangeActions()).thenReturn(Collections.singleton(RA_BE_1));
        Mockito.when(previousLeaf.getOptimizedSetpoint(RA_BE_1, P_STATE)).thenReturn(5.);

        MaximumNumberOfRemedialActionPerTsoFilter naFilter;
        Set<NetworkActionCombination> filteredNaCombination;

        // filter - max 2 topo in FR and DE
        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "be", 2);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, new HashMap<>());
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size()); // 2 combinations filtered
        assertFalse(filteredNaCombination.contains(COMB_2_FR));
        assertFalse(filteredNaCombination.contains(COMB_3_BE));

        // filter - max 1 topo in FR
        maxTopoPerTso = Map.of("fr", 1);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, new HashMap<>());
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size()); // 4 combinations filtered
        assertTrue(filteredNaCombination.contains(IND_BE_2));
        assertTrue(filteredNaCombination.contains(IND_NL_1));
        assertTrue(filteredNaCombination.contains(COMB_3_BE));
        assertTrue(filteredNaCombination.contains(COMB_2_BE_NL));

        // filter - max 1 RA in FR and max 2 RA in BE
        Map<String, Integer> maxRaPerTso = Map.of("fr", 1, "be", 2);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(new HashMap<>(), maxRaPerTso);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(IND_BE_2));
        assertTrue(filteredNaCombination.contains(IND_NL_1));
        assertTrue(filteredNaCombination.contains(COMB_2_BE_NL));

        // filter - max 2 topo in FR, max 0 topo in Nl and max 1 RA in BE
        maxTopoPerTso = Map.of("fr", 2, "nl", 0);
        maxRaPerTso = Map.of("be", 1);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRaPerTso);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(IND_FR_2));
        assertTrue(filteredNaCombination.contains(IND_FR_DE));
        assertTrue(filteredNaCombination.contains(IND_BE_2));

        // filter - no RA in NL
        maxTopoPerTso = Map.of("fr", 10, "nl", 10, "be", 10);
        maxRaPerTso = Map.of("nl", 0);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRaPerTso);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombination.size());
        assertFalse(filteredNaCombination.contains(IND_NL_1));
        assertFalse(filteredNaCombination.contains(COMB_2_BE_NL));
        assertFalse(filteredNaCombination.contains(COMB_2_FR_NL));
    }

    @Test
    void testNotKeptCombinationBecauseItExceedsMaxNaForSecondTso() {
        // The network action combination does not exceed the maximum number of network actions but exceeds the maximum number of range actions for the first TSO.
        // It exceeds the maximum number of network actions for the second TSO.
        // We ensure that the combination is not kept.
        NetworkAction naFr1 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl1 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl2 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl3 = Mockito.mock(NetworkAction.class);
        Mockito.when(naFr1.getOperator()).thenReturn("fr");
        Mockito.when(naNl1.getOperator()).thenReturn("nl");
        Mockito.when(naNl2.getOperator()).thenReturn("nl");
        Mockito.when(naNl3.getOperator()).thenReturn("nl");

        NetworkActionCombination nac = new NetworkActionCombination(Set.of(naFr1, naNl1, naNl2, naNl3));
        assertEquals(Set.of("fr", "nl"), nac.getOperators());

        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(nac));

        PstRangeAction raFr1 = createPstRangeActionWithOperator("lineFr1", "fr");
        PstRangeAction raFr2 = createPstRangeActionWithOperator("lineFr2", "fr");

        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(previousLeaf.getActivatedRangeActions(Mockito.any())).thenReturn(Set.of(raFr1, raFr2));
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr1, P_STATE)).thenReturn(5.);
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr2, P_STATE)).thenReturn(5.);

        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "nl", 2);
        Map<String, Integer> maxRemedialActionsPerTso = Map.of("fr", 2, "nl", 5);

        MaximumNumberOfRemedialActionPerTsoFilter naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRemedialActionsPerTso);
        Set<NetworkActionCombination> filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);
        assertEquals(0, filteredNaCombination.size()); // combination is filtered out
    }
}
