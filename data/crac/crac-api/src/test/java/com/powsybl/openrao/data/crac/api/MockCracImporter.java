/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.io.Importer;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;

import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Importer.class)
public class MockCracImporter implements Importer {

    static class MockCracCreationContext implements CracCreationContext {

        @Override
        public boolean isCreationSuccessful() {
            return true;
        }

        @Override
        public Crac getCrac() {
            return new MockCrac("crac");
        }

        @Override
        public OffsetDateTime getTimeStamp() {
            return null;
        }

        @Override
        public String getNetworkName() {
            return null;
        }

        @Override
        public CracCreationReport getCreationReport() {
            return null;
        }
    }

    @Override
    public String getFormat() {
        return "Mock";
    }

    @Override
    public boolean exists(String filename, InputStream inputStream) {
        return true;
    }

    @Override
    public CracCreationContext importData(InputStream inputStream, CracCreationParameters cracCreationParameters, Network network) {
        return new MockCracCreationContext();
    }
}
