/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_DE_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_DE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_FR_1;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AlreadyAppliedNetworkActionsFilterTest {
    @Test
    void testRemoveAlreadyAppliedNetworkActions() {

        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_1, IND_FR_2, IND_BE_1, IND_FR_DE, COMB_3_FR, COMB_2_FR, COMB_2_FR_DE_BE));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(NA_FR_1));

        // filter already activated NetworkAction
        AlreadyAppliedNetworkActionsFilter naFilter = new AlreadyAppliedNetworkActionsFilter();
        Set<NetworkActionCombination> filteredNaCombinations = naFilter.filter(naCombinations, previousLeaf);

        Assertions.assertThat(filteredNaCombinations)
            .hasSize(5)
            .doesNotContain(IND_FR_1, COMB_3_FR);
    }
}
