/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2BeNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Be;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.createPstRangeActionWithOperator;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indBe2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFr2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFrDe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indNl1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naBe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naFr1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.pState;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.raBe1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumNumberOfRemedialActionPerTsoFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfRaPerTso() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe2, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange Leaf -> naFr1 and raBe1 have already been activated in previous leaf
        // but only naFr1 should count
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(previousLeaf.getRangeActions()).thenReturn(Collections.singleton(raBe1));
        Mockito.when(previousLeaf.getOptimizedSetpoint(raBe1, pState)).thenReturn(5.);

        MaximumNumberOfRemedialActionPerTsoFilter naFilter;
        Map<NetworkActionCombination, Boolean> filteredNaCombination;

        // filter - max 2 topo in FR and DE
        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "be", 2);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, new HashMap<>(), pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size()); // 2 combinations filtered
        assertFalse(filteredNaCombination.containsKey(comb2Fr));
        assertFalse(filteredNaCombination.containsKey(comb3Be));

        // filter - max 1 topo in FR
        maxTopoPerTso = Map.of("fr", 1);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, new HashMap<>(), pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size()); // 4 combinations filtered
        assertTrue(filteredNaCombination.containsKey(indBe2));
        assertTrue(filteredNaCombination.containsKey(indNl1));
        assertTrue(filteredNaCombination.containsKey(comb3Be));
        assertTrue(filteredNaCombination.containsKey(comb2BeNl));

        // filter - max 1 RA in FR and max 2 RA in BE
        Map<String, Integer> maxRaPerTso = Map.of("fr", 1, "be", 2);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(new HashMap<>(), maxRaPerTso, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indBe2));
        assertTrue(filteredNaCombination.containsKey(indNl1));
        assertTrue(filteredNaCombination.containsKey(comb2BeNl));

        // filter - max 2 topo in FR, max 0 topo in Nl and max 1 RA in BE
        maxTopoPerTso = Map.of("fr", 2, "nl", 0);
        maxRaPerTso = Map.of("be", 1);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRaPerTso, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indFrDe));
        assertTrue(filteredNaCombination.containsKey(indBe2));

        // filter - no RA in NL
        maxTopoPerTso = Map.of("fr", 10, "nl", 10, "be", 10);
        maxRaPerTso = Map.of("nl", 0);
        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRaPerTso, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombination.size());
        assertFalse(filteredNaCombination.containsKey(indNl1));
        assertFalse(filteredNaCombination.containsKey(comb2BeNl));
        assertFalse(filteredNaCombination.containsKey(comb2FrNl));

        // check booleans in hashmap
        //Map<NetworkActionCombination, Boolean> naCombinations = Map.of(indFr2, false, indBe2, false, comb3Be, false);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(naBe1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(raBe1));

        Map<String, Integer> maxNaPerTso = Map.of("fr", 1, "be", 2);
        maxRaPerTso = Map.of("fr", 2, "be", 2);

        naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxNaPerTso, maxRaPerTso, pState);
        // indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl
        Map<NetworkActionCombination, Boolean> naToRemove = naFilter.filter(naCombinations, leaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe2, true, indNl1, false, indFrDe, false, comb2BeNl, true, comb2FrNl, false);
        assertEquals(expectedResult, naToRemove);
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

        List<NetworkActionCombination> listOfNaCombinations = List.of(nac);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        PstRangeAction raFr1 = createPstRangeActionWithOperator("lineFr1", "fr");
        PstRangeAction raFr2 = createPstRangeActionWithOperator("lineFr2", "fr");

        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(previousLeaf.getActivatedRangeActions(Mockito.any())).thenReturn(Set.of(raFr1, raFr2));
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr1, pState)).thenReturn(5.);
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr2, pState)).thenReturn(5.);

        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "nl", 2);
        Map<String, Integer> maxRemedialActionsPerTso = Map.of("fr", 2, "nl", 5);

        MaximumNumberOfRemedialActionPerTsoFilter naFilter = new MaximumNumberOfRemedialActionPerTsoFilter(maxTopoPerTso, maxRemedialActionsPerTso, pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);
        assertEquals(0, filteredNaCombination.size()); // combination is filtered out
    }
}
