/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import com.google.auto.service.AutoService;
import org.mockito.Mockito;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class CracImporterMock implements CracImporter {

    @Override
    public Crac importCrac(InputStream inputStream, OffsetDateTime timeStampFilter) {
        return Mockito.mock(Crac.class);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return true;
    }
}
