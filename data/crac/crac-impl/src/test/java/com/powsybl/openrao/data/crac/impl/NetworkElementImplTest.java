/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.iidm.network.*;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

class NetworkElementImplTest {

    @Test
    void testConstructorElementTest() {
        NetworkElementImpl networkElement = new NetworkElementImpl("basicElemId", "basicElemName");
        assertEquals("basicElemId", networkElement.getId());
        assertEquals("basicElemName", networkElement.getName());
        assertEquals("basicElemId", networkElement.toString());
    }

    @Test
    void testEqualsLimits() {
        NetworkElementImpl networkElement = new NetworkElementImpl("network-element-1", "name-1");
        assertEquals(networkElement, networkElement);
        assertNotNull(networkElement);
        assertNotEquals(1, networkElement);
    }

    @Test
    void testDifferent() {
        NetworkElementImpl networkElement1 = new NetworkElementImpl("network-element-1", "name-1");
        NetworkElementImpl networkElement2 = new NetworkElementImpl("network-element-2", "name-2");

        assertNotEquals(networkElement1, networkElement2);
    }

    @Test
    void testEqualWithDifferentNames() {
        NetworkElementImpl networkElement1 = new NetworkElementImpl("network-element-1", "name-1");
        NetworkElementImpl networkElement2 = new NetworkElementImpl("network-element-1", "name-2");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    void testEqualWithSameNames() {
        NetworkElementImpl networkElement1 = new NetworkElementImpl("network-element-1", "name-1");
        NetworkElementImpl networkElement2 = new NetworkElementImpl("network-element-1", "name-1");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    void testSimpleConstructor() {
        NetworkElementImpl networkElement = new NetworkElementImpl("network-element");

        assertEquals("network-element", networkElement.getId());
        assertEquals("network-element", networkElement.getName());
    }

    @Test
    void testHashCode() {
        NetworkElementImpl networkElement = new NetworkElementImpl("network-element");

        assertEquals("network-element".hashCode(), networkElement.hashCode());
    }

    @Test
    void testGetLocation() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));

        Set<Country> countries;

        // Branch
        countries = new NetworkElementImpl("FFR2AA1  DDE3AA1  1").getLocation(network);
        assertEquals(Set.of(Country.FR, Country.DE), countries);

        // Branch
        countries = new NetworkElementImpl("BBE2AA1  BBE3AA1  1").getLocation(network);
        assertEquals(Set.of(Country.BE), countries);

        // Switch
        countries = new NetworkElementImpl("NNL3AA11 NNL3AA12 1").getLocation(network);
        assertEquals(Set.of(Country.NL), countries);

        // Generator
        countries = new NetworkElementImpl("FFR1AA1 _generator").getLocation(network);
        assertEquals(Set.of(Country.FR), countries);

        // Load
        countries = new NetworkElementImpl("NNL1AA1 _load").getLocation(network);
        assertEquals(Set.of(Country.NL), countries);

        // Bus
        countries = new NetworkElementImpl("NNL2AA1 ").getLocation(network);
        assertEquals(Set.of(Country.NL), countries);

        // Voltage level
        countries = new NetworkElementImpl("BBE1AA1").getLocation(network);
        assertEquals(Set.of(Country.BE), countries);

        // Substation
        countries = new NetworkElementImpl("DDE3AA").getLocation(network);
        assertEquals(Set.of(Country.DE), countries);
    }

    @Test
    void testGetLocationAbsent() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        NetworkElementImpl networkElement = new NetworkElementImpl("non-existent");
        assertThrows(OpenRaoException.class, () -> networkElement.getLocation(network));
    }

    @Test
    void testGetLocationOnUnsupportedType() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        NetworkElementImpl networkElement = new NetworkElementImpl("TestCase12NodesWithSwitch");
        assertThrows(NotImplementedException.class, () -> networkElement.getLocation(network));
    }

    @Test
    void testGetLocationAbsentSubstation() {
        Network network = Mockito.mock(Network.class);
        Switch switchMock = Mockito.mock(Switch.class);
        VoltageLevel voltageLevel = Mockito.mock(VoltageLevel.class);
        Mockito.when(voltageLevel.getSubstation()).thenReturn(Optional.empty());
        Mockito.when(switchMock.getVoltageLevel()).thenReturn(voltageLevel);
        Identifiable identifiable = switchMock;
        Mockito.when(network.getIdentifiable("switch")).thenReturn(identifiable);

        NetworkElementImpl networkElement = new NetworkElementImpl("switch");
        assertTrue(networkElement.getLocation(network).isEmpty());
    }
}
