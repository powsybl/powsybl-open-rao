/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class VoltageLevelsAreaTest {

    private Network testNetwork1;
    private VoltageLevelsArea voltageLevelsArea1;
    private List<VoltageLevel> voltageLevels1 = new ArrayList<>();

    private Network testNetwork2;
    private VoltageLevelsArea voltageLevelsArea2;
    private List<VoltageLevel> voltageLevels2 = new ArrayList<>();

    @Before
    public void setUp() {
        testNetwork1 = Importers.loadNetwork("testCase.xiidm", VoltageLevelsAreaTest.class.getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        voltageLevels1 = testNetwork1.getVoltageLevelStream().filter(v -> v.getId().equals("FFR1AA1") || v.getId().equals("DDE3AA1"))
                .collect(Collectors.toList());

        voltageLevelsArea1 = new VoltageLevelsArea("Area1", voltageLevels1);

        //test Network with ThreeWindingsTransformer
        voltageLevels2 = testNetwork2.getVoltageLevelStream().filter(v -> v.getId().equals("vlFr1A") || v.getId().equals("vlEs1B"))
                .collect(Collectors.toList());

        voltageLevelsArea2 = new VoltageLevelsArea("Area2", voltageLevels2);

    }

    private boolean checkSameList(List ls1, List ls2) {
        return ls1.size() == ls2.size() && ls1.containsAll(ls2);
    }

    @Test
    public void testGetAreaVoltageLevels() {
        assertTrue(checkSameList(voltageLevelsArea1.getAreaVoltageLevels(testNetwork1), voltageLevels1));
        List<VoltageLevel> voltageLevelsTest2 = new ArrayList<>(Arrays.asList(testNetwork2.getVoltageLevel("vlFr1A"), testNetwork2.getVoltageLevel("vlEs1B")));
        assertTrue(checkSameList(voltageLevelsArea2.getAreaVoltageLevels(testNetwork2), voltageLevelsTest2));
        assertEquals("Area2", voltageLevelsArea2.getName());
    }

    @Test
    public void testGetBorderDevices() {
        //test BorderBranch
        List<BorderDevice> borderDevices = voltageLevelsArea1.getBorderDevices(testNetwork1);
        assertEquals(4, borderDevices.size());
        assertEquals("FFR1AA1  FFR3AA1  1", borderDevices.get(0).getId());
        assertEquals("FFR2AA1  FFR3AA1  1", borderDevices.get(1).getId());
        assertEquals("DDE1AA1  DDE3AA1  1", borderDevices.get(2).getId());
        assertEquals("DDE2AA1  DDE3AA1  1", borderDevices.get(3).getId());

        //test BorderThreeWindingsTransformer and BorderHvdcLine
        List<BorderDevice> borderDevices2 = voltageLevelsArea2.getBorderDevices(testNetwork2);
        assertEquals(3, borderDevices2.size());
        assertEquals("twtFr1A", borderDevices2.get(0).getId());
        assertEquals("twtEs1A", borderDevices2.get(1).getId());
        assertEquals("hvdcLineFrEs", borderDevices2.get(2).getId());
    }

    @Test
    public void testGetNetPosition() {
        List<Double> flows = new ArrayList<>();
        flows.add(testNetwork1.getBranch("FFR1AA1  FFR3AA1  1").getTerminal1().getP());
        flows.add(testNetwork1.getBranch("FFR2AA1  FFR3AA1  1").getTerminal1().getP());
        flows.add(testNetwork1.getBranch("DDE1AA1  DDE3AA1  1").getTerminal2().getP());
        flows.add(testNetwork1.getBranch("DDE2AA1  DDE3AA1  1").getTerminal2().getP());

        assertEquals(flows.get(0), voltageLevelsArea1.getBorderDevices(testNetwork1).get(0).getLeavingFlow(testNetwork1, voltageLevelsArea1), 1e-3);
        assertEquals(flows.get(1), voltageLevelsArea1.getBorderDevices(testNetwork1).get(1).getLeavingFlow(testNetwork1, voltageLevelsArea1), 1e-3);
        assertEquals(flows.get(2), voltageLevelsArea1.getBorderDevices(testNetwork1).get(2).getLeavingFlow(testNetwork1, voltageLevelsArea1), 1e-3);
        assertEquals(flows.get(3), voltageLevelsArea1.getBorderDevices(testNetwork1).get(3).getLeavingFlow(testNetwork1, voltageLevelsArea1), 1e-3);

        assertEquals(flows.stream().mapToDouble(f -> f).sum(), voltageLevelsArea1.getNetPosition(testNetwork1), 1e-3);

        assertEquals(20, voltageLevelsArea2.getBorderDevices(testNetwork2).get(0).getLeavingFlow(testNetwork2, voltageLevelsArea2), 1e-3);
        assertEquals(-5, voltageLevelsArea2.getBorderDevices(testNetwork2).get(1).getLeavingFlow(testNetwork2, voltageLevelsArea2), 1e-3);
        assertEquals(100, voltageLevelsArea2.getBorderDevices(testNetwork2).get(2).getLeavingFlow(testNetwork2, voltageLevelsArea2), 1e-3);
    }
}
