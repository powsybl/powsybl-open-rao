/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.CountryBoundary;
import com.farao_community.farao.commons.CountryGraph;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SearchTreeBloomerTest {

    private Crac crac;
    private Network network;

    private NetworkAction naFr1;
    private NetworkAction naBe1;
    private PstRangeAction raBe1;

    private NetworkActionCombination indFr1;
    private NetworkActionCombination indFr2;
    private NetworkActionCombination indBe1;
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

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        crac.newFlowCnec()
            .withId("cnecBe")
            .withNetworkElement("BBE1AA1  BBE2AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .withOptimized(true)
            .withOperator("operator1")
            .newThreshold()
            .withUnit(Unit.MEGAWATT)
            .withRule(BranchThresholdRule.ON_LEFT_SIDE)
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
    }

    @Test
    public void testRemoveAlreadyActivatedNetworkActions() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr1, indFr2, indBe1, indFrDe, comb3Fr, comb2Fr, comb2FrDeBe);

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // filter already activated NetworkAction
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        List<NetworkActionCombination> filteredNaCombinations = bloomer.removeAlreadyActivatedNetworkActions(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.contains(indFr1));
        assertFalse(filteredNaCombinations.contains(comb3Fr));
    }

    @Test
    public void testRemoveAlreadyTestedCombinations() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr2, indNl1, indDe1, indFrDe, comb2Fr, comb2FrNl);
        List<NetworkActionCombination> preDefinedNaCombinations = List.of(comb2Fr, comb3Fr, comb3Be, comb2BeNl, comb2FrNl, comb2FrDeBe);

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Set.of(naFr1, naBe1));

        // filter already tested combinations
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, false, 0, preDefinedNaCombinations);
        List<NetworkActionCombination> filteredNaCombinations = bloomer.removeAlreadyTestedCombinations(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombinations.size());
        assertFalse(filteredNaCombinations.contains(indNl1)); // already tested within preDefined comb2BeNl
        assertFalse(filteredNaCombinations.contains(indFrDe)); // already tested within preDefined comb2FrDeBe
    }

    @Test
    public void testRemoveCombinationsWhichExceedMaxNumberOfRa() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr2, indBe1, indNlBe, indNl1, comb2Fr, comb3Be, comb2BeNl, comb2FrDeBe);

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // filter - max 4 RAs
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 4, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        List<NetworkActionCombination> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(8, filteredNaCombination.size()); // no combination filtered

        // filter - max 3 RAs
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 3, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size()); // one combination filtered
        assertFalse(filteredNaCombination.contains(comb3Be));

        // max 2 RAs
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 2, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(indFr2));
        assertTrue(filteredNaCombination.contains(indBe1));
        assertTrue(filteredNaCombination.contains(indNlBe));
        assertTrue(filteredNaCombination.contains(indNl1));

        // max 1 RAs
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 1, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRa(naCombinations, previousLeaf);

        assertEquals(0, filteredNaCombination.size()); // all combination filtered
    }

    @Test
    public void testRemoveCombinationsWhichExceedMaxNumberOfRaPerTso() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl);

        // arrange Leaf -> naFr1 and raBe1 have already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(previousLeaf.getRangeActions()).thenReturn(Collections.singleton(raBe1));
        Mockito.when(previousLeaf.getOptimizedSetPoint(raBe1)).thenReturn(5.);

        // filter - max 2 topo in Fr and De
        Map<String, Integer> maxTopoPerTso = Map.of("fr", 2, "be", 2);
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, new HashMap<>(), false, 0, new ArrayList<>());
        List<NetworkActionCombination> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size()); // 2 combinations filtered
        assertFalse(filteredNaCombination.contains(comb2Fr));
        assertFalse(filteredNaCombination.contains(comb3Be));

        // filter - max 1 topo in Fr
        maxTopoPerTso = Map.of("fr", 1);
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, new HashMap<>(), false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(4, filteredNaCombination.size()); // 4 combinations filtered
        assertTrue(filteredNaCombination.contains(indBe1));
        assertTrue(filteredNaCombination.contains(indNl1));
        assertTrue(filteredNaCombination.contains(comb3Be));
        assertTrue(filteredNaCombination.contains(comb2BeNl));

        // filter - max 1 RA in Fr and max 2 RA in BE
        Map<String, Integer> maxRaPerTso = Map.of("fr", 1, "be", 2);
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, new HashMap<>(), maxRaPerTso, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(indBe1));
        assertTrue(filteredNaCombination.contains(indNl1));
        assertTrue(filteredNaCombination.contains(comb2BeNl));

        // filter - max 2 topo in Fr, max 0 topo in Nl and max 1 RA in BE
        maxTopoPerTso = Map.of("fr", 2, "nl", 0);
        maxRaPerTso = Map.of("be", 1);
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, maxRaPerTso, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(2, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(indFr2));
        assertTrue(filteredNaCombination.contains(indFrDe));

        // filter - no RA in NL
        maxTopoPerTso = Map.of("fr", 10, "nl", 10, "be", 10);
        maxRaPerTso = Map.of("nl", 0);
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, maxTopoPerTso, maxRaPerTso, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfRaPerTso(naCombinations, previousLeaf);

        assertEquals(5, filteredNaCombination.size());
        assertFalse(filteredNaCombination.contains(indNl1));
        assertFalse(filteredNaCombination.contains(comb2BeNl));
        assertFalse(filteredNaCombination.contains(comb2FrNl));
    }

    @Test
    public void testRemoveCombinationsWhichExceedMaxNumberOfTsos() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr2, indBe1, indNl1, indFrDe, comb2Fr, comb3Be, comb2BeNl, comb2FrNl);

        // arrange previous Leaf -> naFr1 has already been activated
        Leaf previousLeaf = Mockito.mock(Leaf.class);
        Mockito.when(previousLeaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));

        // max 3 TSOs
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, 3, null, null, false, 0, new ArrayList<>());
        List<NetworkActionCombination> filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(8, filteredNaCombination.size()); // no combination filtered

        // max 2 TSOs
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, 2, null, null, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertFalse(filteredNaCombination.contains(comb2BeNl)); // one combination filtered

        // max 1 TSO
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, 1, null, null, false, 0, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsWhichExceedMaxNumberOfTsos(naCombinations, previousLeaf);

        assertEquals(3, filteredNaCombination.size());
        assertTrue(filteredNaCombination.contains(indFr2));
        assertTrue(filteredNaCombination.contains(indFrDe));
        assertTrue(filteredNaCombination.contains(comb2Fr));
    }

    @Test
    public void testRemoveNetworkActionsFarFromMostLimitingElement() {

        // arrange naCombination list
        List<NetworkActionCombination> naCombinations = List.of(indFr2, indDe1, indBe1, indNl1, indNlBe, indFrDe, indDeNl, comb3Be, comb2De, comb2FrDeBe, comb2BeNl);

        // arrange previous Leaf -> most limiting element is in DE/FR
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getVirtualCostNames()).thenReturn(Collections.emptySet());

        // test - no border cross, most limiting element is in BE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec1basecase"))); // be fr
        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), Integer.MAX_VALUE, Integer.MAX_VALUE, null, null, true, 0, new ArrayList<>());
        List<NetworkActionCombination> filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsAll(List.of(indFr2, indBe1, indNlBe, indFrDe, comb3Be, comb2FrDeBe, comb2BeNl)));

        // test - no border cross, most limiting element is in DE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnec2basecase"))); // de fr
        filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsAll(List.of(indFr2, indDe1, indFrDe, indDeNl, comb2De, comb2FrDeBe)));

        // test - max 1 border cross, most limiting element is in BE
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(crac.getFlowCnec("cnecBe"))); // be
        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, true, 1, new ArrayList<>());
        filteredNaCombination = bloomer.removeCombinationsFarFromMostLimitingElement(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size());
        assertTrue(filteredNaCombination.containsAll(List.of(indFr2, indBe1, indNl1, indNlBe, indFrDe, indDeNl, comb3Be, comb2FrDeBe, comb2BeNl)));
    }

    @Test
    public void testGetOptimizedMostLimitingElementsLocation() {

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());

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
    public void testIsNetworkActionCloseToLocations() {
        NetworkAction na1 = crac.newNetworkAction().withId("na")
            .newTopologicalAction().withNetworkElement("BBE2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add()
            .newFreeToUseUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withInstant(Instant.PREVENTIVE).add()
            .add();
        NetworkAction na2 = mock(NetworkAction.class);
        Mockito.when(na2.getLocation(network)).thenReturn(Set.of(Optional.of(Country.FR), Optional.empty()));

        HashSet<CountryBoundary> boundaries = new HashSet<>();
        boundaries.add(new CountryBoundary(Country.FR, Country.BE));
        boundaries.add(new CountryBoundary(Country.FR, Country.DE));
        boundaries.add(new CountryBoundary(Country.DE, Country.AT));
        CountryGraph countryGraph = new CountryGraph(boundaries);

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.empty()), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.FR)), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.BE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));
        assertTrue(bloomer.isNetworkActionCloseToLocations(na2, Set.of(Optional.of(Country.AT)), countryGraph));

        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, true, 1, new ArrayList<>());
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.DE)), countryGraph));
        assertFalse(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));

        bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, true, 2, new ArrayList<>());
        assertTrue(bloomer.isNetworkActionCloseToLocations(na1, Set.of(Optional.of(Country.AT)), countryGraph));
    }

    @Test
    public void testGetActivatedTsos() {
        RangeAction nonActivatedRa = createPstRangeActionWithOperator("NNL2AA1  NNL3AA1  1", "nl");
        Set<RangeAction> rangeActions = new HashSet<>();
        rangeActions.add(nonActivatedRa);
        rangeActions.add(raBe1);

        RangeActionResult prePerimeterRangeActionResult = Mockito.mock(RangeActionResult.class);
        Mockito.when(prePerimeterRangeActionResult.getOptimizedSetPoint(raBe1)).thenReturn(0.);
        Mockito.when(prePerimeterRangeActionResult.getOptimizedSetPoint(nonActivatedRa)).thenReturn(0.);

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.singleton(naFr1));
        Mockito.when(leaf.getRangeActions()).thenReturn(rangeActions);
        Mockito.when(leaf.getOptimizedSetPoint(raBe1)).thenReturn(5.);
        Mockito.when(leaf.getOptimizedSetPoint(nonActivatedRa)).thenReturn(0.);

        SearchTreeBloomer bloomer = new SearchTreeBloomer(network, mock(RangeActionResult.class), 0, Integer.MAX_VALUE, null, null, false, 0, new ArrayList<>());
        Set<String> activatedTsos = bloomer.getActivatedTsos(leaf);

        assertEquals(2, activatedTsos.size());
        assertTrue(activatedTsos.contains("fr"));
        assertTrue(activatedTsos.contains("be"));
    }

    private NetworkAction createNetworkActionWithOperator(String networkElementId, String operator) {
        return crac.newNetworkAction().withId("na - " + networkElementId).withOperator(operator)
            .newTopologicalAction().withNetworkElement(networkElementId).withActionType(ActionType.OPEN).add()
            .add();
    }

    private PstRangeAction createPstRangeActionWithOperator(String networkElementId, String operator) {
        Map<Integer, Double> conversionMap = new HashMap<>();
        conversionMap.put(0, 0.);
        conversionMap.put(1, 1.);
        return crac.newPstRangeAction().withId("pst - " + networkElementId).withOperator(operator).withNetworkElement(networkElementId)
            .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
            .withInitialTap(0)
            .withTapToAngleConversionMap(conversionMap)
            .add();
    }
}
