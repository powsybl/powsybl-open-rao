/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.json;

import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationDeserializer extends JsonDeserializer<VirtualHubsConfiguration> {
    @Override
    public VirtualHubsConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Map<String, MarketArea> marketAreas = new HashMap<>();
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "marketAreas":
                    deserializeMarketAreas(jsonParser, configuration, marketAreas);
                    break;
                case "virtualHubs":
                    deserializeVirtualHubs(jsonParser, configuration, marketAreas);
                    break;
                default:
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for configuration", jsonParser.getCurrentName()));
            }
        }
        return configuration;
    }

    private void deserializeMarketAreas(JsonParser jsonParser, VirtualHubsConfiguration configuration, Map<String, MarketArea> marketAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all market areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeMarketArea(jsonParser, configuration, marketAreas);
        }
    }

    private void deserializeVirtualHubs(JsonParser jsonParser, VirtualHubsConfiguration configuration, Map<String, MarketArea> marketAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all virtual hubs
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeVirtualHub(jsonParser, configuration, marketAreas);
        }
    }

    private void deserializeMarketArea(JsonParser jsonParser, VirtualHubsConfiguration configuration, Map<String, MarketArea> marketAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isMcParticipant = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "code":
                    code = jsonParser.nextTextValue();
                    break;
                case "eic":
                    eic = jsonParser.nextTextValue();
                    break;
                case "isMcParticipant":
                    isMcParticipant = jsonParser.nextBooleanValue();
                    break;
                default:
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for market area", jsonParser.getCurrentName()));
            }
        }
        MarketArea marketArea = new MarketArea(code, eic, isMcParticipant);
        marketAreas.put(code, marketArea);
        configuration.addMarketArea(marketArea);
    }

    private void deserializeVirtualHub(JsonParser jsonParser, VirtualHubsConfiguration configuration, Map<String, MarketArea> marketAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isMcParticipant = null;
        String nodeName = null;
        MarketArea marketArea = null;
        String oppositeHub = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "code":
                    code = jsonParser.nextTextValue();
                    break;
                case "eic":
                    eic = jsonParser.nextTextValue();
                    break;
                case "isMcParticipant":
                    isMcParticipant = jsonParser.nextBooleanValue();
                    break;
                case "nodeName":
                    nodeName = jsonParser.nextTextValue();
                    break;
                case "marketArea":
                    String marketAreaCode = jsonParser.nextTextValue();
                    marketArea = marketAreas.get(marketAreaCode);
                    break;
                case "oppositeHub":
                    oppositeHub = jsonParser.nextTextValue();
                    break;
                default:
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for virtual hub", jsonParser.getCurrentName()));
            }
        }
        configuration.addVirtualHub(new VirtualHub(code, eic, isMcParticipant, nodeName, marketArea, oppositeHub));
    }
}
