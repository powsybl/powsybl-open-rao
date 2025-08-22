/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.NetworkActionParameters;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_FR_NL_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_DE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NA_FR_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.P_STATE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.RA_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.addPstRangeActionToCrac;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SearchTreeBloomerTest {
    @Test
    void testDoNotRemoveCombinationDetectedInRao() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = initBloomer(List.of(new NetworkActionCombination(Set.of(na2), true)), Map.of(P_STATE.getInstant(), new RaUsageLimits()));
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Set<NetworkActionCombination> bloomResults = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(2, bloomResults.size());
        assertTrue(bloomResults.stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na1)));
        assertTrue(bloomResults.stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na2)));
    }

    @Test
    void testFilterIdenticalCombinations() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = initBloomer(List.of(new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), true)), Map.of(P_STATE.getInstant(), new RaUsageLimits()));
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Set<NetworkActionCombination> bloomResults = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(4, bloomResults.size());
    }

    @Test
    void testRangeActionRemoverWithRaUsageLimits() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE));

        // mock Leaf
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(NA_FR_1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(RA_BE_1));

        // init bloomer with raUsageLimits
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxTso(2);
        raUsageLimits.setMaxRa(3);
        raUsageLimits.setMaxRaPerTso(new HashMap<>(Map.of("be", 1)));
        SearchTreeBloomer bloomer = initBloomer(naCombinations.stream().toList(), Map.of(P_STATE.getInstant(), raUsageLimits));

        // If one of the following condition is met, to apply the naCombination, we should remove the activated RangeActions.
        // 1- (maxRa): The combination has more than one network action
        // 2- (maxTso): It contains any other operator than FR or BE
        // 3- (maxRaPerTso): It cannot contain the operator BE
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_NL_1, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_BE_1, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_BE_NL, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_FR_NL, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_3_FR_NL_BE, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_FR, leaf));
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_3_BE, leaf));
        // otherwise they can be kept.
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_FR_2, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_FR_DE, leaf));
    }

    @Test
    void testRangeActionRemoverWithMaxElementaryActionsRaUsageLimit() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE));

        // mock Leaf -> simulate a tap change from 0 to 3
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(RA_BE_1));
        Mockito.when(leaf.getOptimizedTap(RA_BE_1, P_STATE)).thenReturn(3);

        // init bloomer with raUsageLimits
        RaUsageLimits raUsageLimits = new RaUsageLimits();
        raUsageLimits.setMaxElementaryActionsPerTso(new HashMap<>(Map.of("be", 4)));
        SearchTreeBloomer bloomer = initBloomer(naCombinations.stream().toList(), Map.of(P_STATE.getInstant(), raUsageLimits));

        // If a network action combination has more than 2 elementary actions and is operated by "be" then PST range actions must be removed
        assertTrue(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_3_BE, leaf));
        // otherwise they can be kept.
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_BE_NL, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_3_FR_NL_BE, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_FR, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(COMB_2_FR_NL, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_BE_1, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_NL_1, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_FR_2, leaf));
        assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(IND_FR_DE, leaf));
    }

    @Test
    void testRangeActionRemoverWithoutRaUsageLimits() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_FR_DE, COMB_2_FR, COMB_3_BE, COMB_2_BE_NL, COMB_2_FR_NL, COMB_3_FR_NL_BE));

        // mock Leaf
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(NA_FR_1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(RA_BE_1));

        // init bloomer with fake raUsageLimits
        Instant fakeInstant = Mockito.mock(Instant.class);
        SearchTreeBloomer bloomer = initBloomer(naCombinations.stream().toList(), Map.of(fakeInstant, new RaUsageLimits()));

        // asserts that no range action should be removed as there are no RaUsageLimits in preventive
        for (NetworkActionCombination na : naCombinations) {
            assertFalse(bloomer.shouldRangeActionsBeRemovedToApplyNa(na, leaf));
        }
    }

    private SearchTreeBloomer initBloomer(List<NetworkActionCombination> naCombinations, Map<Instant, RaUsageLimits> raUsageLimits) {
        OptimizationPerimeter perimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(perimeter.getMainOptimizationState()).thenReturn(P_STATE);
        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(prePerimeterResult.getTap(Mockito.any())).thenReturn(0);
        SearchTreeInput input = SearchTreeInput.create()
            .withNetwork(NetworkActionCombinationsUtils.NETWORK)
            .withOptimizationPerimeter(perimeter)
            .withPrePerimeterResult(prePerimeterResult)
            .build();
        NetworkActionParameters networkActionParameters = Mockito.mock(NetworkActionParameters.class);
        Mockito.when(networkActionParameters.getNetworkActionCombinations()).thenReturn(naCombinations);
        Mockito.when(networkActionParameters.skipNetworkActionFarFromMostLimitingElements()).thenReturn(false);
        Mockito.when(networkActionParameters.getMaxNumberOfBoundariesForSkippingNetworkActions()).thenReturn(0);
        SearchTreeParameters parameters = Mockito.mock(SearchTreeParameters.class);
        Mockito.when(parameters.getRaLimitationParameters()).thenReturn(raUsageLimits);
        Mockito.when(parameters.getNetworkActionParameters()).thenReturn(networkActionParameters);
        return new SearchTreeBloomer(input, parameters);
    }

    @Test
    void testGetNumberOfPstTapsMovedByTso() {
        PstRangeAction pstRangeAction = addPstRangeActionToCrac();
        SearchTreeBloomer bloomer = initBloomer(List.of(), Map.of());

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedRangeActions(P_STATE)).thenReturn(Set.of(pstRangeAction));
        Mockito.when(leaf.getOptimizedTap(pstRangeAction, P_STATE)).thenReturn(10);

        assertEquals(Map.of("BE", 10), bloomer.getNumberOfPstTapsMovedByTso(leaf));
    }
}
