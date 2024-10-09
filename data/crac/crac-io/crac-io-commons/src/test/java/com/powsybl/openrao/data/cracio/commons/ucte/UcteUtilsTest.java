/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.commons.ucte;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class UcteUtilsTest {

    @Test
    void testExactMatch() {
        String node = "FFR1AA1 ";
        String candidate = "FFR1AA1 ";
        assertTrue(UcteUtils.matchNodeNames(node, candidate));

        candidate = "FFR1AA11";
        assertFalse(UcteUtils.matchNodeNames(node, candidate));
    }

    @Test
    void testShortName() {
        String node = "FFR1AA1";
        String candidate = "FFR1AA11";
        assertFalse(UcteUtils.matchNodeNames(node, candidate));
    }

    @Test
    void testWildCard() {
        String node = "FFR1AA1*";
        String candidate = "FFR1AA11";
        assertTrue(UcteUtils.matchNodeNames(node, candidate));

        candidate = "FFR1AA21";
        assertFalse(UcteUtils.matchNodeNames(node, candidate));

        candidate = "FFR1AA**";
        assertFalse(UcteUtils.matchNodeNames(node, candidate)); // wildcard only works on last character
    }
}
