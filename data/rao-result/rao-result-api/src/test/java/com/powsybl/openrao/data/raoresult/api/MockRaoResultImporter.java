/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.io.utils.SafeFileReader;
import com.powsybl.openrao.data.raoresult.api.io.Importer;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Importer.class)
public class MockRaoResultImporter implements Importer {
    @Override
    public String getFormat() {
        return "Mock";
    }

    @Override
    public boolean exists(SafeFileReader inputFile) {
        return true;
    }

    @Override
    public RaoResult importData(SafeFileReader inputFile, Crac crac) {
        return new MockRaoResult();
    }
}
