/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.xml;

import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.HvdcPole;
import com.powsybl.openrao.virtualhubs.InternalHvdc;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import jakarta.xml.bind.JAXBException;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class XmlVirtualHubsConfigurationImporterTest {
    @Test
    void checkThatConfigurationFileIsCorrectlyImported() {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.xml"));

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(5);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(4);
    }

    @Test
    void checkThatConfigurationFileWithHvdcElementsIsCorrectlyImported() {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFileWithHvdcElements.xml"));

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(4);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(4);
        Assertions.assertThat(configuration.getInternalHvdcs()).hasSize(1);

        final InternalHvdc internalHvdc = configuration.getInternalHvdcs().getFirst();
        Assertions.assertThat(internalHvdc.code()).isEqualTo("A1B1_TESTFILE");

        final List<HvdcPole> poles = internalHvdc.poles();
        Assertions.assertThat(poles).hasSize(2);

        final HvdcPole pole1 = poles.getFirst();
        final HvdcPole pole2 = poles.getLast();

        final HvdcConverter pole1cvt1 = pole1.converters().getFirst();
        final HvdcConverter pole1cvt2 = pole1.converters().getLast();
        final HvdcConverter pole2cvt1 = pole2.converters().getFirst();
        final HvdcConverter pole2cvt2 = pole2.converters().getLast();
        final HvdcLine line1 = pole1.lines().getFirst();
        final HvdcLine line2 = pole2.lines().getFirst();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(pole1.converters()).hasSize(2);
        softly.assertThat(pole1cvt1.node()).isEqualTo("AAAAA11A");
        softly.assertThat(pole1cvt1.station()).isEqualTo("Station 1");
        softly.assertThat(pole1cvt2.node()).isEqualTo("BBBBB11B");
        softly.assertThat(pole1cvt2.station()).isEqualTo("Station 2");
        softly.assertThat(pole1.lines()).hasSize(1);
        softly.assertThat(line1.id()).isEqualTo("1");
        softly.assertThat(line1.from()).isEqualTo("AAAAA11A");
        softly.assertThat(line1.to()).isEqualTo("BBBBB11B");
        softly.assertAll();

        softly = new SoftAssertions();
        softly.assertThat(pole2.converters()).hasSize(2);
        softly.assertThat(pole2cvt1.node()).isEqualTo("AAAAA21A");
        softly.assertThat(pole2cvt1.station()).isEqualTo("Station 1");
        softly.assertThat(pole2cvt2.node()).isEqualTo("BBBBB21B");
        softly.assertThat(pole2cvt2.station()).isEqualTo("Station 2");
        softly.assertThat(pole2.lines()).hasSize(1);
        softly.assertThat(line2.id()).isEqualTo("2");
        softly.assertThat(line2.from()).isEqualTo("AAAAA21A");
        softly.assertThat(line2.to()).isEqualTo("BBBBB21B");
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
        try (InputStream inputStream = getClass().getResourceAsStream("/truncatedFile.xml")) {
            Assertions.assertThatExceptionOfType(VirtualHubsConfigProcessingException.class)
                .isThrownBy(() -> XmlVirtualHubsConfiguration.importConfiguration(inputStream))
                .withCauseInstanceOf(JAXBException.class);
        }
    }
}
