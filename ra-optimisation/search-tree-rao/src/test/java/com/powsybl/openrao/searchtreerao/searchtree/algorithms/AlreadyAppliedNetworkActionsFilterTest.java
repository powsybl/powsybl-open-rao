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

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb2FrDeBe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.comb3Fr;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indBe1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFr1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFr2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.indFrDe;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.naFr1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AlreadyAppliedNetworkActionsFilterTest {
    @Test
    void testRemoveAlreadyAppliedNetworkActions() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr1, indFr2, indBe1, indFrDe, comb3Fr, comb2Fr, comb2FrDeBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // filter already activated NetworkAction
        AlreadyAppliedNetworkActionsFilter naFilter = new AlreadyAppliedNetworkActionsFilter();
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.containsKey(indFr1));
        assertFalse(filteredNaCombinations.containsKey(comb3Fr));
    }
}
