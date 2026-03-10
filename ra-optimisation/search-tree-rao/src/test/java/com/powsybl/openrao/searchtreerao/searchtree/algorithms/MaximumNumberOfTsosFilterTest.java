/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.*;
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
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        MaximumNumberOfTsosFilter naFilter;
        Set<NetworkActionCombination> filteredNaCombination;

        // max 3 TSOs
        naFilter = new MaximumNumberOfTsosFilter(3);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf, ReportNode.NO_OP);

        assertEquals(9, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        naFilter = new MaximumNumberOfTsosFilter(2);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf, ReportNode.NO_OP);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.contains(COMB_2_BE_NL)); // one combination filtered

        // max 1 TSO
        naFilter = new MaximumNumberOfTsosFilter(1);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf, ReportNode.NO_OP);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(IND_FR_2));
        assertTrue(filteredNaCombination.contains(IND_FR_DE));
        assertTrue(filteredNaCombination.contains(COMB_2_FR));
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

        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(Integer.MAX_VALUE);
        Set<String> activatedTsos = naFilter.getTsosWithActivatedNetworkActions(leaf);

        // only network actions count when counting activated RAs in previous leaf
        assertEquals(Set.of("fr"), activatedTsos);
    }

    @Test
    void testDontFilterNullOperator() {
        NetworkAction naNoOperator1 = createNetworkActionWithOperator("NNL2AA1  NNL3AA1  1", null);
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(new NetworkActionCombination(Set.of(NA_FR_1, NA_BE_1, naNoOperator1))));

        // previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        // max 2 TSOs
        MaximumNumberOfTsosFilter naFilter = new MaximumNumberOfTsosFilter(2);
        Set<NetworkActionCombination> filteredNaCombination = naFilter.filter(naCombinations, previousLeaf, ReportNode.NO_OP);

        assertEquals(1, filteredNaCombination.size()); // no combination filtered, because null operator should not count
    }
}
