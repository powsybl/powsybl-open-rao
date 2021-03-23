/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class NetworkElementTest {

    @Test
    public void testConstructorElementTest() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        assertEquals("basicElemId", networkElement.getId());
        assertEquals("basicElemName", networkElement.getName());
        assertEquals("basicElemId", networkElement.toString());
    }

    @Test
    public void testDifferent() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-2", "name-2");

        assertNotEquals(networkElement1, networkElement2);
    }

    @Test
    public void testEqualWithDifferentNames() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-1", "name-2");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    public void testEqualWithSameNames() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-1", "name-1");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    public void testSimpleConstructor() {
        NetworkElement networkElement = new NetworkElement("network-element");

        assertEquals("network-element", networkElement.getId());
        assertEquals("network-element", networkElement.getName());
    }

    @Test
    public void testHashCode() {
        NetworkElement networkElement = new NetworkElement("network-element");

        assertEquals("network-element".hashCode(), networkElement.getId().hashCode());
    }

    @Test
    public void testGetLocation() {
        Network network = Importers.loadNetwork("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));

        Set<Optional<Country>> countries;

        // Branch
        countries = new NetworkElement("FFR2AA1  DDE3AA1  1").getLocation(network);
        assertEquals(2, countries.size());
        assertTrue(countries.contains(Optional.of(Country.FR)));
        assertTrue(countries.contains(Optional.of(Country.DE)));

        // Branch
        countries = new NetworkElement("BBE2AA1  BBE3AA1  1").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));

        // Switch
        countries = new NetworkElement("NNL3AA11 NNL3AA12 1").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.NL)));

        // Generator
        countries = new NetworkElement("FFR1AA1 _generator").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.FR)));

        // Load
        countries = new NetworkElement("NNL1AA1 _load").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.NL)));

        // Bus
        countries = new NetworkElement("NNL2AA1 ").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.NL)));

        // Voltage level
        countries = new NetworkElement("BBE1AA1").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.BE)));

        // Substation
        countries = new NetworkElement("DDE3AA").getLocation(network);
        assertEquals(1, countries.size());
        assertTrue(countries.contains(Optional.of(Country.DE)));
    }

    @Test(expected = FaraoException.class)
    public void testGetLocationAbsent() {
        Network network = Importers.loadNetwork("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        new NetworkElement("non-existent").getLocation(network);
    }

    @Test(expected = NotImplementedException.class)
    public void testGetLocationOnUnsupportedType() {
        Network network = Importers.loadNetwork("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        new NetworkElement("TestCase12NodesWithSwitch").getLocation(network);
    }
}
