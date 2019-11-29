/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class NetworkUtilTest {
    private Network testNetwork;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
    }

    @Test
    public void isBranchConnectedAndInMainSynchronous() {
        Branch testBranch = testNetwork.getBranch("BBE1AA1  BBE2AA1  1");
        assertTrue(NetworkUtil.isConnectedAndInMainSynchronous(testBranch));

        // Branch disconnected one side
        testBranch.getTerminal1().disconnect();
        assertFalse(NetworkUtil.isConnectedAndInMainSynchronous(testBranch));
        testBranch.getTerminal1().connect();
        assertTrue(NetworkUtil.isConnectedAndInMainSynchronous(testBranch));

        // Branch not in main synchronous component
        testNetwork.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().disconnect();
        testNetwork.getBranch("NNL2AA1  BBE3AA1  1").getTerminal2().disconnect();
        assertFalse(NetworkUtil.isConnectedAndInMainSynchronous(testBranch));

    }

    @Test
    public void isInjectionConnectedAndInMainSynchronous() {
        Injection testInjection = testNetwork.getLoad("BBE1AA1 _load");
        assertTrue(NetworkUtil.isConnectedAndInMainSynchronous(testInjection));

        // Injection disconnected
        testInjection.getTerminal().disconnect();
        assertFalse(NetworkUtil.isConnectedAndInMainSynchronous(testInjection));
        testInjection.getTerminal().connect();
        assertTrue(NetworkUtil.isConnectedAndInMainSynchronous(testInjection));

        // Injection not in main synchronous component
        testNetwork.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().disconnect();
        testNetwork.getBranch("NNL2AA1  BBE3AA1  1").getTerminal2().disconnect();
        assertFalse(NetworkUtil.isConnectedAndInMainSynchronous(testInjection));
    }

    @Test
    public void getNetworkInjectionStream() {
        assertEquals(24, NetworkUtil.getInjectionStream(testNetwork).count());
    }

    @Test
    public void getBusInjectionStream() {
        Load load = testNetwork.getLoad("DDE2AA1 _load");
        Bus specificBus = load.getTerminal().getBusView().getBus();
        assertEquals(2, NetworkUtil.getInjectionStream(specificBus).count());
        assertTrue(NetworkUtil.getInjectionStream(specificBus).anyMatch(injection -> injection.equals(load)));
    }

    @Test
    public void branchIsPst() {
        Branch twt = testNetwork.getBranch("BBE1AA1  BBE3AA1  2");
        assertTrue(NetworkUtil.branchIsPst(twt));

        Branch line = testNetwork.getBranch("BBE1AA1  BBE2AA1  1");
        assertFalse(NetworkUtil.branchIsPst(line));

        ((TwoWindingsTransformer) twt).getPhaseTapChanger().remove();
        assertFalse(NetworkUtil.branchIsPst(twt));
    }

    @Test
    public void getBranchSideCountry() {
        Branch line = testNetwork.getBranch("BBE1AA1  BBE2AA1  1");
        assertEquals(Country.BE, NetworkUtil.getBranchSideCountry(line, Branch.Side.ONE));
        assertEquals(Country.BE, NetworkUtil.getBranchSideCountry(line, Branch.Side.TWO));

        Branch interconnection = testNetwork.getBranch("FFR2AA1  DDE3AA1  1");
        assertEquals(Country.FR, NetworkUtil.getBranchSideCountry(interconnection, Branch.Side.ONE));
        assertEquals(Country.DE, NetworkUtil.getBranchSideCountry(interconnection, Branch.Side.TWO));
    }

    @Test
    public void getInjectionFrom() {
        Load load = testNetwork.getLoad("DDE2AA1 _load");
        assertEquals(load, NetworkUtil.getInjectionFrom(testNetwork, "DDE2AA1 _load"));

        Generator generator = testNetwork.getGenerator("DDE2AA1 _generator");
        assertEquals(generator, NetworkUtil.getInjectionFrom(testNetwork, "DDE2AA1 _generator"));

        assertNull(NetworkUtil.getInjectionFrom(testNetwork, "BBE1AA1  BBE2AA1  1"));
        assertNull(NetworkUtil.getInjectionFrom(testNetwork, "Invalid ID"));
    }
}
