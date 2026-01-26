/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.InternalHvdc;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class XmlVirtualHubsConfigurationImporterTest {
    @Test
    void checkThatConfigurationFileIsCorrectlyImported() {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.xml"));

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(4);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(4);
    }

    @Test
    void checkThatConfigurationFileWithHvdcElementsIsCorrectlyImported() {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFileWithHvdcElements.xml"));

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(4);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(4);
        Assertions.assertThat(configuration.getInternalHvdcs()).hasSize(2);

        final InternalHvdc internalHvdc1 = configuration.getInternalHvdcs().get(0);
        final InternalHvdc internalHvdc2 = configuration.getInternalHvdcs().get(1);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(internalHvdc1.converters()).hasSize(2);
        softly.assertThat(internalHvdc1.converters().get(0).node()).isEqualTo("AAAAA11A");
        softly.assertThat(internalHvdc1.converters().get(0).station()).isEqualTo("Station 1");
        softly.assertThat(internalHvdc1.converters().get(1).node()).isEqualTo("BBBBB11B");
        softly.assertThat(internalHvdc1.converters().get(1).station()).isEqualTo("Station 2");
        softly.assertThat(internalHvdc1.lines()).hasSize(1);
        softly.assertThat(internalHvdc1.lines().getFirst().from()).isEqualTo("AAAAA11A");
        softly.assertThat(internalHvdc1.lines().getFirst().to()).isEqualTo("BBBBB11B");
        softly.assertAll();

        softly = new SoftAssertions();
        softly.assertThat(internalHvdc2.converters()).hasSize(2);
        softly.assertThat(internalHvdc2.converters().get(0).node()).isEqualTo("AAAAA21A");
        softly.assertThat(internalHvdc2.converters().get(0).station()).isEqualTo("Station 1");
        softly.assertThat(internalHvdc2.converters().get(1).node()).isEqualTo("BBBBB21B");
        softly.assertThat(internalHvdc2.converters().get(1).station()).isEqualTo("Station 2");
        softly.assertThat(internalHvdc2.lines()).hasSize(1);
        softly.assertThat(internalHvdc2.lines().getFirst().from()).isEqualTo("AAAAA21A");
        softly.assertThat(internalHvdc2.lines().getFirst().to()).isEqualTo("BBBBB21B");
        softly.assertAll();
    }

    @Test
    void checkThatConfigurationImportWithNullInputStreamThrows() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> XmlVirtualHubsConfiguration.importConfiguration(null))
            .withMessage("Virtual hubs configuration import on null input stream is invalid");
    }

    @Test
    void checkThatInvalidInputStreamThrowsException() throws IOException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/truncatedFile.xml")) {
            Assertions.assertThatExceptionOfType(VirtualHubsConfigProcessingException.class)
                .isThrownBy(() -> XmlVirtualHubsConfiguration.importConfiguration(inputStream))
                .withCauseInstanceOf(SAXException.class);
        }
    }
}
