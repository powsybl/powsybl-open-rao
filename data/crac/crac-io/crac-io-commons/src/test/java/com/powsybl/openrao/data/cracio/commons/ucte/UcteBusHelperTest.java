/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.ucte;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteBusHelperTest {

    private void assertThrowsOnGetId(UcteBusHelper ucteBusHelper) {
        assertThrows(UnsupportedOperationException.class, () -> ucteBusHelper.getIdInNetwork());
    }

    @Test
    void testReplaceWithWhiteSpacesOrWildcards() {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));
        UcteNetworkAnalyzer ucteNetworkAnalyzerWhiteSpaces = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES));
        UcteNetworkAnalyzer ucteNetworkAnalyzerWildCards = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));

        UcteBusHelper busHelper = new UcteBusHelper("DDE2AA1*", ucteNetworkAnalyzerWhiteSpaces);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE2AA11")), busHelper.getBusMatchesInNetwork());

        busHelper = new UcteBusHelper("DDE2AA1", ucteNetworkAnalyzerWildCards);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE2AA11")), busHelper.getBusMatchesInNetwork());

        busHelper = new UcteBusHelper("DDE2AA1", ucteNetworkAnalyzerWhiteSpaces);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
        assertTrue(busHelper.getBusMatchesInNetwork().isEmpty());

        busHelper = new UcteBusHelper("DDE1AA11", ucteNetworkAnalyzerWhiteSpaces);
        assertTrue(busHelper.isValid());
        assertEquals("DDE1AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE1AA11")), busHelper.getBusMatchesInNetwork());

        // doesn't exist
        busHelper = new UcteBusHelper("AAAAAAAA", ucteNetworkAnalyzerWhiteSpaces);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
        assertTrue(busHelper.getBusMatchesInNetwork().isEmpty());

        // many matches
        busHelper = new UcteBusHelper("DDE1AA1*", ucteNetworkAnalyzerWhiteSpaces);
        assertTrue(busHelper.isValid());
        assertThrowsOnGetId(busHelper);
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE1AA11"), network.getIdentifiable("DDE1AA12")), busHelper.getBusMatchesInNetwork());
    }

    @Test
    void testReplace8thByWildcard() {
        Network network = Network.read("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));
        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));

        // bus found with exact name
        UcteBusHelper busHelper = new UcteBusHelper("NNL2AA13", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("NNL2AA13", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("NNL2AA13")), busHelper.getBusMatchesInNetwork());

        // bus found replacing the 8th character by wildcard
        busHelper = new UcteBusHelper("NNL2AA18", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("NNL2AA13", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("NNL2AA13")), busHelper.getBusMatchesInNetwork());

        // bus found with exact name, even if several bus exist with same first seven characters
        busHelper = new UcteBusHelper("DDE1AA12", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("DDE1AA12", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE1AA12")), busHelper.getBusMatchesInNetwork());

        // many matches when replacing 8th character by *
        busHelper = new UcteBusHelper("DDE1AA13", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertThrowsOnGetId(busHelper);
        assertNull(busHelper.getInvalidReason());
        assertEquals(Set.of(network.getIdentifiable("DDE1AA11"), network.getIdentifiable("DDE1AA12")), busHelper.getBusMatchesInNetwork());

        // bus not found, as no bus of the network match the 7th first character
        busHelper = new UcteBusHelper("RANDOM12", ucteNetworkAnalyzer);
        assertFalse(busHelper.isValid());
        assertTrue(busHelper.getInvalidReason().contains("No bus"));
        assertTrue(busHelper.getBusMatchesInNetwork().isEmpty());
        assertNull(busHelper.getIdInNetwork());
    }
}
