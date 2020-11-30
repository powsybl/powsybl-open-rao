/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class CracCleanerTest {

    @Test
    public void testCracAliasesUtil() {
        Crac crac = CracImporters.importCrac("crac-for-aliases.json", getClass().getResourceAsStream("/crac-for-aliases.json"));
        Network network = Importers.loadNetwork("case-for-aliases.uct", getClass().getResourceAsStream("/case-for-aliases.uct"));

        network.getBranch("FFR2AA1H DDE3AA1F 1").addAlias("FFR2AA1H DDE3AA1F HFSK JDV");
        network.getBranch("DDE1AA1D DDE2AA1E 1").addAlias("DDE2AA1E DDE1AA1D DLJKSC H");

        CracCleaner.cracAliasesUtil(crac, network);

        assertEquals(1, network.getBranch("FFR1AA1G FFR3AA1I 1").getAliases().size());
        assertEquals(2, network.getBranch("DDE1AA1D DDE2AA1E 1").getAliases().size());
        assertEquals(3, network.getBranch("FFR2AA1H DDE3AA1F 1").getAliases().size());
    }
}
