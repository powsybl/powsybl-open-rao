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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class JsonVirtualHubsConfigurationTest {
    @Test
    void checkThatCorrectConfigurationFileImportsCorrectly() {
        VirtualHubsConfiguration configuration = JsonVirtualHubsConfiguration.importConfiguration(getClass().getResourceAsStream("/virtualHubsConfigurationFile.json"));

        assertEquals(3, configuration.getMarketAreas().size());
        assertEquals(4, configuration.getVirtualHubs().size());
    }

    @Test
    void checkThatConfigurationExportsCorrectlyOnOutputStream() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, true);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub"));
        configuration.addBorderDirection(new BorderDirection("From", "To", false));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonVirtualHubsConfiguration.exportConfiguration(baos, configuration);

        assertEquals("{\"marketAreas\":[{\"code\":\"AreaCode\",\"eic\":\"AreaEic\",\"isMcParticipant\":true,\"isAhc\":true},{\"code\":\"OtherAreaCode\",\"eic\":\"OtherAreaEic\",\"isMcParticipant\":false,\"isAhc\":false}],\"virtualHubs\":[{\"code\":\"HubCode\",\"eic\":\"HubEic\",\"isMcParticipant\":true,\"isAhc\":false,\"nodeName\":\"HubNodeName\",\"marketArea\":\"AreaCode\",\"oppositeHub\":\"OppositeHub\"}],\"borderDirections\":[{\"from\":\"From\",\"to\":\"To\",\"isAhc\":false}]}", new String(baos.toByteArray()));
    }

    @Test
    void checkThatConfigurationExportsCorrectlyOnWriter() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, true));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, true, "HubNodeName", marketArea, "OppositeHub"));
        configuration.addBorderDirection(new BorderDirection("From", "To", true));

        StringWriter writer = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(writer, configuration);

        assertEquals("{\"marketAreas\":[{\"code\":\"AreaCode\",\"eic\":\"AreaEic\",\"isMcParticipant\":true,\"isAhc\":false},{\"code\":\"OtherAreaCode\",\"eic\":\"OtherAreaEic\",\"isMcParticipant\":false,\"isAhc\":true}],\"virtualHubs\":[{\"code\":\"HubCode\",\"eic\":\"HubEic\",\"isMcParticipant\":true,\"isAhc\":true,\"nodeName\":\"HubNodeName\",\"marketArea\":\"AreaCode\",\"oppositeHub\":\"OppositeHub\"}],\"borderDirections\":[{\"from\":\"From\",\"to\":\"To\",\"isAhc\":true}]}", writer.toString());
    }

    @Test
    void checkThatIncorrectConfigurationImportThrows() {
        InputStream inputStream = getClass().getResourceAsStream("/invalidConfiguration.json");
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
            "Invalid parameter in configuration should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for configuration", thrown.getMessage());
    }

    @Test
    void checkThatIncorrectMarketAreaImportThrows() {
        InputStream inputStream = getClass().getResourceAsStream("/invalidMarketArea.json");
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
            "Invalid parameter in market area should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for market area", thrown.getMessage());

    }

    @Test
    void checkThatIncorrectVirtualHubImportThrows() {
        InputStream inputStream = getClass().getResourceAsStream("/invalidVirtualHub.json");
        VirtualHubsConfigurationDeserializationException thrown = assertThrows(
            VirtualHubsConfigurationDeserializationException.class,
            () -> JsonVirtualHubsConfiguration.importConfiguration(inputStream),
            "Invalid parameter in virtual hub should throw"
        );
        assertEquals("Attribute 'brokenParam' invalid for virtual hub", thrown.getMessage());
    }
}
