/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static junit.framework.TestCase.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteNetworkHelperTest {

    @Test
    public void testGetter() {
        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes.uct"));
        UcteNetworkHelperProperties properties = new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        UcteNetworkHelper networkHelper = new UcteNetworkHelper(network, properties);

        assertSame(network, networkHelper.getNetwork());
        assertSame(properties, networkHelper.getProperties());
    }

    @Test
    public void testBusIdMatchPolicy() {

        Network network = Importers.loadNetwork("TestCase_severalVoltageLevels_Xnodes_8characters.uct", getClass().getResourceAsStream("/TestCase_severalVoltageLevels_Xnodes_8characters.uct"));

        UcteNetworkHelperProperties properties = new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WILDCARDS);
        UcteNetworkHelper networkHelper = new UcteNetworkHelper(network, properties);
        UcteMatchingResult result = networkHelper.findNetworkElement("BBE1AA1", "BBE2AA1", "1");
        assertTrue(result.hasMatched()); // branch BBE1AA11 BBE2AA11 1

        properties = new UcteNetworkHelperProperties(UcteNetworkHelperProperties.BusIdMatchPolicy.COMPLETE_WITH_WHITESPACES);
        networkHelper = new UcteNetworkHelper(network, properties);
        result = networkHelper.findNetworkElement("BBE1AA1", "BBE2AA1", "1");
        assertFalse(result.hasMatched());
    }
}
