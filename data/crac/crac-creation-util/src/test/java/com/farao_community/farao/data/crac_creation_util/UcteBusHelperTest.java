/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteBusHelperTest {

    /*
    @Test
    public void testExactMatch() {
        String node = "FFR1AA1 ";
        String candidate = "FFR1AA1 ";
        UcteBusHelper busHelper = new UcteBusHelper(node, candidate, false);
        assertTrue(busHelper.isValid());
        assertEquals("FFR1AA1 ", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        candidate = "FFR1AA11";
        busHelper = new UcteBusHelper(node, candidate, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
    }

    @Test
    public void testShortName() {
        String node = "FFR1AA1";
        String candidate = "FFR1AA11";
        UcteBusHelper busHelper = new UcteBusHelper(node, candidate, true);
        assertTrue(busHelper.isValid());
        assertEquals("FFR1AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        node = "FFR1A";
        candidate = "FFR1AA11";
        busHelper = new UcteBusHelper(node, candidate, true);
        assertTrue(busHelper.isValid());
        assertEquals("FFR1AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        node = "FFR2A";
        candidate = "FFR1AA11";
        busHelper = new UcteBusHelper(node, candidate, true);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        node = "FFR1AA1";
        candidate = "FFR1AA11";
        busHelper = new UcteBusHelper(node, candidate, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        node = "FFR1AA1";
        candidate = "FFR1AA1 ";
        busHelper = new UcteBusHelper(node, candidate, true);
        assertTrue(busHelper.isValid());
        assertEquals("FFR1AA1 ", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());
    }

    @Test
    public void testWildCard() {
        String node = "FFR1AA1*";
        String candidate = "FFR1AA11";
        UcteBusHelper busHelper = new UcteBusHelper(node, candidate, false);
        assertTrue(busHelper.isValid());
        assertEquals("FFR1AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        candidate = "FFR1AA21";
        busHelper = new UcteBusHelper(node, candidate, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        candidate = "FFR1AA*";
        busHelper = new UcteBusHelper(node, candidate, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
    }

    @Test
    public void testWithNetwork() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));

        UcteBusHelper busHelper = new UcteBusHelper("DDE2AA1*", network, false);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE2AA", network, true);
        assertTrue(busHelper.isValid());
        assertEquals("DDE2AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE2AA", network, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        busHelper = new UcteBusHelper("DDE1AA11", network, false);
        assertTrue(busHelper.isValid());
        assertEquals("DDE1AA11", busHelper.getBusIdInNetwork());
        assertNull(busHelper.getInvalidReason());

        // doesn't exist
        busHelper = new UcteBusHelper("AAAAAAAA", network, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());

        // Too many matches
        busHelper = new UcteBusHelper("DDE1AA1*", network, false);
        assertFalse(busHelper.isValid());
        assertNull(busHelper.getBusIdInNetwork());
        assertNotNull(busHelper.getInvalidReason());
    }


     */
}
