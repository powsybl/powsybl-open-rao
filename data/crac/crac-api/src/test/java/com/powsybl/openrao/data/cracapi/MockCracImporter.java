/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.io.Importer;

import java.io.InputStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Importer.class)
public class MockCracImporter implements Importer {

    @Override
    public String getFormat() {
        return "Mock";
    }

    @Override
    public Crac importData(InputStream inputStream, CracFactory cracFactory, Network network) {
        return null;
    }
}
