/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.csv_exporter;

import com.powsybl.iidm.network.Country;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NeighbouringCountryPairsInCoreTest {

    @Test
    public void testBelongs() {
        assertTrue(NeighbouringCountryPairsInCore.belongs(Country.BE.getName(), Country.FR.getName()));
        assertTrue(NeighbouringCountryPairsInCore.belongs(Country.FR.getName(), Country.BE.getName()));
        assertTrue(NeighbouringCountryPairsInCore.belongs(Country.HU.getName(), Country.SK.getName()));
        assertFalse(NeighbouringCountryPairsInCore.belongs(Country.NL.getName(), Country.RO.getName()));
        assertFalse(NeighbouringCountryPairsInCore.belongs(Country.DE.getName(), Country.SI.getName()));
        assertFalse(NeighbouringCountryPairsInCore.belongs(Country.FR.getName(), Country.ES.getName()));
        assertFalse(NeighbouringCountryPairsInCore.belongs("unknown1", "unknown2"));
    }
}
