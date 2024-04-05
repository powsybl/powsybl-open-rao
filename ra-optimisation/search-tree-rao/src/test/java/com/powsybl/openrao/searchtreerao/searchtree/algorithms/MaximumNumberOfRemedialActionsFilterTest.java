/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2BeNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrDeBe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Be;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indBe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFr2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indNl1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indNlBe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naBe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naFr1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumNumberOfRemedialActionsFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfRa() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe1, indNlBe, indNl1, comb2Fr, comb3Be, comb2BeNl, comb2FrDeBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        MaximumNumberOfRemedialActionsFilter naFilter;
        Map<NetworkActionCombination, Boolean> filteredNaCombination;

        // filter - max 4 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(4);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(8, filteredNaCombination.size()); // no combination filtered

        // filter - max 3 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(3);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size()); // one combination filtered
        assertFalse(filteredNaCombination.containsKey(comb3Be));

        // max 2 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(2);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indBe1));
        assertTrue(filteredNaCombination.containsKey(indNlBe));
        assertTrue(filteredNaCombination.containsKey(indNl1));

        // max 1 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(1);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(0, filteredNaCombination.size()); // all combination filtered

        // check booleans in hashmap -> max 4 RAs
        previousLeaf = Mockito.mock(Leaf.class);
        naFilter = new MaximumNumberOfRemedialActionsFilter(4);

        Mockito.when(previousLeaf.getNumberOfActivatedRangeActions()).thenReturn(1L);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1, naBe1));

        Map<NetworkActionCombination, Boolean> naToRemove = naFilter.filter(naCombinations, previousLeaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe1, false, indNlBe, false, indNl1, false, comb2Fr, true, comb2BeNl, true, comb2FrDeBe, true);
        assertEquals(expectedResult, naToRemove);
    }
}
