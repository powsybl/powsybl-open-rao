/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs.json;

import com.farao_community.farao.virtual_hubs.MarketArea;
import com.farao_community.farao.virtual_hubs.VirtualHub;
import com.farao_community.farao.virtual_hubs.VirtualHubsConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class JsonVirtualHubsConfigurationTest {
    @Test
    public void checkThatCorrectConfigurationFileImportsCorrectly() {
        VirtualHubsConfiguration configuration = JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.json"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(4, configuration.getVirtualHubs().size());
    }

    @Test
    public void checkThatConfigurationExportsCorrectlyOnOutputStream() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, "HubNodeName", marketArea));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonVirtualHubsConfiguration.exportConfiguration(baos, configuration);

        assertEquals("{\"marketAreas\":[{\"code\":\"AreaCode\",\"eic\":\"AreaEic\",\"isMcParticipant\":true},{\"code\":\"OtherAreaCode\",\"eic\":\"OtherAreaEic\",\"isMcParticipant\":false}],\"virtualHubs\":[{\"code\":\"HubCode\",\"eic\":\"HubEic\",\"isMcParticipant\":true,\"nodeName\":\"HubNodeName\",\"marketArea\":\"AreaCode\"}]}", new String(baos.toByteArray()));
    }

    @Test
    public void checkThatConfigurationExportsCorrectlyOnWriter() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, "HubNodeName", marketArea));

        StringWriter writer = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(writer, configuration);

        assertEquals("{\"marketAreas\":[{\"code\":\"AreaCode\",\"eic\":\"AreaEic\",\"isMcParticipant\":true},{\"code\":\"OtherAreaCode\",\"eic\":\"OtherAreaEic\",\"isMcParticipant\":false}],\"virtualHubs\":[{\"code\":\"HubCode\",\"eic\":\"HubEic\",\"isMcParticipant\":true,\"nodeName\":\"HubNodeName\",\"marketArea\":\"AreaCode\"}]}", writer.toString());
    }

    @Test
    public void checkThatIncorrectConfigurationImportThrows() {
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/invalidConfiguration.json")),
            "Invalid parameter in configuration should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for configuration", thrown.getMessage());
    }

    @Test
    public void checkThatIncorrectMarketAreaImportThrows() {
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/invalidMarketArea.json")),
            "Invalid parameter in market area should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for market area", thrown.getMessage());

    }

    @Test
    public void checkThatIncorrectVirtualHubImportThrows() {
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/invalidVirtualHub.json")),
            "Invalid parameter in virtual hub should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for virtual hub", thrown.getMessage());
    }
}
