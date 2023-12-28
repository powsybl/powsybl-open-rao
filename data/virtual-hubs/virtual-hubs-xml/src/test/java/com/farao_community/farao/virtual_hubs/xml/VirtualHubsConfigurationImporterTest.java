/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.xml;

import com.farao_community.farao.virtual_hubs.VirtualHubsConfiguration;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationImporterTest {
    @Test
    public void checkThatConfigurationFileIsCorrectlyImported() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        VirtualHubsConfiguration configuration = importer.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.xml"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(4, configuration.getVirtualHubs().size());
    }

    @Test
    public void checkThatConfigurationImportWithNullInputStreamThrows() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> importer.importConfiguration(null),
            "Null input stream as importConfiguration input should throw but does not"
        );
        assertEquals("Cannot import configuration from null input stream", thrown.getMessage());
    }

    @Test
    public void checkThatInvalidInputStreamThrowsException() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        VirtualHubsConfigProcessingException thrown = assertThrows(
            VirtualHubsConfigProcessingException.class,
            () -> importer.importConfiguration(getClass().getResourceAsStream("/truncatedFile.xml")),
            "Errors in XML processing should be thrown as VirtualHubsConfigProcessingException"
        );
        assertTrue(thrown.getCause() instanceof SAXException);
    }
}
