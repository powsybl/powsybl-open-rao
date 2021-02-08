/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CountryGraphTest {

    @Test
    public void testGraphOnSmallNetwork() {

        Network network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        CountryGraph graph = new CountryGraph(network);

        // FR-FR
        assertTrue(graph.areNeighbors(Country.FR, Country.FR));
        assertTrue(graph.areNeighbors(Country.FR, Country.FR, 1));
        assertTrue(graph.areNeighbors(Country.FR, Country.FR, 2));

        // FR-BE
        assertTrue(graph.areNeighbors(Country.FR, Country.BE));
        assertTrue(graph.areNeighbors(Country.FR, Country.BE, 1));
        assertTrue(graph.areNeighbors(Country.FR, Country.BE, 2));

        // FR-NL
        assertFalse(graph.areNeighbors(Country.FR, Country.NL));
        assertFalse(graph.areNeighbors(Country.FR, Country.NL, 1));
        assertTrue(graph.areNeighbors(Country.FR, Country.NL, 2));

        // 0 or negative max number of boundaries
        assertFalse(graph.areNeighbors(Country.FR, Country.BE, 0));
        assertFalse(graph.areNeighbors(Country.FR, Country.BE, -10));
    }
}
