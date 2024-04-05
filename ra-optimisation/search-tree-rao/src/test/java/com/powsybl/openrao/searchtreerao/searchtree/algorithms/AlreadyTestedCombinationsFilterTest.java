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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2BeNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrDeBe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrNl;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Be;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indBe2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indDe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFr2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFrDe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indNl1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naBe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naFr1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AlreadyTestedCombinationsFilterTest {
    @Test
    void testRemoveAlreadyTestedCombinations() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe2, indNl1, indDe1, indFrDe, comb2Fr, comb2FrNl);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));
        List<NetworkActionCombination> preDefinedNaCombinations = List.of(comb2Fr, comb3Fr, comb3Be, comb2BeNl, comb2FrNl, comb2FrDeBe);

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1, naBe1));

        // filter already tested combinations
        AlreadyTestedCombinationsFilter naFilter = new AlreadyTestedCombinationsFilter(preDefinedNaCombinations);
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.containsKey(indNl1)); // already tested within preDefined comb2BeNl
        assertFalse(filteredNaCombinations.containsKey(indFrDe)); // already tested within preDefined comb2FrDeBe
    }
}
