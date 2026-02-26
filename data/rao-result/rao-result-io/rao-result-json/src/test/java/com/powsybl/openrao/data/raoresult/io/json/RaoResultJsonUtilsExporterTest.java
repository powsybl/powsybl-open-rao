/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.io.json.JsonCracCreationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultJsonUtilsExporterTest {
    private final RaoResultJsonExporter exporter = new RaoResultJsonExporter();

    @Test
    void testFormat() {
        assertEquals("JSON", exporter.getFormat());
    }

    @Test
    void testExportWithWrongCracCreationContext() {
        CracCreationContext cracCreationContext = Mockito.mock(CracCreationContext.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.exportData(null, cracCreationContext, null, null));
        assertEquals("JSON exporter expects a JsonCracCreationContext.", exception.getMessage());
    }

    @Test
    void testProperties() {
        assertTrue(exporter.getRequiredProperties().isEmpty());
    }

    @Test
    void testCracCreationContextClass() {
        assertEquals(JsonCracCreationContext.class, exporter.getCracCreationContextClass());
    }

    @Test
    void testWrongCracCreationContextClass() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(Mockito.mock(CracCreationContext.class), null));
        assertEquals("JSON exporter expects a JsonCracCreationContext.", exception.getMessage());
    }
}
