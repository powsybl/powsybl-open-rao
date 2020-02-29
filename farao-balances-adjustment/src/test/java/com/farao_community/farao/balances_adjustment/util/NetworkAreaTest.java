/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class NetworkAreaTest {

    private Network testNetwork1;
    private NetworkArea countryAreaFR;

    private NetworkArea voltageLevelsArea1;

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", NetworkAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        List<VoltageLevel> voltageLevels1 = testNetwork1.getVoltageLevelStream().filter(v -> v.getId().equals("FFR1AA1") || v.getId().equals("FFR3AA1"))
                .collect(Collectors.toList());

        voltageLevelsArea1 = new VoltageLevelsArea("Area1", voltageLevels1);

        countryAreaFR = new CountryArea(Country.FR);

    }

    private boolean checkSameList(List ls1, List ls2) {
        return ls1.size() == ls2.size() && ls1.containsAll(ls2);
    }

    @Test
    public void testGetAreaVoltageLevels() {

        assertTrue(checkSameList(voltageLevelsArea1.getAreaVoltageLevels(testNetwork1), countryAreaFR.getAreaVoltageLevels(testNetwork1)));
    }

    @Test
    public void testGetBorderDevices() {
        List<BorderDevice> borderDevices1 = voltageLevelsArea1.getBorderDevices(testNetwork1);
        List<BorderDevice> borderDevices2 = countryAreaFR.getBorderDevices(testNetwork1);
        assertTrue(checkSameList(borderDevices1.stream().map(BorderDevice::getId).collect(Collectors.toList()), borderDevices2.stream().map(BorderDevice::getId).collect(Collectors.toList())));

    }

    @Test
    public void testGetNetPosition() {
        assertEquals(countryAreaFR.getNetPosition(testNetwork1), voltageLevelsArea1.getNetPosition(testNetwork1), 1e-3);
    }

}
