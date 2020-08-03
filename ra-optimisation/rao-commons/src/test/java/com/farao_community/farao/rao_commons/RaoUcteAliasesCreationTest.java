/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RaoUcteAliasesCreationTest {

    @Test
    public void createAliases() {
        Network network = Importers.loadNetwork("TestCase12Nodes_withXnodes.uct", getClass().getResourceAsStream("/TestCase12Nodes_withXnodes.uct"));
        RaoUcteAliasesCreation.createAliases(network);

        // Working when one node has no bus bar specified (8th character is white space) "BBE2AA1 "
        assertNotNull(network.getBranch("BBE1AA11 BBE2AA1  1")); // UCTE ID
        assertSame(network.getBranch("BBE1AA11 BBE2AA1  1"), network.getBranch("BBE1AA1  BBE2AA1  1"));

        // Working when both nodes have specification on bus bar and the branch has element name
        assertNotNull(network.getBranch("DDE2AA11 DDE3AA11 1")); // UCTE ID
        assertSame(network.getBranch("DDE2AA11 DDE3AA11 1"), network.getBranch("DDE2AA1  DDE3AA1  1")); // Short ID with order code
        assertSame(network.getBranch("DDE2AA11 DDE3AA11 1"), network.getBranch("DDE2AA1  DDE3AA1  ROT")); // Short ID with element name

        // Working on branch with x_node
        assertNotNull(network.getBranch("DDE3AA11 X_DEFR11 1")); // UCTE ID
        assertSame(network.getBranch("DDE3AA11 X_DEFR11 1"), network.getBranch("FFR2AA1  X_DEFR1  1")); // French side short ID
        assertSame(network.getBranch("DDE3AA11 X_DEFR11 1"), network.getBranch("DDE3AA1  X_DEFR1  1")); // German side short ID
    }
}
