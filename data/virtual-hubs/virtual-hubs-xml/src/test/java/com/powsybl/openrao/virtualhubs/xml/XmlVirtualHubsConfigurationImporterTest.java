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

        Assertions.assertThat(configuration.getRegion()).isNotNull();
        Assertions.assertThat(configuration.getSynchronousAreas()).hasSize(1);
        Assertions.assertThat(configuration.getControlAreas()).hasSize(2);
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
        assertPole1Content(softly, pole1, pole1cvt1, pole1cvt2, line1);
        softly.assertAll();

        softly = new SoftAssertions();
        assertPole2Content(softly, pole2, pole2cvt1, pole2cvt2, line2);
        softly.assertAll();
    }

    private static void assertPole1Content(final SoftAssertions assertions, final HvdcPole pole, final HvdcConverter converter1, final HvdcConverter converter2, final HvdcLine line) {
        assertions.assertThat(pole.converters()).hasSize(2);
        assertions.assertThat(converter1.node()).isEqualTo("AAAAA11A");
        assertions.assertThat(converter1.station()).isEqualTo("Station 1");
        assertions.assertThat(converter2.node()).isEqualTo("BBBBB11B");
        assertions.assertThat(converter2.station()).isEqualTo("Station 2");
        assertions.assertThat(pole.lines()).hasSize(1);
        assertions.assertThat(line.id()).isEqualTo("1");
        assertions.assertThat(line.from()).isEqualTo("AAAAA11A");
        assertions.assertThat(line.to()).isEqualTo("BBBBB11B");
    }

    private static void assertPole2Content(final SoftAssertions assertions, final HvdcPole pole, final HvdcConverter converter1, final HvdcConverter converter2, final HvdcLine line) {
        assertions.assertThat(pole.converters()).hasSize(2);
        assertions.assertThat(converter1.node()).isEqualTo("AAAAA21A");
        assertions.assertThat(converter1.station()).isEqualTo("Station 1");
        assertions.assertThat(converter2.node()).isEqualTo("BBBBB21B");
        assertions.assertThat(converter2.station()).isEqualTo("Station 2");
        assertions.assertThat(pole.lines()).hasSize(1);
        assertions.assertThat(line.id()).isEqualTo("2");
        assertions.assertThat(line.from()).isEqualTo("AAAAA21A");
        assertions.assertThat(line.to()).isEqualTo("BBBBB21B");
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
