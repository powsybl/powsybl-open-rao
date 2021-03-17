/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.native_crac_io_api;

import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.google.auto.service.AutoService;
import org.mockito.Mockito;

import java.io.InputStream;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(NativeCracImporter.class)
public class NativeCracImporterMock implements NativeCracImporter {

    @Override
    public String getFormat() {
        return "MockedNativeCracFormat";
    }

    @Override
    public NativeCrac importNativeCrac(InputStream inputStream) {
        NativeCrac nativeCrac = Mockito.mock(NativeCrac.class);
        Mockito.when(nativeCrac.getFormat()).thenReturn("MockedNativeCracFormat");
        return nativeCrac;
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return true;
    }
}
