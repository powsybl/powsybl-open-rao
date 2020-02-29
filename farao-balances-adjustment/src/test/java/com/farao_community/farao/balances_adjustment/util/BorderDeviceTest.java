/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BorderDeviceTest {

    private BorderDevice borderDevice;
    private BorderDevice borderDevice2;
    private BorderDevice borderDevice3;
    private BorderDevice borderDevice4;
    private Network testNetwork1;
    private Network testNetwork2;
    private CountryArea countryAreaFR = new CountryArea(Country.FR);
    private Branch branchBorder;
    private Branch branchNotBorder;
    private HvdcLine hvdcNetwork2;
    private ThreeWindingsTransformer threeWindingsTransformer;

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", CountryAreaTest.class.getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        countryAreaFR = new CountryArea(Country.FR);

        List<BorderDevice> borderDevicesFR = countryAreaFR.getBorderDevices(testNetwork1);

        assertEquals(2, borderDevicesFR.size());
        assertEquals("FFR2AA1  DDE3AA1  1", borderDevicesFR.get(0).getId());
        assertEquals("BBE2AA1  FFR3AA1  1", borderDevicesFR.get(1).getId());

        branchBorder = testNetwork1.getBranch("FFR2AA1  DDE3AA1  1");
        borderDevice = new BorderBranch(branchBorder);

        branchNotBorder = testNetwork1.getBranch("BBE1AA1  BBE2AA1  1");
        borderDevice2 = new BorderBranch(branchNotBorder);

        hvdcNetwork2 = testNetwork2.getHvdcLine("hvdcLineFrEs");
        borderDevice3 = new BorderHvdcLine(hvdcNetwork2);

        threeWindingsTransformer = testNetwork2.getThreeWindingsTransformer("twtEs1A");
        borderDevice4 = new BorderThreeWindingsTransformer(threeWindingsTransformer);

    }

    @Test
    public void testGetLeavingFlow() {
        assertEquals(testNetwork1.getBranch("FFR2AA1  DDE3AA1  1").getTerminal1().getP(), borderDevice.getLeavingFlow(testNetwork1, countryAreaFR), 1e-3);
        assertEquals(0, borderDevice2.getLeavingFlow(testNetwork1, countryAreaFR), 1e-3);
        assertEquals(0, borderDevice3.getLeavingFlow(testNetwork1, countryAreaFR), 1e-3);
    }

    @Test
    public void testGetId() {
        assertEquals("FFR2AA1  DDE3AA1  1", borderDevice.getId());
        assertEquals("BBE1AA1  BBE2AA1  1", borderDevice2.getId());
        assertEquals("hvdcLineFrEs", borderDevice3.getId());
    }

    //Test BorderBranch
    @Test
    public void testGetBranch() {
        assertEquals(branchBorder, ((BorderBranch) borderDevice).getBranch());

    }

    //Test BorderHvdcLine
    @Test
    public void testGetHvdcLine() {
        assertEquals(hvdcNetwork2, ((BorderHvdcLine) borderDevice3).getHvdcLine());
    }

    //Test BorderThreeWindingsTransformer
    @Test
    public void testGetThreeWindingsTransformer() {
        assertEquals(threeWindingsTransformer, ((BorderThreeWindingsTransformer) borderDevice4).getThreeWindingsTransformer());
    }
}
