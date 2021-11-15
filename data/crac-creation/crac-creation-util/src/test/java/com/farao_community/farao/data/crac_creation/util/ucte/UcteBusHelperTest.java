/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation.util.ucte;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelperTest {

    @Test
    public void testReplaceWithWhiteSpacesOrWildcards() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));
        UcteNetworkAnalyzer ucteNetworkAnalyzerWhiteSpaces = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES));
        UcteNetworkAnalyzer ucteNetworkAnalyzerWildCards = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS));

        UcteBusHelper busHelper = new UcteBusHelper("DDE2AA1*", ucteNetworkAnalyzerWhiteSpaces);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE2AA1", ucteNetworkAnalyzerWildCards);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE2AA1", ucteNetworkAnalyzerWhiteSpaces);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE1AA11", ucteNetworkAnalyzerWhiteSpaces);
        assertTrue(busHelper.isValid());
        assertEquals("DDE1AA11", busHelper.getIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        // doesn't exist
        busHelper = new UcteBusHelper("AAAAAAAA", ucteNetworkAnalyzerWhiteSpaces);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        // Too many matches
        busHelper = new UcteBusHelper("DDE1AA1*", ucteNetworkAnalyzerWhiteSpaces);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
    }

    @Test
    public void testReplace8thByWildcard() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));
        UcteNetworkAnalyzer ucteNetworkAnalyzer = new UcteNetworkAnalyzer(network, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));

        // bus found with exact name
        UcteBusHelper busHelper = new UcteBusHelper("NNL2AA13", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("NNL2AA13", busHelper.getIdInNetwork());

        // bus found replacing the 8th character by wildcard
        busHelper = new UcteBusHelper("NNL2AA18", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("NNL2AA13", busHelper.getIdInNetwork());

        // bus found with exact name, even if several bus exist with same first seven characters
        busHelper = new UcteBusHelper("DDE1AA12", ucteNetworkAnalyzer);
        assertTrue(busHelper.isValid());
        assertEquals("DDE1AA12", busHelper.getIdInNetwork());

        // bus not found when replacing 8th character by *, cause of too many matches
        busHelper = new UcteBusHelper("DDE1AA13", ucteNetworkAnalyzer);
        assertFalse(busHelper.isValid());
        assertTrue(busHelper.getInvalidReason().contains("Too many buses"));

        // bus not found, as no bus of the network match the 7th first character
        busHelper = new UcteBusHelper("RANDOM12", ucteNetworkAnalyzer);
        assertFalse(busHelper.isValid());
        assertTrue(busHelper.getInvalidReason().contains("No bus"));
    }
}
