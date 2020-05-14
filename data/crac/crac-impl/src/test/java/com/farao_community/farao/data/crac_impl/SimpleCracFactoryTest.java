/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleCracFactoryTest {
    final String factoryName = "SimpleCracFactory";

    @Test
    public void testDependencyInjection() {
        assertEquals(factoryName, new SimpleCracFactory().getName());
        CracFactory factory = CracFactory.find(factoryName);
        assertNotNull(factory);
        assertEquals(factory.getClass(), SimpleCracFactory.class);
    }

    @Test
    public void testCreateSimpleCrac() {
        String id = "idForTest";
        String name = "testName";
        Crac crac = new SimpleCracFactory().create(id, name);
        assertEquals(crac.getClass(), SimpleCrac.class);
        assertEquals(crac.getId(), id);
        assertEquals(crac.getName(), name);
    }
}
