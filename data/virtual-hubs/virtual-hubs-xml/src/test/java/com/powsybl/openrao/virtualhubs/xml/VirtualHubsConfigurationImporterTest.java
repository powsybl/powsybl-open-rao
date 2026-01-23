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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationImporterTest {
    @Test
    void checkThatConfigurationFileIsCorrectlyImported() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        VirtualHubsConfiguration configuration = importer.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.xml"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(4, configuration.getVirtualHubs().size());
        assertEquals(4, configuration.getBorderDirections().size());
    }

    @Test
    void checkThatConfigurationFileWithHvdcElementsIsCorrectlyImported() {
        VirtualHubsConfigurationImporter importer = new VirtualHubsConfigurationImporter();
        VirtualHubsConfiguration configuration = importer.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFileWithHvdcElements.xml"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(4, configuration.getVirtualHubs().size());
        assertEquals(4, configuration.getBorderDirections().size());
        assertEquals(2, configuration.getInternalHvdcs().size());
        assertEquals(2, configuration.getInternalHvdcs().getFirst().converters().size());
        assertEquals("AAAAA11A", configuration.getInternalHvdcs().getFirst().converters().get(0).node());
        assertEquals("Station 1", configuration.getInternalHvdcs().getFirst().converters().get(0).station());
        assertEquals("BBBBB11B", configuration.getInternalHvdcs().getFirst().converters().get(1).node());
        assertEquals("Station 2", configuration.getInternalHvdcs().getFirst().converters().get(1).station());
        assertEquals(1, configuration.getInternalHvdcs().getFirst().lines().size());
        assertEquals("AAAAA11A", configuration.getInternalHvdcs().getFirst().lines().getFirst().from());
        assertEquals("BBBBB11B", configuration.getInternalHvdcs().getFirst().lines().getFirst().to());
        assertEquals(2, configuration.getInternalHvdcs().get(1).converters().size());
        assertEquals("AAAAA21A", configuration.getInternalHvdcs().get(1).converters().get(0).node());
        assertEquals("Station 1", configuration.getInternalHvdcs().get(1).converters().get(0).station());
        assertEquals("BBBBB21B", configuration.getInternalHvdcs().get(1).converters().get(1).node());
        assertEquals("Station 2", configuration.getInternalHvdcs().get(1).converters().get(1).station());
        assertEquals(1, configuration.getInternalHvdcs().get(1).lines().size());
        assertEquals("AAAAA21A", configuration.getInternalHvdcs().get(1).lines().getFirst().from());
        assertEquals("BBBBB21B", configuration.getInternalHvdcs().get(1).lines().getFirst().to());
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
