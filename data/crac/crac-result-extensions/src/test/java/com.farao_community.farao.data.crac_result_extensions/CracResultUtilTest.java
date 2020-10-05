/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CracResultUtilTest {
    private Network testNetwork;
    private Crac testCracWithResult;

    @Before
    public void setUp() {
        testNetwork = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));
        testCracWithResult = CracImporters.importCrac("simple_crac_with_preventive_ras.json", getClass().getResourceAsStream("/simple_crac_with_preventive_ras.json"));
    }

    @Test
    public void checkCorrectApplicationOfPreventiveRemedialActions() {
        assertTrue(testNetwork.getBranch("NNL1AA1  NNL2AA1  1").getTerminal1().isConnected());
        assertTrue(testNetwork.getBranch("NNL1AA1  NNL2AA1  1").getTerminal2().isConnected());
        assertEquals(0, testNetwork.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        CracResultUtil.applyPreventiveRemedialActions(testNetwork, testCracWithResult, "postOptimisationResults-d98e1bba-05ef-46d9-8f47-fae6752991ea");
        assertFalse(testNetwork.getBranch("NNL1AA1  NNL2AA1  1").getTerminal1().isConnected());
        assertFalse(testNetwork.getBranch("NNL1AA1  NNL2AA1  1").getTerminal2().isConnected());
        assertEquals(-16, testNetwork.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());

    }
}
