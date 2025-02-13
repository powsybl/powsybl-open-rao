/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.google.auto.service.AutoService;

import java.time.OffsetDateTime;

/**
 * Mock CracFactory implementation, for unit tests only
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracFactory.class)
public class MockCracFactory2 implements CracFactory {
    @Override
    public Crac create(String id, String name, OffsetDateTime timestamp) {
        return null;
    }

    @Override
    public Crac create(String id, String name) {
        return null;
    }

    @Override
    public String getName() {
        return "MockCracFactory2";
    }
}
