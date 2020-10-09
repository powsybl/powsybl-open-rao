/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoPtdfParametersTest {

    private RaoPtdfParameters ptdfParameters;

    @Before
    public void setUp() {
        ptdfParameters = new RaoPtdfParameters();
    }

    @Test
    public void testGetName() {
        assertEquals("RaoPtdfParameters", ptdfParameters.getName());
    }

    @Test
    public void testSetBoundariesFromCountryCodes() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR-ES", "ES-PT"));
        ptdfParameters.setBoundariesFromCountryCodes(stringBoundaries);
        assertEquals(2, ptdfParameters.getBoundaries().size());
        assertTrue(ptdfParameters.getBoundaries().contains(new ImmutablePair<>(Country.FR, Country.ES)));
        assertTrue(ptdfParameters.getBoundaries().contains(new ImmutablePair<>(Country.ES, Country.PT)));
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException1() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FRANCE-SPAIN"));
        ptdfParameters.setBoundariesFromCountryCodes(stringBoundaries);
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException2() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR-ES-"));
        ptdfParameters.setBoundariesFromCountryCodes(stringBoundaries);
    }

    @Test (expected = FaraoException.class)
    public void testSetBoundariesFromCountryCodesException3() {
        List<String> stringBoundaries = new ArrayList<>(Arrays.asList("FR-YY"));
        ptdfParameters.setBoundariesFromCountryCodes(stringBoundaries);
    }

    @Test
    public void testGetBoundariesFromString() {
        List<Pair<Country, Country>> countryBoundaries = new ArrayList<>(
                Arrays.asList(new ImmutablePair<>(Country.BE, Country.FR), new ImmutablePair<>(Country.DE, Country.AT))
        );
        ptdfParameters.setBoundaries(countryBoundaries);
        assertEquals(2, ptdfParameters.getBoundariesAsString().size());
        assertTrue(ptdfParameters.getBoundariesAsString().contains("BE-FR"));
        assertTrue(ptdfParameters.getBoundariesAsString().contains("DE-AT"));
    }

  /*  @Test
    public void writeExtension() throws IOException {
        RaoParameters parameters = new RaoParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonRaoParameters::write, AbstractConverterTest::compareTxt, "/RaoParametersWithExtension.json");
    }*/

    /*@Test
    public void readExtension() throws IOException {
        RaoParameters parameters = JsonRaoParameters.read(getClass().getResourceAsStream("/RaoParametersWithBoundaries.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(RaoPtdfParameters.class));
        assertNotNull(parameters.getExtensionByName("rao-ptdf-parameters"));
    }*/
}
