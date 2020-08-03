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

        assertSame(network.getBranch("DDE2AA   DDE3AA   1"), network.getBranch("DDE2AA   DDE3AA   ROT"));
        assertSame(network.getBranch("DDE3AA   X_DEFR1  1"), network.getBranch("FFR2AA   X_DEFR1  1"));
    }
}