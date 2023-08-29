/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.search_tree.algorithms;

import com.farao_community.farao.commons.CountryBoundary;
import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.search_tree_rao.commons.NetworkActionCombination;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class SearchTreeBloomerTest {

    private Crac crac;
    private Network network;
    private State pState;

    private NetworkAction naFr1;
    private NetworkAction naBe1;
    private PstRangeAction raBe1;

    private NetworkActionCombination indFr1;
    private NetworkActionCombination indFr2;
    private NetworkActionCombination indBe1;
    private NetworkActionCombination indBe2;
    private NetworkActionCombination indNl1;
    private NetworkActionCombination indDe1;
    private NetworkActionCombination indFrDe;
    private NetworkActionCombination indNlBe;
    private NetworkActionCombination indDeNl;

    private NetworkActionCombination comb3Fr;
    private NetworkActionCombination comb2Fr;
    private NetworkActionCombination comb3Be;
    private NetworkActionCombination comb2De;
    private NetworkActionCombination comb2BeNl;
    private NetworkActionCombination comb2FrNl;
    private NetworkActionCombination comb2FrDeBe;
    private NetworkActionCombination comb3FrNlBe;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        pState = crac.getPreventiveState();

        crac.newFlowCnec()
            .withId("cnecBe")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(Instant.PREVENTIVE).withOptimized(true)
            .withOperator("operator1").newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withSide(Side.LEFT)
            .withMin(-1500.)
            .withMax(1500.)
            .add()
            .withNominalVoltage(380.)
            .withIMax(5000.)
            .add();

        naFr1 = createNetworkActionWithOperator("FFR1AA1  FFR2AA1  1", "fr");
        naBe1 = createNetworkActionWithOperator("BBE1AA1  BBE2AA1  1", "be");
        raBe1 = createPstRangeActionWithOperator("BBE2AA1  BBE3AA1  1", "be");

        NetworkAction naFr2 = createNetworkActionWithOperator("FFR1AA1  FFR3AA1  1", "fr");
        NetworkAction naFr3 = createNetworkActionWithOperator("FFR2AA1  FFR3AA1  1", "fr");
        NetworkAction naBe2 = createNetworkActionWithOperator("BBE1AA1  BBE3AA1  1", "be");
        NetworkAction naBe3 = createNetworkActionWithOperator("BBE2AA1  BBE3AA1  1", "be");
        NetworkAction naNl1 = createNetworkActionWithOperator("NNL1AA1  NNL2AA1  1", "nl");
        NetworkAction naDe1 = createNetworkActionWithOperator("DDE1AA1  DDE3AA1  1", "de");
        NetworkAction naDe2 = createNetworkActionWithOperator("DDE2AA1  DDE3AA1  1", "de");
        NetworkAction naFrDe = createNetworkActionWithOperator("FFR2AA1  DDE3AA1  1", "fr");
        NetworkAction naNlBe = createNetworkActionWithOperator("NNL2AA1  BBE3AA1  1", "nl");
        NetworkAction naDeNl = createNetworkActionWithOperator("DDE2AA1  NNL3AA1  1", "de");

        indFr1 = new NetworkActionCombination(naFr1);
        indFr2 = new NetworkActionCombination(naFr2);
        indBe1 = new NetworkActionCombination(naBe1);
        indBe2 = new NetworkActionCombination(naBe2);
        indNl1 = new NetworkActionCombination(naNl1);
        indDe1 = new NetworkActionCombination(naDe1);
        indFrDe = new NetworkActionCombination(naFrDe);
        indNlBe = new NetworkActionCombination(naNlBe);
        indDeNl = new NetworkActionCombination(naDeNl);

        comb3Fr = new NetworkActionCombination(Set.of(naFr1, naFr2, naFr3));
        comb2Fr = new NetworkActionCombination(Set.of(naFr2, naFr3));
        comb3Be = new NetworkActionCombination(Set.of(naBe1, naBe2, naBe3));
        comb2De = new NetworkActionCombination(Set.of(naDe1, naDe2));
        comb2BeNl = new NetworkActionCombination(Set.of(naBe1, naNl1));
        comb2FrNl = new NetworkActionCombination(Set.of(naFr2, naNl1));
        comb2FrDeBe = new NetworkActionCombination(Set.of(naFrDe, naBe1));
        comb3FrNlBe = new NetworkActionCombination(Set.of(naFr2, naBe2, naNlBe));
    }

    @Test
    void testRemoveAlreadyActivatedNetworkActions() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr1, indFr2, indBe1, indFrDe, comb3Fr, comb2Fr, comb2FrDeBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // filter already activated NetworkAction
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = bloomer.removeAlreadyActivatedNetworkActions(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.containsKey(indFr1));
        assertFalse(filteredNaCombinations.containsKey(comb3Fr));
    }

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
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, false, 0, preDefinedNaCombinations, pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombinations = bloomer.removeAlreadyTestedCombinations(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.containsKey(indNl1)); // already tested within preDefined comb2BeNl
        assertFalse(filteredNaCombinations.containsKey(indFrDe)); // already tested within preDefined comb2FrDeBe
    }

    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfRa() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe1, indNlBe, indNl1, comb2Fr, comb3Be, comb2BeNl, comb2FrDeBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // filter - max 4 RAs
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, 4, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(8, filteredNaCombination.size()); // no combination filtered

        // filter - max 3 RAs
        bloomer = new SearchTreeBloomer(network, 3, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size()); // one combination filtered
        assertFalse(filteredNaCombination.containsKey(comb3Be));

        // max 2 RAs
        bloomer = new SearchTreeBloomer(network, 2, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indBe1));
        assertTrue(filteredNaCombination.containsKey(indNlBe));
        assertTrue(filteredNaCombination.containsKey(indNl1));

        // max 1 RAs
        bloomer = new SearchTreeBloomer(network, 1, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(0, filteredNaCombination.size()); // all combination filtered

        // check booleans in hashmap -> max 4 RAs
        previousLeaf = Mockito.mock(Leaf.class);
        bloomer = new SearchTreeBloomer(network, 4, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), false, 0, new ArrayList<>(), pState);

        Mockito.when(previousLeaf.getNumberOfActivatedRangeActions()).thenReturn(1L);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1, naBe1));

        Map<NetworkActionCombination, Boolean> naToRemove = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe1, false, indNlBe, false, indNl1, false, comb2Fr, true, comb2BeNl, true, comb2FrDeBe, true);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfRaPerTso() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe2, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange Leaf -> naFr1 and raBe1 have already been activated in previous leaf
        // but only naFr1 should count
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(previousLeaf.getRangeActions()).thenReturn(Collections.singleton(raBe1));
        Mockito.when(previousLeaf.getOptimizedSetpoint(raBe1, pState)).thenReturn(5.);

        // filter - max 2 topo in FR and DE
        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "be", 2);
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, new HashMap<>(), false, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size()); // 2 combinations filtered
        assertFalse(filteredNaCombination.containsKey(comb2Fr));
        assertFalse(filteredNaCombination.containsKey(comb3Be));

        // filter - max 1 topo in FR
        maxTopoPerTso = Map.of("fr", 1);
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, new HashMap<>(), false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size()); // 4 combinations filtered
        assertTrue(filteredNaCombination.containsKey(indBe2));
        assertTrue(filteredNaCombination.containsKey(indNl1));
        assertTrue(filteredNaCombination.containsKey(comb3Be));
        assertTrue(filteredNaCombination.containsKey(comb2BeNl));

        // filter - max 1 RA in FR and max 2 RA in BE
        Map<String, Integer> maxRaPerTso = Map.of("fr", 1, "be", 2);
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), maxRaPerTso, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indBe2));
        assertTrue(filteredNaCombination.containsKey(indNl1));
        assertTrue(filteredNaCombination.containsKey(comb2BeNl));

        // filter - max 2 topo in FR, max 0 topo in Nl and max 1 RA in BE
        maxTopoPerTso = Map.of("fr", 2, "nl", 0);
        maxRaPerTso = Map.of("be", 1);
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, maxRaPerTso, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indFrDe));
        assertTrue(filteredNaCombination.containsKey(indBe2));

        // filter - no RA in NL
        maxTopoPerTso = Map.of("fr", 10, "nl", 10, "be", 10);
        maxRaPerTso = Map.of("nl", 0);
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, maxRaPerTso, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombination.size());
        assertFalse(filteredNaCombination.containsKey(indNl1));
        assertFalse(filteredNaCombination.containsKey(comb2BeNl));
        assertFalse(filteredNaCombination.containsKey(comb2FrNl));

        // check booleans in hashmap
        //Map<NetworkActionCombination, Boolean> naCombinations = Map.of(indFr2, false, indBe2, false, comb3Be, false);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(naBe1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(raBe1));

        Map<String, Integer> maxNaPerTso = Map.of("fr", 1, "be", 2);
        maxRaPerTso = Map.of("fr", 2, "be", 2);

        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxNaPerTso, maxRaPerTso, false, 0, new ArrayList<>(), pState);
        // indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl
        Map<NetworkActionCombination, Boolean> naToRemove = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, leaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe2, true, indNl1, false, indFrDe, false, comb2BeNl, true, comb2FrNl, false);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testRemoveCombinationsWhichExceedMaxNumberOfTsos() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl, comb3FrNlBe);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // max 3 TSOs
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, 3, null, null, false, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, 2, null, null, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.containsKey(comb2BeNl)); // one combination filtered

        // max 1 TSO
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, 1, null, null, false, 0, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsKey(indFr2));
        assertTrue(filteredNaCombination.containsKey(indFrDe));
        assertTrue(filteredNaCombination.containsKey(comb2Fr));

        // check booleans in hashmap -> max 2 TSOs
        Leaf leaf = Mockito.mock(Leaf.class);
        bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, 2, new HashMap<>(), new HashMap<>(), false, 0, new ArrayList<>(), pState);

        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1));
        Mockito.when(leaf.getActivatedRangeActions(Mockito.any(State.class))).thenReturn(Set.of(raBe1));

        Map<NetworkActionCombination, Boolean> naToRemove = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, leaf);
        Map<NetworkActionCombination, Boolean> expectedResult = Map.of(indFr2, false, indBe1, false, indNl1, true, indFrDe, false, comb2Fr, false, comb3Be, false, comb2FrNl, true);
        assertEquals(expectedResult, naToRemove);
    }

    @Test
    void testRemoveNetworkActionsFarFromMostLimitingElement() {

        // arrange naCombination list
        List<NetworkActionCombination> listOfNaCombinations = List.of(indFr2, indDe1, indBe1, indNl1, indNlBe, indFrDe, indDeNl, comb3Be, comb2De, comb2FrDeBe, comb2BeNl);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        // arrange previous Leaf -> most limiting element is in DE/FR
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getVirtualCostNames()).thenReturn(Collections.emptySet());

        // test - no border cross, most limiting element is in BE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec1basecase"))); // be fr
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, true, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        List<NetworkActionCombination> list1 = List.of(indFr2, indBe1, indNlBe, indFrDe, comb3Be, comb2FrDeBe, comb2BeNl);
        Map<NetworkActionCombination, Boolean> finalFilteredNaCombination = filteredNaCombination;
        list1.forEach(na -> assertTrue(finalFilteredNaCombination.containsKey(na)));

        // test - no border cross, most limiting element is in DE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec2basecase"))); // de fr
        filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size());
        List<NetworkActionCombination> list2 = List.of(indFr2, indDe1, indFrDe, indDeNl, comb2De, comb2FrDeBe);
        Map<NetworkActionCombination, Boolean> finalFilteredNaCombination2 = filteredNaCombination;
        list2.forEach(na -> assertTrue(finalFilteredNaCombination2.containsKey(na)));

        // test - max 1 border cross, most limiting element is in BE
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnecBe"))); // be
        bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, true, 1, new ArrayList<>(), pState);
        filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size());
        List<NetworkActionCombination> list3 = List.of(indFr2, indBe1, indNl1, indNlBe, indFrDe, indDeNl, comb3Be, comb2FrDeBe, comb2BeNl);
        Map<NetworkActionCombination, Boolean> finalFilteredNaCombination3 = filteredNaCombination;
        list3.forEach(na -> assertTrue(finalFilteredNaCombination3.containsKey(na)));
    }

    @Test
    void testGetOptimizedMostLimitingElementsLocation() {

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);

        Leaf leaf = mock(Leaf.class);
        Mockito.when(leaf.getVirtualCostNames()).thenReturn(Set.of("mnec", "lf"));

        Mockito.when(leaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec1basecase"))); // be fr
        Mockito.when(leaf.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(crac.getFlowCnec("cnec2basecase"))); // de fr
        Mockito.when(leaf.getCostlyElements(eq("lf"), anyInt())).thenReturn(Collections.emptyList());
        assertEquals(Set.of(Optional.of(Country.BE), Optional.of(Country.FR), Optional.of(Country.DE)), bloomer.getOptimizedMostLimitingElementsLocation(leaf));

        Mockito.when(leaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec1basecase"))); // be fr
        Mockito.when(leaf.getCostlyElements(eq("mnec"), anyInt())).thenReturn(Collections.emptyList());
        Mockito.when(leaf.getCostlyElements(eq("lf"), anyInt())).thenReturn(Collections.emptyList());
        assertEquals(Set.of(Optional.of(Country.BE), Optional.of(Country.FR)), bloomer.getOptimizedMostLimitingElementsLocation(leaf));

        Mockito.when(leaf.getMostLimitingElements(1)).thenReturn(Collections.emptyList());
        Mockito.when(leaf.getCostlyElements(eq("mnec"), anyInt())).thenReturn(Collections.emptyList());
        Mockito.when(leaf.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(crac.getFlowCnec("cnec2basecase"))); // de fr
        assertEquals(Set.of(Optional.of(Country.FR), Optional.of(Country.DE)), bloomer.getOptimizedMostLimitingElementsLocation(leaf));

        Mockito.when(leaf.getMostLimitingElements(1)).thenReturn(Collections.emptyList());
        Mockito.when(leaf.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(crac.getFlowCnec("cnec1basecase"), crac.getFlowCnec("cnec2basecase"))); // be de fr
        Mockito.when(leaf.getCostlyElements(eq("lf"), anyInt())).thenReturn(Collections.emptyList());
        assertEquals(Set.of(Optional.of(Country.BE), Optional.of(Country.FR), Optional.of(Country.DE)), bloomer.getOptimizedMostLimitingElementsLocation(leaf));

        Mockito.when(leaf.getMostLimitingElements(1)).thenReturn(Collections.emptyList());
        Mockito.when(leaf.getCostlyElements(eq("mnec"), anyInt())).thenReturn(List.of(crac.getFlowCnec("cnec2basecase")));
        Mockito.when(leaf.getCostlyElements(eq("lf"), anyInt())).thenReturn(List.of(crac.getFlowCnec("cnec1basecase")));
        assertEquals(Set.of(Optional.of(Country.BE), Optional.of(Country.FR), Optional.of(Country.DE)), bloomer.getOptimizedMostLimitingElementsLocation(leaf));
    }

    @Test
    void testIsNetworkActionCloseToLocations() {
        NetworkAction na1 = (NetworkAction) crac.newNetworkAction().withId("na").newTopologicalAction().withNetworkElement("BBE2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add().newOnInstantUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add().add();
        NetworkAction na2 = mock(NetworkAction.class);
        Mockito.when(na2.getLocation(network)).thenReturn(Set.of(Optional.of(Country.FR), Optional.empty()));

        HashSet<CountryBoundary> boundaries = new HashSet<>();
        boundaries.add(new CountryBoundary(Country.FR, Country.BE));
        boundaries.add(new CountryBoundary(Country.FR, Country.DE));
        boundaries.add(new CountryBoundary(Country.DE, Country.AT));
        CountryGraph countryGraph = new CountryGraph(boundaries);

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.empty()), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.FR)), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.BE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na2, Set.of(Optional.of(Country.AT)), countryGraph));

        bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, true, 1, new ArrayList<>(), pState);
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));

        bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, true, 2, new ArrayList<>(), pState);
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));
    }

    @Test
    void testGetActivatedTsos() {
        RangeAction<?> nonActivatedRa = createPstRangeActionWithOperator("NNL2AA1  NNL3AA1  1", "nl");
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        rangeActions.add(nonActivatedRa);
        rangeActions.add(raBe1);

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(leaf.getRangeActions()).thenReturn(rangeActions);
        Mockito.when(leaf.getOptimizedSetpoint(raBe1, pState)).thenReturn(5.);
        Mockito.when(leaf.getOptimizedSetpoint(nonActivatedRa, pState)).thenReturn(0.);

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>(), pState);
        Set<String> activatedTsos = bloomer.getTsosWithActivatedNetworkActions(leaf);

        // only network actions count when counting activated RAs in previous leaf
        assertEquals(Set.of("fr"), activatedTsos);
    }

    @Test
    void testDoNotRemoveCombinationDetectedInRao() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), false, 0, List.of(new NetworkActionCombination(Set.of(na2), true)), pState);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Map<NetworkActionCombination, Boolean> result = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(2, result.size());
        assertTrue(result.keySet().stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na1)));
        assertTrue(result.keySet().stream().anyMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na2)));
    }

    private NetworkAction createNetworkActionWithOperator(String networkElementId, String operator) {
        return (NetworkAction) crac.newNetworkAction().withId("na - " + networkElementId).withOperator(operator).newTopologicalAction().withNetworkElement(networkElementId).withActionType(ActionType.OPEN).add().add();
    }

    private PstRangeAction createPstRangeActionWithOperator(String networkElementId, String operator) {
        Map<Integer, Double> conversionMap = new HashMap<>();
        conversionMap.put(0, 0.);
        conversionMap.put(1, 1.);
        return (PstRangeAction) crac.newPstRangeAction().withId("pst - " + networkElementId).withOperator(operator).withNetworkElement(networkElementId).newOnInstantUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add().newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add().withInitialTap(0).withTapToAngleConversionMap(conversionMap).add();
    }

    @Test
    void testFilterIdenticalCombinations() {
        NetworkAction na1 = Mockito.mock(NetworkAction.class);
        NetworkAction na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getOperator()).thenReturn("fake_tso");
        Mockito.when(na2.getOperator()).thenReturn("fake_tso");

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), new HashMap<>(), false, 0, List.of(new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), false), new NetworkActionCombination(Set.of(na1, na2), true)), pState);
        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Map<NetworkActionCombination, Boolean> result = bloomer.bloom(leaf, Set.of(na1, na2));
        assertEquals(4, result.size());
    }

    @Test
    void testNotKeptCombinationBecauseItExceedsMaxNaForSecondTso() {
        // The network action combination does not exceed the maximum number of network actions but exceeds the maximum number of range actions for the first TSO.
        // It exceeds the maximum number of network actions for the second TSO.
        // We ensure that the combination is not kept.
        NetworkAction naFr1 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl1 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl2 = Mockito.mock(NetworkAction.class);
        NetworkAction naNl3 = Mockito.mock(NetworkAction.class);
        Mockito.when(naFr1.getOperator()).thenReturn("fr");
        Mockito.when(naNl1.getOperator()).thenReturn("nl");
        Mockito.when(naNl2.getOperator()).thenReturn("nl");
        Mockito.when(naNl3.getOperator()).thenReturn("nl");

        NetworkActionCombination nac = new NetworkActionCombination(Set.of(naFr1, naNl1, naNl2, naNl3));
        assertEquals(Set.of("fr", "nl"), nac.getOperators());

        List<NetworkActionCombination> listOfNaCombinations = List.of(nac);
        Map<NetworkActionCombination, Boolean> naCombinations = new HashMap<>();
        listOfNaCombinations.forEach(na -> naCombinations.put(na, false));

        PstRangeAction raFr1 = createPstRangeActionWithOperator("lineFr1", "fr");
        PstRangeAction raFr2 = createPstRangeActionWithOperator("lineFr2", "fr");

        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(previousLeaf.getActivatedRangeActions(Mockito.any())).thenReturn(Set.of(raFr1, raFr2));
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr1, pState)).thenReturn(5.);
        Mockito.when(previousLeaf.getOptimizedSetpoint(raFr2, pState)).thenReturn(5.);

        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "nl", 2);
        Map<String, Integer> maxRemedialActionsPerTso = Map.of("fr", 2, "nl", 5);

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, maxRemedialActionsPerTso, false, 0, new ArrayList<>(), pState);
        Map<NetworkActionCombination, Boolean> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);
        assertEquals(0, filteredNaCombination.size()); // combination is filtered out
    }
}
