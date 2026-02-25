/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultExportTest {

    private MockRaoResult raoResult;

    @BeforeEach
    void setUp() {
        raoResult = new MockRaoResult();
    }

    @Test
    void testExportWithUnknownExporter() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> raoResult.write("unknownFormat", (CracCreationContext) null, null, null));
        assertEquals("Export format unknownFormat not supported", exception.getMessage());
        assertFalse(raoResult.wasExportSuccessful());
    }

    @Test
    void testExportCracWithValidExporter() {
        raoResult.write("Mock", (CracCreationContext) null, new Properties(), null);
        assertTrue(raoResult.wasExportSuccessful());
    }

    @Test
    void testValidateExportData() {
        OpenRaoException exception;
        CracCreationContext cracCreationContext = Mockito.mock(CracCreationContext.class);
        MockRaoResultExporter exporter = new MockRaoResultExporter();
        // wrong CRAC creation context class
        exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(cracCreationContext, new Properties()));
        assertEquals("Mock exporter expects a MockCracCreationContext.", exception.getMessage());
        // null properties
        exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(new MockCracCreationContext(), null));
        assertEquals("The export properties cannot be null for Mock export.", exception.getMessage());
        // missing required properties
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.mock.property-1", "true");
        exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(new MockCracCreationContext(), properties));
        assertEquals("The mandatory rao-result.export.mock.property-2 property is missing for Mock export.", exception.getMessage());
    }
}
