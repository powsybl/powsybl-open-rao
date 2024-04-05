/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2BeNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Be;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3FrNlBe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.createNetworkActionWithOperator;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.createPstRangeActionWithOperator;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indBe1;
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
class MaximumNumberOfTsosFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfTsos() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl, comb3FrNlBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        
        MaximumNumberOfTsosFilter naFilter;
        Map<NetworkActionCombination, Boolean> filteredNaCombination;

        // max 3 TSOs
        naFilter = new MaximumNumberOfTsosFilter(3, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        naFilter = new MaximumNumberOfTsosFilter(2, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.containsKey(comb2BeNl)); // one combination filtered

        // max 1 TSO
        naFilter = new MaximumNumberOfTsosFilter(1, pState);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indFrDe));
        assertTrue(filteredNaCombination.containsKey(comb2Fr));

        // check booleans in hashmap -> max 2 TSOs
        Leaf leaf = Mockito.mock(Leaf.class);
        naFilter = new MaximumNumberOfTsosFilter(2, pState);

        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(raBe1));

        Map<NetworkActionCombination, Boolean> naToRemove = naFilter.filter(naCombinations, leaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe1, false, indNl1, true, indFrDe, false, comb2Fr, false, comb3Be, false, comb2FrNl, true);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testGetActivatedTsos() {
        RangeAction<?> nonActivatedRa = createPstRangeActionWithOperator("NNL2AA1  NNL3AA1  1", "nl");
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        rangeActions.add(nonActivatedRa);
        rangeActions.add(raBe1);

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(leaf.getRangeActions()).thenReturn(rangeActions);
        Mockito.when(leaf.getOptimizedSetpoint(raBe1, pState)).thenReturn(5.);
        Mockito.when(leaf.getOptimizedSetpoint(nonActivatedRa, pState)).thenReturn(0.);

        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(Integer.MAX_VALUE, pState);
        Set<String> activatedTsos = naFilter.getTsosWithActivatedNetworkActions(leaf);

        // only network actions count when counting activated RAs in previous leaf
        assertEquals(Set.of("fr"), activatedTsos);
    }

    @Test
    void testDontFilterNullOperator() {
        NetworkAction naNoOperator1 = createNetworkActionWithOperator("NNL2AA1  NNL3AA1  1", null);
        List<NetworkActionCombination> listOfNaCombinations = List.of(new NetworkActionCombination(Set.of(naFr1, naBe1, naNoOperator1)));
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // max 2 TSOs
        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(2, pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(1, filteredNaCombination.size()); // no combination filtered, because null operator should not count
    }
}
