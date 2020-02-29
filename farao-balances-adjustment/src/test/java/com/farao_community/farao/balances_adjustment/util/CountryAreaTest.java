/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CountryAreaTest {

    private Network testNetwork1;
    private Network testNetwork2;

    private CountryArea countryAreaFR;
    private CountryArea countryAreaBE;
    private CountryArea countryAreaES;

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", CountryAreaTest.class.getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        countryAreaFR = new CountryArea(Country.FR);
        countryAreaBE = new CountryArea(Country.BE);
        countryAreaES = new CountryArea(Country.ES);

    }

    private boolean checkSameList(List ls1, List ls2) {
        return ls1.size() == ls2.size() && ls1.containsAll(ls2);
    }

    @Test
    public void testGetAreaVoltageLevels() {
        List<VoltageLevel> voltageLevels = testNetwork1.getVoltageLevelStream().filter(v -> v.getSubstation().getCountry().get().equals(countryAreaFR.getCountry()))
                .collect(Collectors.toList());

        List<VoltageLevel> voltageLevelsFR = countryAreaFR.getAreaVoltageLevels(testNetwork1);
        assertTrue(checkSameList(voltageLevels, voltageLevelsFR));

        voltageLevels = testNetwork1.getVoltageLevelStream().filter(v -> v.getSubstation().getCountry().get().equals(countryAreaBE.getCountry()))
                .collect(Collectors.toList());
        List<VoltageLevel> voltageLevelsBE = countryAreaBE.getAreaVoltageLevels(testNetwork1);
        assertTrue(checkSameList(voltageLevels, voltageLevelsBE));

    }

    @Test
    public void testGetBorderDevices() {
        //Test BranchBorder
        List<BorderDevice> borderDevicesES = countryAreaES.getBorderDevices(testNetwork1);
        assertTrue(borderDevicesES.isEmpty());

        List<BorderDevice> borderDevicesFR = countryAreaFR.getBorderDevices(testNetwork1);
        assertEquals(2, borderDevicesFR.size());
        assertEquals("FFR2AA1  DDE3AA1  1", borderDevicesFR.get(0).getId());
        assertEquals("BBE2AA1  FFR3AA1  1", borderDevicesFR.get(1).getId());

        // Test HVDCLines
        assertEquals(1, testNetwork2.getHvdcLineCount());
        List<BorderDevice> borderDevicesFR2 = countryAreaFR.getBorderDevices(testNetwork2);
        assertEquals(1, borderDevicesFR2.size());
        assertEquals("hvdcLineFrEs", borderDevicesFR2.get(0).getId());

        List<BorderDevice> borderDevicesES2 = countryAreaES.getBorderDevices(testNetwork2);
        assertEquals(1, borderDevicesES2.size());
        assertEquals("hvdcLineFrEs", borderDevicesES2.get(0).getId());

    }

    private Stream<Injection> getInjectionStream(Network network) {
        Stream returnStream = Stream.empty();
        returnStream = Stream.concat(network.getGeneratorStream(), returnStream);
        returnStream = Stream.concat(network.getLoadStream(), returnStream);
        returnStream = Stream.concat(network.getDanglingLineStream(), returnStream);
        return returnStream;
    }

    private double getSumFlowCountry(Network network, Country country) {
        double sumFlow = 0;
        List<Injection> injections = getInjectionStream(network).filter(i -> country.equals(i.getTerminal().getVoltageLevel().getSubstation().getCountry().get()))
                .collect(Collectors.toList());
        for (Injection injection : injections) {
            sumFlow += injection.getTerminal().getBusBreakerView().getBus().isInMainConnectedComponent() ? injection.getTerminal().getP() : 0;

        }
        return sumFlow;
    }

    @Test
    public void testGetNetPosition() {
        //Test network with BranchBorder
        assertEquals(0, countryAreaES.getNetPosition(testNetwork1), 1e-3);

        assertEquals(-getSumFlowCountry(testNetwork1, Country.FR), countryAreaFR.getNetPosition(testNetwork1), 1e-3);

        //Test network with HVDCLines
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation1().getTerminal().getP(), countryAreaFR.getNetPosition(testNetwork2), 1e-3);
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation2().getTerminal().getP(), countryAreaES.getNetPosition(testNetwork2), 1e-3);
    }

    @Test
    public void testEquals() {
        List<CountryArea> countries = Arrays.asList(countryAreaBE, countryAreaES, countryAreaFR);
        CountryArea countryAreaFr2 = new CountryArea(Country.valueOf("FR"));
        assertTrue(countries.contains(countryAreaFr2));
        assertTrue(!countryAreaFR.equals(countryAreaES));
        assertTrue(countryAreaFR.equals(countryAreaFR));
    }
}
