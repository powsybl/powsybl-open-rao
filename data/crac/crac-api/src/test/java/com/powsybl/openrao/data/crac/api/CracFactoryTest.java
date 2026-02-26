/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracFactoryTest {

    @Test
    void testGetFactory() {
        CracFactory cf1 = CracFactory.find("MockCracFactory1");
        assertNotNull(cf1);
        assertEquals(cf1.getClass(), MockCracFactory1.class);
        CracFactory cf2 = CracFactory.find("MockCracFactory2");
        assertNotNull(cf2);
        assertEquals(cf2.getClass(), MockCracFactory2.class);
    }

    @Test
    void mustThrowIfImplemNotFound() {
        assertThrows(OpenRaoException.class, () -> CracFactory.find("SimpleCracFactory"));
    }

    @Test
    void mustThrowIfNameNullAndMultipleImplem() {
        assertThrows(OpenRaoException.class, () -> CracFactory.find(null));
    }

    @Test
    void testDefaultConfig() {
        CracFactory cf = CracFactory.findDefault();
        assertNotNull(cf);
        assertEquals(cf.getClass(), MockCracFactory1.class);
    }

    // TODO : test different default configs : empty (should throw),
    // with MockCracFactory2, with wrong implem name (should throw)
    // (this is not yet possible with PowSyBl's TestPlatformConfigProvider)
}
