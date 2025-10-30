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
import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_DE_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_BE;

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
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_BE, IND_NL_1, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_DE_BE));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        MaximumNumberOfRemedialActionsFilter naFilter;
        Set<NetworkActionCombination> filteredNaCombination;

        // filter - max 4 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(4);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(8, filteredNaCombination.size()); // no combination filtered

        // filter - max 3 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(3);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size()); // one combination filtered
        assertFalse(filteredNaCombination.contains(COMB_3_BE));

        // max 2 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(2);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(IND_FR_2));
        assertTrue(filteredNaCombination.contains(IND_BE_1));
        assertTrue(filteredNaCombination.contains(IND_NL_BE));
        assertTrue(filteredNaCombination.contains(IND_NL_1));

        // max 1 RAs
        naFilter = new MaximumNumberOfRemedialActionsFilter(1);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(0, filteredNaCombination.size()); // all combination filtered
    }
}
