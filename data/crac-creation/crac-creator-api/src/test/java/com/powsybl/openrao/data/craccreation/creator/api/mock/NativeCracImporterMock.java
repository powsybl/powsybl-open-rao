/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.api.mock;

import com.powsybl.openrao.data.nativecracioapi.NativeCracImporter;
import com.google.auto.service.AutoService;

import java.io.InputStream;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(NativeCracImporter.class)
public class NativeCracImporterMock implements NativeCracImporter<NativeCracMock> {

    @Override
    public String getFormat() {
        return "MockedNativeCracFormat";
    }

    @Override
    public NativeCracMock importNativeCrac(InputStream inputStream) {
        return new NativeCracMock(true);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return true;
    }
}
