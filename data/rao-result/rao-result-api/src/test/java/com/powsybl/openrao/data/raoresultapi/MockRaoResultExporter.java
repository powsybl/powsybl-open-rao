/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresultapi;

import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.raoresultapi.io.Exporter;

import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(Exporter.class)
public class MockRaoResultExporter implements Exporter {
    @Override
    public String getFormat() {
        return "Mock";
    }

    @Override
    public String getPropertiesPrefix() {
        return "rao-result.export.mock.";
    }

    @Override
    public Set<String> getRequiredProperties() {
        return Set.of("rao-result.export.mock.property-1", "rao-result.export.mock.property-2");
    }

    @Override
    public Class<? extends CracCreationContext> getCracCreationContextClass() {
        return MockCracCreationContext.class;
    }

    @Override
    public void exportData(RaoResult raoResult, CracCreationContext cracCreationContext, Properties properties, OutputStream outputStream) {
        if (raoResult instanceof MockRaoResult mockRaoResult) {
            mockRaoResult.setExportSuccessful();
        }
    }

    @Override
    public void exportData(RaoResult raoResult, Crac crac, Properties properties, OutputStream outputStream) {
        if (raoResult instanceof MockRaoResult mockRaoResult) {
            mockRaoResult.setExportSuccessful();
        }
    }
}
