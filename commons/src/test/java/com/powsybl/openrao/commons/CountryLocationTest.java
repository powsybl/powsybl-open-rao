/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.iidm.network.VoltageLevel;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CountryLocationTest {
    @Test
    void testGetLocation() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));

        Set<Country> countries;

        // Branch
        countries = CountryLocation.getLocation(network, "FFR2AA1  DDE3AA1  1");
        assertEquals(Set.of(Country.FR, Country.DE), countries);

        // Branch
        countries = CountryLocation.getLocation(network, "BBE2AA1  BBE3AA1  1");
        assertEquals(Set.of(Country.BE), countries);

        // Switch
        countries = CountryLocation.getLocation(network, "NNL3AA11 NNL3AA12 1");
        assertEquals(Set.of(Country.NL), countries);

        // Generator
        countries = CountryLocation.getLocation(network, "FFR1AA1 _generator");
        assertEquals(Set.of(Country.FR), countries);

        // Load
        countries = CountryLocation.getLocation(network, "NNL1AA1 _load");
        assertEquals(Set.of(Country.NL), countries);

        // Bus
        countries = CountryLocation.getLocation(network, "NNL2AA1 ");
        assertEquals(Set.of(Country.NL), countries);

        // Voltage level
        countries = CountryLocation.getLocation(network, "BBE1AA1");
        assertEquals(Set.of(Country.BE), countries);

        // Substation
        countries = CountryLocation.getLocation(network, "DDE3AA");
        assertEquals(Set.of(Country.DE), countries);
    }

    @Test
    void testGetLocationAbsent() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        assertThrows(OpenRaoException.class, () -> CountryLocation.getLocation(network, "non-existent"));
    }

    @Test
    void testGetLocationOnUnsupportedType() {
        Network network = Network.read("TestCase12NodesWithSwitch.uct", getClass().getResourceAsStream("/TestCase12NodesWithSwitch.uct"));
        assertThrows(NotImplementedException.class, () -> CountryLocation.getLocation(network, "TestCase12NodesWithSwitch"));
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

        assertTrue(CountryLocation.getLocation(network, "switch").isEmpty());
    }
}
