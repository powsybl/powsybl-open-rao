/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.google.auto.service.AutoService;

import java.time.OffsetDateTime;

/**
 * Crac Factory implementation.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracFactory.class)
public class CracImplFactory implements CracFactory {

    private static final String NAME = "CracImplFactory";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Crac create(String id, String name, OffsetDateTime timestamp) {
        return new CracImpl(id, name, timestamp);
    }

    @Override
    public Crac create(String id, String name) {
        return new CracImpl(id, name);
    }

    @Override
    public Crac create(String id) {
        return new CracImpl(id);
    }
}
