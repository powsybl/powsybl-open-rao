/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.core_cne_exporter.xml_formatter;

import org.junit.Test;

import static com.farao_community.farao.data.core_cne_exporter.xml_formatter.NumberFormatter.printFloat;
import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NumberFormatterTest {

    @Test
    public void printFloatTest() {

        // perfect match
        assertEquals("10000000", printFloat(1e7F));
        assertEquals("0.00001", printFloat(1e-5F));
        assertEquals("1", printFloat(1F));
        assertEquals("1002.34", printFloat(1002.34F));
        assertEquals("1001", printFloat(1001F));
        assertEquals("728.11", printFloat(728.11F));
        assertEquals("1204845", printFloat(1204845F));

        // cannot match accuracy
        assertEquals("0", printFloat(1e-6F));
        assertEquals("198.13", printFloat(198.1313563F));
    }
}
