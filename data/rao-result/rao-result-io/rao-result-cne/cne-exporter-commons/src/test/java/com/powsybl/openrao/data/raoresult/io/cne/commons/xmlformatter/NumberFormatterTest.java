/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.commons.xmlformatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class NumberFormatterTest {

    @Test
    void printFloatTest() {

        // perfect match
        Assertions.assertEquals("10000000", NumberFormatter.printFloat(1e7F));
        Assertions.assertEquals("0.00001", NumberFormatter.printFloat(1e-5F));
        Assertions.assertEquals("1", NumberFormatter.printFloat(1F));
        Assertions.assertEquals("1002.34", NumberFormatter.printFloat(1002.34F));
        Assertions.assertEquals("1001", NumberFormatter.printFloat(1001F));
        Assertions.assertEquals("728.11", NumberFormatter.printFloat(728.11F));
        Assertions.assertEquals("1204845", NumberFormatter.printFloat(1204845F));

        // cannot match accuracy
        Assertions.assertEquals("0", NumberFormatter.printFloat(1e-6F));
        Assertions.assertEquals("198.13", NumberFormatter.printFloat(198.1313563F));
    }
}
