/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.swe_cne_exporter.xml_formatter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NumberFormatterTest {

    @Test
    public void printFloatTest() {

        // perfect match
        Assert.assertEquals("10000000", NumberFormatter.printFloat(1e7F));
        Assert.assertEquals("0.00001", NumberFormatter.printFloat(1e-5F));
        Assert.assertEquals("1", NumberFormatter.printFloat(1F));
        Assert.assertEquals("1002.34", NumberFormatter.printFloat(1002.34F));
        Assert.assertEquals("1001", NumberFormatter.printFloat(1001F));
        Assert.assertEquals("728.11", NumberFormatter.printFloat(728.11F));
        Assert.assertEquals("1204845", NumberFormatter.printFloat(1204845F));

        // cannot match accuracy
        Assert.assertEquals("0", NumberFormatter.printFloat(1e-6F));
        Assert.assertEquals("198.13", NumberFormatter.printFloat(198.1313563F));
    }
}
