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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_DE_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_FR_1;
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
        List<NetworkActionCombination> listOfNaCombinations = List.of(IND_FR_2, IND_BE_1, IND_NL_BE, IND_NL_1, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_DE_BE);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

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
        assertFalse(filteredNaCombination.containsKey(COMB_3_BE));

        // max 2 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(2);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(IND_FR_2));
        assertTrue(filteredNaCombination.containsKey(IND_BE_1));
        assertTrue(filteredNaCombination.containsKey(IND_NL_BE));
        assertTrue(filteredNaCombination.containsKey(IND_NL_1));

        // max 1 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(1);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(0, filteredNaCombination.size()); // all combination filtered

        // check booleans in hashmap -> max 4 RAs
        previousLeaf = Mockito.mock(Leaf.class);
        naFilter = new MaximumNumberOfRemedialActionsFilter(4);

        Mockito.when(previousLeaf.getNumberOfActivatedRangeActions()).thenReturn(1L);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(NA_FR_1, NA_BE_1));

        Map<NetworkActionCombination, Boolean> naToRemove = naFilter.filter(naCombinations, previousLeaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(IND_FR_2, false, IND_BE_1, false, IND_NL_BE, false, IND_NL_1, false, COMB_2_FR, true, COMB_2_BE_NL, true, COMB_2_FR_DE_BE, true);
        assertEquals(expectedResult, naToRemove);
    }
}
