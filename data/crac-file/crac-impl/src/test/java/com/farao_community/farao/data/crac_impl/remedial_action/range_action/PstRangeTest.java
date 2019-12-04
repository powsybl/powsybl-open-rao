/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.PstSetpoint;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeTest {

    @Test
    public void apply() {
        Network network = Importers.loadNetwork(
            "TestCase12Nodes.uct",
            getClass().getResourceAsStream("/TestCase12Nodes.uct")
        );
        PstRange pstRange = new PstRange(new NetworkElement("BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1"));

        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        pstRange.apply(network, 12);
        assertEquals(12, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }
}