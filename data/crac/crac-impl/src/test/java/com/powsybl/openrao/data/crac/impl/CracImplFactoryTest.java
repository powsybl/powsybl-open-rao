/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CracImplFactoryTest {
    private final String factoryName = "CracImplFactory";

    @Test
    void testDependencyInjection() {
        assertEquals(factoryName, new CracImplFactory().getName());
        CracFactory factory = CracFactory.find(factoryName);
        assertNotNull(factory);
        assertEquals(factory.getClass(), CracImplFactory.class);
    }

    @Test
    void testCreateSimpleCrac() {
        String id = "idForTest";
        String name = "testName";
        Crac crac = new CracImplFactory().create(id, name);
        assertEquals(crac.getClass(), CracImpl.class);
        assertEquals(crac.getId(), id);
        assertEquals(crac.getName(), name);
    }
}
