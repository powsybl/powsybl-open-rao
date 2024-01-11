/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getNetworkFromResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GeographicalFilterTest {

    private final Network network = getNetworkFromResource("/TestCase16Nodes.zip");
    private final String frenchLineId = "FFR2AA1  FFR3AA1  2";
    private final String dutchBelgianLineId = "NNL2AA1  BBE3AA1  1";
    private final String belgianSwitchId = "BBE1AA1  BBE4AA1  1";

    @Test
    void getLocationsFromSingleElementFromOneCountry() {
        assertEquals(
                Set.of(Country.FR),
                GeographicalFilter.getNetworkElementLocation(frenchLineId, network)
        );
        assertEquals(
                Set.of(Country.BE),
                GeographicalFilter.getNetworkElementLocation(belgianSwitchId, network)
        );
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
