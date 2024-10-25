/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.powsybl.openrao.data.corecneexporter;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.CracCreationContext;
import com.powsybl.openrao.data.cracio.commons.api.stdcreationcontext.UcteCracCreationContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CoreCneExporterTest {
    private final CoreCneExporter exporter = new CoreCneExporter();

    @Test
    void testFormat() {
        assertEquals("CORE-CNE", exporter.getFormat());
    }

    @Test
    void testExportWithWrongCracCreationContext() {
        CracCreationContext cracCreationContext = Mockito.mock(CracCreationContext.class);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.exportData(null, cracCreationContext, null, null));
        assertEquals("CORE-CNE exporter expects a UcteCracCreationContext.", exception.getMessage());
    }

    @Test
    void testProperties() {
        assertEquals(Set.of("document-id", "revision-number", "domain-id", "process-type", "sender-id", "sender-role", "receiver-id", "receiver-role", "time-interval"), exporter.getRequiredProperties());
    }

    @Test
    void testCracCreationContextClass() {
        assertEquals(UcteCracCreationContext.class, exporter.getCracCreationContextClass());
    }

    @Test
    void testWrongCracCreationContextClass() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(Mockito.mock(CracCreationContext.class), null));
        assertEquals("CORE-CNE exporter expects a UcteCracCreationContext.", exception.getMessage());
    }

    @Test
    void testNullProperties() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(Mockito.mock(UcteCracCreationContext.class), null));
        assertEquals("The export properties cannot be null for CORE-CNE export.", exception.getMessage());
    }

    @Test
    void testMissingRequiredProperty() {
        Properties properties = new Properties();
        properties.setProperty("document-id", "");
        properties.setProperty("revision-number", "");
        properties.setProperty("process-type", "");
        properties.setProperty("sender-id", "");
        properties.setProperty("sender-role", "");
        properties.setProperty("receiver-id", "");
        properties.setProperty("receiver-role", "");
        properties.setProperty("time-interval", "");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> exporter.validateDataToExport(Mockito.mock(UcteCracCreationContext.class), properties));
        assertEquals("The mandatory domain-id property is missing for CORE-CNE export.", exception.getMessage());
    }
}
