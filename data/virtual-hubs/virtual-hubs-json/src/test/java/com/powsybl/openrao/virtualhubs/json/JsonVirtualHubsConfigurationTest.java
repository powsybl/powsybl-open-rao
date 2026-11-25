/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.json;

import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class JsonVirtualHubsConfigurationTest {
    @Test
    void roundtripTest() throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream("/roundtrip.json");
        final VirtualHubsConfiguration configuration = JsonVirtualHubsConfiguration.importConfiguration(inputStream);
        final String originalConfig = new String(getClass().getResourceAsStream("/roundtrip.json").readAllBytes());

        final ByteArrayOutputStream serializedConfigOutputStream = new ByteArrayOutputStream();
        JsonVirtualHubsConfiguration.exportConfiguration(serializedConfigOutputStream, configuration);
        final String serializedConfig = serializedConfigOutputStream.toString();

        final String onelinedOriginalConfig = originalConfig.replace("\r\n", "").replace("\n", "");
        Assertions.assertThat(serializedConfig).isEqualToIgnoringWhitespace(onelinedOriginalConfig);

        final VirtualHubsConfiguration deserializedConfiguration = JsonVirtualHubsConfiguration.importConfiguration(new ByteArrayInputStream(serializedConfig.getBytes()));

        Assertions.assertThat(deserializedConfiguration)
            .usingRecursiveComparison()
            .isEqualTo(configuration);
    }

    @Test
    void checkThatCorrectConfigurationFileImportsCorrectly() {
        VirtualHubsConfiguration configuration = JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.json"));

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(3);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(4);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(1);
    }

    @Test
    void checkThatConfigurationExportsCorrectlyOnOutputStream() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, true, false, false, null, null, false, false);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, false, false, false, null, null, false, false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub", false, null, null, null));
        configuration.addBorderDirection(new BorderDirection("From", "To", false, false));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonVirtualHubsConfiguration.exportConfiguration(baos, configuration);

        Assertions.assertThat(baos.toString()).isEqualToIgnoringNewLines(
            """
                {
                "marketAreas":[{"code":"AreaCode","eic":"AreaEic","isMcParticipant":true,"isAhc":true,"isTechCounterparty":false,"isSdac":false,"isItcp":false,"isIgnoreECs":false},{"code":"OtherAreaCode","eic":"OtherAreaEic","isMcParticipant":false,"isAhc":false,"isTechCounterparty":false,"isSdac":false,"isItcp":false,"isIgnoreECs":false}],
                "virtualHubs":[{"code":"HubCode","eic":"HubEic","isMcParticipant":true,"isAhc":false,"nodeName":"HubNodeName","relatedMa":"AreaCode","oppositeHub":"OppositeHub","isSdac":false}],
                "borderDirections":[{"from":"From","to":"To","isAhc":false,"isSdac":false}]
                }
                """
        );
    }

    @Test
    void checkThatConfigurationExportsCorrectlyOnWriter() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false, false, false, null, null, false, false);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, true, false, false, null, null, false, false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, true, "HubNodeName", marketArea, "OppositeHub", false, null, null, null));
        configuration.addBorderDirection(new BorderDirection("From", "To", true, false));

        StringWriter writer = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(writer, configuration);

        Assertions.assertThat(writer.toString()).isEqualToIgnoringNewLines(
            """
                {
                "marketAreas":[{"code":"AreaCode","eic":"AreaEic","isMcParticipant":true,"isAhc":false,"isTechCounterparty":false,"isSdac":false,"isItcp":false,"isIgnoreECs":false},{"code":"OtherAreaCode","eic":"OtherAreaEic","isMcParticipant":false,"isAhc":true,"isTechCounterparty":false,"isSdac":false,"isItcp":false,"isIgnoreECs":false}],
                "virtualHubs":[{"code":"HubCode","eic":"HubEic","isMcParticipant":true,"isAhc":true,"nodeName":"HubNodeName","relatedMa":"AreaCode","oppositeHub":"OppositeHub","isSdac":false}],
                "borderDirections":[{"from":"From","to":"To","isAhc":true,"isSdac":false}]
                }
                """
        );
    }

    @Test
    void checkThatIncorrectConfigurationImportThrows() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/invalidConfiguration.json")) {
            VirtualHubsConfigurationDeserializationException thrown = assertThrows(
                VirtualHubsConfigurationDeserializationException.class,
                () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
                "Invalid parameter in configuration should throw"
            );
            assertEquals("Attribute 'brokenParam' invalid for configuration", thrown.getMessage());
        }
    }

    @Test
    void checkThatIncorrectMarketAreaImportThrows() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/invalidMarketArea.json")) {
            VirtualHubsConfigurationDeserializationException thrown = assertThrows(
                VirtualHubsConfigurationDeserializationException.class,
                () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
                "Invalid parameter in market area should throw"
            );
            assertEquals("Attribute 'brokenParam' invalid for market area", thrown.getMessage());
        }
    }

    @Test
    void checkThatIncorrectVirtualHubImportThrows() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/invalidVirtualHub.json")) {
            VirtualHubsConfigurationDeserializationException thrown = assertThrows(
                VirtualHubsConfigurationDeserializationException.class,
                () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
                "Invalid parameter in virtual hub should throw"
            );
            assertEquals("Attribute 'brokenParam' invalid for virtual hub", thrown.getMessage());
        }
    }
}
