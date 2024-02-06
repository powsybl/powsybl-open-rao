/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.cnec;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationTestUtil.getNetworkFromResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GeographicalFilterTest {

    private final Network network = getNetworkFromResource("/TestCase16Nodes_CGMES.zip");
    private final String frenchLineId = "FFR2AA1  FFR3AA1  2";
    private final String dutchBelgianLineId = "NNL2AA1  BBE3AA1  1";
    private final String belgianSwitchId = "BBE1AA1  BBE4AA1  1";

    @Test
    void getLocationsFromSingleElementFromOneCountry() {
        // Branch
        assertEquals(
            Set.of(Country.FR),
            GeographicalFilter.getNetworkElementLocation(frenchLineId, network)
        );
        // Switch
        assertEquals(
            Set.of(Country.BE),
            GeographicalFilter.getNetworkElementLocation(belgianSwitchId, network)
        );
        // Voltage Level
        assertEquals(
            Set.of(Country.BE),
            GeographicalFilter.getNetworkElementLocation("BBE1AA1", network)
        );
        // Substation
        assertEquals(
            Set.of(Country.DE),
            GeographicalFilter.getNetworkElementLocation("DDE3AA", network)
        );
        // Generator
        assertEquals(
            Set.of(Country.NL),
            GeographicalFilter.getNetworkElementLocation("NNL2AA1 _generator", network)
        );
        // Bus
        assertEquals(
            Set.of(Country.FR),
            GeographicalFilter.getNetworkElementLocation("FFR2AA1 ", network)
        );
        // Unknown network element
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> GeographicalFilter.getNetworkElementLocation("Unknown element", network));
        assertEquals("Network element Unknown element was not found in the network.", exception.getMessage());
    }

    @Test
    void getLocationsFromSingleElementFromTwoCountries() {
        assertEquals(
            Set.of(Country.BE, Country.NL),
            GeographicalFilter.getNetworkElementLocation(dutchBelgianLineId, network)
        );
    }

    @Test
    void getLocationsFromSetOfElements() {
        assertEquals(
            Set.of(Country.BE, Country.FR, Country.NL),
            GeographicalFilter.getNetworkElementsLocations(Set.of(frenchLineId, dutchBelgianLineId), network)
        );
    }

    @Test
    void setsOfNetworkElementsHaveCommonCountries() {
        assertTrue(GeographicalFilter.networkElementsShareCommonCountry(Set.of(dutchBelgianLineId, frenchLineId), Set.of(belgianSwitchId), network));
        assertFalse(GeographicalFilter.networkElementsShareCommonCountry(Set.of(frenchLineId), Set.of(belgianSwitchId), network));
    }
}
