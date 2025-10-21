/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.CountryBoundary;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.PREVENTIVE_INSTANT_ID;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_BE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_DE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_2_FR_DE_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.COMB_3_BE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_BE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_DE_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_DE_NL;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_2;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_FR_DE;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_1;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.IND_NL_BE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FarFromMostLimitingElementFilterTest {
    @Test
    void testRemoveNetworkActionsFarFromMostLimitingElement() {
        // arrange naCombination list
        Set<NetworkActionCombination> naCombinations = new HashSet<>(Set.of(IND_FR_2, IND_DE_1, IND_BE_1, IND_NL_1, IND_NL_BE, IND_FR_DE, IND_DE_NL, COMB_3_BE, COMB_2_DE, COMB_2_FR_DE_BE, COMB_2_BE_NL));

        // arrange previous Leaf -> most limiting element is in DE/FR
        Leaf previousLeaf = mock(Leaf.class);
        Mockito.when(previousLeaf.getVirtualCostNames()).thenReturn(Collections.emptySet());

        FarFromMostLimitingElementFilter naFilter;
        Set<NetworkActionCombination> filteredNaCombination;

        // test - no border cross, most limiting element is in BE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(NetworkActionCombinationsUtils.CRAC.getFlowCnec("cnec1basecase"))); // be fr
        naFilter = new FarFromMostLimitingElementFilter(NetworkActionCombinationsUtils.NETWORK, 0);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(7, filteredNaCombination.size());
        List<NetworkActionCombination> list1 = List.of(IND_FR_2, IND_BE_1, IND_NL_BE, IND_FR_DE, COMB_3_BE, COMB_2_FR_DE_BE, COMB_2_BE_NL);
        Set<NetworkActionCombination> finalFilteredNaCombination = filteredNaCombination;
        list1.forEach(na -> assertTrue(finalFilteredNaCombination.contains(na)));

        // test - no border cross, most limiting element is in DE/FR
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(NetworkActionCombinationsUtils.CRAC.getFlowCnec("cnec2basecase"))); // de fr
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(6, filteredNaCombination.size());
        List<NetworkActionCombination> list2 = List.of(IND_FR_2, IND_DE_1, IND_FR_DE, IND_DE_NL, COMB_2_DE, COMB_2_FR_DE_BE);
        Set<NetworkActionCombination> finalFilteredNaCombination2 = filteredNaCombination;
        list2.forEach(na -> assertTrue(finalFilteredNaCombination2.contains(na)));

        // test - max 1 border cross, most limiting element is in BE
        Mockito.when(previousLeaf.getMostLimitingElements(1)).thenReturn(List.of(NetworkActionCombinationsUtils.CRAC.getFlowCnec("cnecBe"))); // be
        naFilter = new FarFromMostLimitingElementFilter(NetworkActionCombinationsUtils.NETWORK, 1);
        filteredNaCombination = naFilter.filter(naCombinations, previousLeaf);

        assertEquals(9, filteredNaCombination.size());
        List<NetworkActionCombination> list3 = List.of(IND_FR_2, IND_BE_1, IND_NL_1, IND_NL_BE, IND_FR_DE, IND_DE_NL, COMB_3_BE, COMB_2_FR_DE_BE, COMB_2_BE_NL);
        Set<NetworkActionCombination> finalFilteredNaCombination3 = filteredNaCombination;
        list3.forEach(na -> assertTrue(finalFilteredNaCombination3.contains(na)));
    }

    @Test
    void testIsNetworkActionCloseToLocations() {
        NetworkAction na1 = NetworkActionCombinationsUtils.CRAC.newNetworkAction().withId("na").newTerminalsConnectionAction().withNetworkElement("BBE2AA1  FFR3AA1  1").withActionType(ActionType.OPEN).add().newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add().add();
        NetworkAction na2 = mock(NetworkAction.class);
        Mockito.when(na2.getLocation(NetworkActionCombinationsUtils.NETWORK)).thenReturn(Set.of(Country.FR));

        HashSet<CountryBoundary> boundaries = new HashSet<>();
        boundaries.add(new CountryBoundary(Country.FR, Country.BE));
        boundaries.add(new CountryBoundary(Country.FR, Country.DE));
        boundaries.add(new CountryBoundary(Country.DE, Country.AT));
        CountryGraph countryGraph = new CountryGraph(boundaries);

        FarFromMostLimitingElementFilter naFilter;

        naFilter = new FarFromMostLimitingElementFilter(NetworkActionCombinationsUtils.NETWORK, 0);
        assertTrue(naFilter.isNetworkActionCloseToLocations(na1, Set.of(), countryGraph));
        assertTrue(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.FR), countryGraph));
        assertTrue(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.BE), countryGraph));
        assertFalse(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.DE), countryGraph));
        assertFalse(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.AT), countryGraph));
        assertFalse(naFilter.isNetworkActionCloseToLocations(na2, Set.of(Country.AT), countryGraph));

        naFilter = new FarFromMostLimitingElementFilter(NetworkActionCombinationsUtils.NETWORK, 1);
        assertTrue(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.DE), countryGraph));
        assertFalse(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.AT), countryGraph));
        assertFalse(naFilter.isNetworkActionCloseToLocations(na2, Set.of(Country.AT), countryGraph));

        naFilter = new FarFromMostLimitingElementFilter(NetworkActionCombinationsUtils.NETWORK, 2);
        assertTrue(naFilter.isNetworkActionCloseToLocations(na1, Set.of(Country.AT), countryGraph));
        assertTrue(naFilter.isNetworkActionCloseToLocations(na2, Set.of(Country.AT), countryGraph));
    }
}
