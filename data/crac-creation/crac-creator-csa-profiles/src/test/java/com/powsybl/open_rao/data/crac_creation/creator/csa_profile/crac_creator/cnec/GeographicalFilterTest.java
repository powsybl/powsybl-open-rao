package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.cnec;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.powsybl.open_rao.data.crac_creation.creator.csa_profile.crac_creator.CsaProfileCracCreationTestUtil.getNetworkFromResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeographicalFilterTest {

    private final Network network = getNetworkFromResource("/TestCase16Nodes.zip");
    private final GeographicalFilter geographicalFilter = new GeographicalFilter(network);
    private final String frenchLineId = "FFR2AA1  FFR3AA1  2";
    private final String dutchBelgianLineId = "NNL2AA1  BBE3AA1  1";
    private final String belgianSwitchId = "BBE1AA1  BBE4AA1  1";

    @Test
    void getLocationsFromSingleElementFromOneCountry() {
        assertCountriesSetEquality(
                Set.of(Country.FR),
                geographicalFilter.getNetworkElementLocation(frenchLineId)
        );
        assertCountriesSetEquality(
                Set.of(Country.BE),
                geographicalFilter.getNetworkElementLocation(belgianSwitchId)
        );
    }

    @Test
    void getLocationsFromSingleElementFromTwoCountries() {
        assertCountriesSetEquality(
                Set.of(Country.BE, Country.NL),
                geographicalFilter.getNetworkElementLocation(dutchBelgianLineId)
        );
    }

    @Test
    void getLocationsFromSetOfElements() {
        assertCountriesSetEquality(
                Set.of(Country.BE, Country.FR, Country.NL),
                geographicalFilter.getNetworkElementsLocations(Set.of(frenchLineId, dutchBelgianLineId))
        );
    }

    @Test
    void setsOfNetworkElementsHaveCommonCountries() {
        assertTrue(geographicalFilter.networkElementsShareCommonCountry(Set.of(dutchBelgianLineId, frenchLineId), Set.of(belgianSwitchId)));
        assertFalse(geographicalFilter.networkElementsShareCommonCountry(Set.of(frenchLineId), Set.of(belgianSwitchId)));
    }

    private void assertCountriesSetEquality(Set<Country> expectedSet, Set<Country> actualSet) {
        assertEquals(expectedSet.size(), actualSet.size());
        actualSet.forEach(country -> assertTrue(expectedSet.contains(country)));
    }
}
