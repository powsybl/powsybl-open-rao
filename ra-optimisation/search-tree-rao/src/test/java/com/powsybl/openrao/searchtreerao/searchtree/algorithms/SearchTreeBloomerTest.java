/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.NetworkActionParameters;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.*;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_NL;
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
        SearchTreeInput input = SearchTreeInput.create()
            .withNetwork(NetworkActionCombinationsUtils.NETWORK)
            .withOptimizationPerimeter(perimeter)
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
    void testRemoveCombinationsWhichExceedMaxElementaryActionsPerTso() {
        PstRangeAction pstRangeAction = addPstRangeActionToCrac();
        pstRangeAction.apply(network, pstRangeAction.getTapToAngleConversionMap().get(3));

        TopologicalAction topoFr1 = Mockito.mock(TopologicalAction.class);
        TopologicalAction topoFr2 = Mockito.mock(TopologicalAction.class);
        NetworkAction naFr = Mockito.mock(NetworkAction.class);
        Mockito.when(naFr.getOperator()).thenReturn("FR");
        Mockito.when(naFr.getElementaryActions()).thenReturn(Set.of(topoFr1, topoFr2));

        TopologicalAction topoNl = Mockito.mock(TopologicalAction.class);
        NetworkAction naNl = Mockito.mock(NetworkAction.class);
        Mockito.when(naNl.getOperator()).thenReturn("NL");
        Mockito.when(naNl.getElementaryActions()).thenReturn(Set.of(topoNl));

        TopologicalAction topoBe = Mockito.mock(TopologicalAction.class);
        NetworkAction naBe = Mockito.mock(NetworkAction.class);
        Mockito.when(naBe.getOperator()).thenReturn("BE");
        Mockito.when(naBe.getElementaryActions()).thenReturn(Set.of(topoBe));

        TopologicalAction topoDe = Mockito.mock(TopologicalAction.class);
        NetworkAction naDe = Mockito.mock(NetworkAction.class);
        Mockito.when(naDe.getOperator()).thenReturn("DE");
        Mockito.when(naDe.getElementaryActions()).thenReturn(Set.of(topoDe));

        NetworkActionCombination networkActionCombinationFrNl = new NetworkActionCombination(Set.of(naFr, naNl));
        NetworkActionCombination networkActionCombinationBe = new NetworkActionCombination(Set.of(naBe));
        NetworkActionCombination networkActionCombinationDe = new NetworkActionCombination(Set.of(naDe));

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(leaf.getActivatedRangeActions(pState)).thenReturn(Set.of(pstRangeAction));

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), Map.of("BE", 3, "DE", 2, "FR", 2, "NL", 2), false, 0, List.of(), pState, null);
        Map<NetworkActionCombination, Boolean> result = bloomer.removeCombinationsWhichExceedMaxElementaryActionsPerTso(Map.of(networkActionCombinationFrNl, false, networkActionCombinationBe, false, networkActionCombinationDe, true), leaf);

        assertEquals(result, Map.of(networkActionCombinationBe, true, networkActionCombinationDe, true));
    }

    @Test
    void testGetNumberOfPstTapsMovedByTso() {
        PstRangeAction pstRangeAction = addPstRangeActionToCrac();
        pstRangeAction.apply(network, pstRangeAction.getTapToAngleConversionMap().get(10));

        assertEquals(10, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedRangeActions(pState)).thenReturn(Set.of(pstRangeAction));

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), new HashMap<>(), false, 0, List.of(), pState, null);
        assertEquals(Map.of("BE", 10), bloomer.getNumberOfPstTapsMovedByTso(leaf));
    }

    private PstRangeAction addPstRangeActionToCrac() {
        CommonCracCreation.IidmPstHelper iidmPstHelper = new CommonCracCreation.IidmPstHelper("BBE2AA1  BBE3AA1  1", network);

        crac.newPstRangeAction()
            .withId("pst-range-action")
            .withName("pst-range-action")
            .withOperator("BE")
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withInitialTap(iidmPstHelper.getInitialTap())
            .withTapToAngleConversionMap(iidmPstHelper.getTapToAngleConversionMap())
            .add();

        return crac.getPstRangeAction("pst-range-action");
    }
}
