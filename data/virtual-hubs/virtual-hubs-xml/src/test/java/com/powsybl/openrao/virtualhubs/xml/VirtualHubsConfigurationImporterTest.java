/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationImporterTest {
    @Test
    void checkThatConfigurationFileIsCorrectlyImported() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        VirtualHubsConfiguration configuration = importer.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.xml"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(5, configuration.getVirtualHubs().size());
        assertEquals(4, configuration.getBorderDirections().size());
    }

    @Test
    void checkThatConfigurationImportWithNullInputStreamThrows() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> importer.importConfiguration(null),
            "Null input stream as importConfiguration input should throw but does not"
        );
        assertEquals("Cannot import configuration from null input stream", thrown.getMessage());
    }

    @Test
    void checkThatInvalidInputStreamThrowsException() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        InputStream inputStream = getClass().getResourceAsStream("/truncatedFile.xml");
        VirtualHubsConfigProcessingException thrown = assertThrows(
            VirtualHubsConfigProcessingException.class,
            () -> importer.importConfiguration(inputStream),
            "Errors in XML processing should be thrown as VirtualHubsConfigProcessingException"
        );
        assertTrue(thrown.getCause() instanceof SAXException);
    }
}
