/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

import org.junit.Test;

/**
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracAliasesUtilTest {

    @Test
    public void testCracAliasesUtil() {
        Crac crac = CracImporters.importCrac("crac-for-aliases.json", getClass().getResourceAsStream("/crac-for-aliases.json"));
        Network network = Importers.loadNetwork("case-for-aliases.uct", getClass().getResourceAsStream("/case-for-aliases.uct"));

        CracImporters.cracAliasesUtil(crac, network);

        int i = 1;
    }
}
