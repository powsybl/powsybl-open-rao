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

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_FR_NL_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.createNetworkActionWithOperator;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.createPstRangeActionWithOperator;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_DE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_FR_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.P_STATE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.RA_BE_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumNumberOfTsosFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfTsos_() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        MaximumNumberOfTsosFilter naFilter;
        Map<NetworkActionCombination, Boolean> filteredNaCombination;

        // max 3 TSOs
        naFilter = new MaximumNumberOfTsosFilter(3, P_STATE);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        naFilter = new MaximumNumberOfTsosFilter(2, P_STATE);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.containsKey(COMB_2_BE_NL)); // one combination filtered

        // max 1 TSO
        naFilter = new MaximumNumberOfTsosFilter(1, P_STATE);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(IND_FR_2));
        assertTrue(filteredNaCombination.containsKey(IND_FR_DE));
        assertTrue(filteredNaCombination.containsKey(COMB_2_FR));

        // check booleans in hashmap -> max 2 TSOs
        Leaf leaf = Mockito.mock(Leaf.class);
        naFilter = new MaximumNumberOfTsosFilter(2, P_STATE);

        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(NA_FR_1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(RA_BE_1));

        Map<NetworkActionCombination, Boolean> naToRemove = naFilter.filter(naCombinations, leaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(IND_FR_2, false, IND_BE_1, false, IND_NL_1, true, IND_FR_DE, false, COMB_2_FR, false, COMB_3_BE, false, COMB_2_FR_NL, true);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfTsos() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        MaximumNumberOfTsosFilter naFilter;
        Set<NetworkActionCombination> filteredNaCombination;

        // max 3 TSOs
        naFilter = new MaximumNumberOfTsosFilter(3, P_STATE);
        filteredNaCombination = naFilter.filterCombinations(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        naFilter = new MaximumNumberOfTsosFilter(2, P_STATE);
        filteredNaCombination = naFilter.filterCombinations(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.contains(COMB_2_BE_NL)); // one combination filtered

        // max 1 TSO
        naFilter = new MaximumNumberOfTsosFilter(1, P_STATE);
        filteredNaCombination = naFilter.filterCombinations(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(IND_FR_2));
        assertTrue(filteredNaCombination.contains(IND_FR_DE));
        assertTrue(filteredNaCombination.contains(COMB_2_FR));

        // check booleans in hashmap -> max 2 TSOs
        Leaf leaf = Mockito.mock(Leaf.class);
        naFilter = new MaximumNumberOfTsosFilter(2, P_STATE);

        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(NA_FR_1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(RA_BE_1));

        Set<NetworkActionCombination> naToRemove = naFilter.filterCombinations(naCombinations, leaf);
        // TODO: move this to bloomer test: Map<NetworkActionCombination, Boolean> expectedResult = Map.of(IND_FR_2, false, IND_BE_1, false, IND_NL_1, true, IND_FR_DE, false, COMB_2_FR, false, COMB_3_BE, false, COMB_2_FR_NL, true);
        Set<NetworkActionCombination> expectedResult = Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_FR_NL);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testGetActivatedTsos() {
        RangeAction<?> nonActivatedRa = createPstRangeActionWithOperator("NNL2AA1  NNL3AA1  1", "nl");
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        rangeActions.add(nonActivatedRa);
        rangeActions.add(RA_BE_1);

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));
        Mockito.when(leaf.getRangeActions()).thenReturn(rangeActions);
        Mockito.when(leaf.getOptimizedSetpoint(RA_BE_1, P_STATE)).thenReturn(5.);
        Mockito.when(leaf.getOptimizedSetpoint(nonActivatedRa, P_STATE)).thenReturn(0.);

        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(Integer.MAX_VALUE, P_STATE);
        Set<String> activatedTsos = naFilter.getTsosWithActivatedNetworkActions(leaf);

        // only network actions count when counting activated RAs in previous leaf
        assertEquals(Set.of("fr"), activatedTsos);
    }

    @Test
    void testDontFilterNullOperator() {
        NetworkAction naNoOperator1 = createNetworkActionWithOperator("NNL2AA1  NNL3AA1  1", null);
        List<NetworkActionCombination> listOfNaCombinations = List.of(new NetworkActionCombination(Set.of(NA_FR_1, NA_BE_1, naNoOperator1)));
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        // max 2 TSOs
        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(2, P_STATE);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(1, filteredNaCombination.size()); // no combination filtered, because null operator should not count
    }
}
