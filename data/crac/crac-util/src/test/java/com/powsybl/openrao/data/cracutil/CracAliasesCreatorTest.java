/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracutil;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class CracAliasesCreatorTest {

    @Test
    void testDeprecatedCracExtensions1() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-1.json");
        assertThrows(OpenRaoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-1.json", inputStream, null));
    }

    @Test
    void testDeprecatedCracExtensions2() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-2.json");
        assertThrows(OpenRaoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-2.json", inputStream, null));
    }

    @Test
    void testDeprecatedCracExtensions3() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-3.json");
        assertThrows(OpenRaoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-3.json", inputStream, null));
    }

    @Test
    void testDeprecatedCracExtensions4() {
        // Extensions have been deprecated
        InputStream inputStream = getClass().getResourceAsStream("/deprecated-crac-for-aliases-4.json");
        assertThrows(OpenRaoException.class, () -> CracImporters.importCrac("deprecated-crac-for-aliases-4.json", inputStream, null));
    }
}
