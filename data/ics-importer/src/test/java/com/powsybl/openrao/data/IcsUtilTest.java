/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class IcsUtilTest {

    @Test
    public void testUpdateNominalVoltage() {
        String networkFilePath1 = "2Nodes2ParallelLinesPST_voltagelevel2.uct";
        Network network1 = Network.read(networkFilePath1, IcsDataTest.class.getResourceAsStream("/network/" + networkFilePath1));
        IcsUtil.updateNominalVoltage(network1);
        assertEquals(network1.getVoltageLevel("BBE1AA2").getNominalV(), 225.);

        String networkFilePath2 = "2Nodes2ParallelLinesPST_0030.uct";
        Network network2 = Network.read(networkFilePath2, IcsDataTest.class.getResourceAsStream("/network/" + networkFilePath2));
        IcsUtil.updateNominalVoltage(network2);
        assertEquals(network2.getVoltageLevel("BBE1AA1").getNominalV(), 400);
    }
}
