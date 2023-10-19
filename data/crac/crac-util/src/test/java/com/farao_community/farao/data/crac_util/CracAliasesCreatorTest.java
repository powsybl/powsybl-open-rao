/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class CracAliasesCreatorTest {

    @Test
    void testDeprecatedCracExtensions1() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-1.json");
        FaraoException exception = assertThrows(FaraoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-1.json", inputStream));
        assertEquals("Extensions are deprecated since CRAC version 1.7", exception.getMessage());
    }

    @Test
    void testDeprecatedCracExtensions2() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-2.json");
        FaraoException exception = assertThrows(FaraoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-2.json", inputStream));
        assertEquals("Extensions are deprecated since CRAC version 1.7", exception.getMessage());
    }

    @Test
    void testDeprecatedCracExtensions3() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-3.json");
        FaraoException exception = assertThrows(FaraoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-3.json", inputStream));
        assertEquals("FlowCnec cnec3curId does not exist in crac. Consider adding it first.", exception.getMessage()); // TODO review this
    }

    @Test
    void testDeprecatedCracExtensions4() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-4.json");
        FaraoException exception = assertThrows(FaraoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-4.json", inputStream));
        assertEquals("Contingency contingency1Id of OnContingencyState usage rule does not exist in the crac. Use crac.newContingency() first.", exception.getMessage()); // TODO review this
    }

    @Test
    void testCracAliasesUtil7Char() {
        // Extensions have been deprecated
        Crac crac = CracImporters.importCrac("crac-for-aliases.json", getClass().getResourceAsStream("/crac-for-aliases.json"));
        Network network = Network.read("case-for-aliases.uct", getClass().getResourceAsStream("/case-for-aliases.uct"));

        network.getBranch("FFR2AA1H DDE3AA1F 1").addAlias("FFR2AA1H DDE3AA1F HFSK JDV");
        network.getBranch("DDE1AA1D DDE2AA1E 1").addAlias("DDE2AA1E DDE1AA1D DLJKSC H");

        CracAliasesCreator cracAliasesCreator = new CracAliasesCreator();
        cracAliasesCreator.createAliases(crac, network, UcteNodeMatchingRule.FIRST_7_CHARACTER_EQUAL);

        assertEquals(1, network.getBranch("FFR1AA1G FFR3AA1I 1").getAliases().size());
        assertEquals(2, network.getBranch("DDE1AA1D DDE2AA1E 1").getAliases().size());
        assertEquals(3, network.getBranch("FFR2AA1H DDE3AA1F 1").getAliases().size());
    }

    @Test
    void testCracAliasesUtil8Char() {
        Crac crac = CracImporters.importCrac("crac-for-aliases.json", getClass().getResourceAsStream("/crac-for-aliases.json"));
        Network network = Network.read("case-for-aliases.uct", getClass().getResourceAsStream("/case-for-aliases.uct"));

        network.getBranch("FFR2AA1H DDE3AA1F 1").addAlias("FFR2AA1H DDE3AA1F HFSK JDV");
        network.getBranch("DDE1AA1D DDE2AA1E 1").addAlias("DDE2AA1E DDE1AA1D DLJKSC H");

        CracAliasesCreator cracAliasesCreator = new CracAliasesCreator();
        cracAliasesCreator.createAliases(crac, network, UcteNodeMatchingRule.ALL_8_CHARACTER_EQUAL);

        assertEquals(0, network.getBranch("FFR1AA1G FFR3AA1I 1").getAliases().size());
        assertEquals(1, network.getBranch("DDE1AA1D DDE2AA1E 1").getAliases().size());
        assertEquals(1, network.getBranch("FFR2AA1H DDE3AA1F 1").getAliases().size());
    }
}
