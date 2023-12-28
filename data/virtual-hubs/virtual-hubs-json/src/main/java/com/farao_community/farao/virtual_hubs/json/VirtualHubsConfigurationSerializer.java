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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationSerializer extends JsonSerializer<VirtualHubsConfiguration> {
    @Override
    public void serialize(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeMarketAreas(virtualHubsConfiguration, jsonGenerator);
        serializeVirtualHubs(virtualHubsConfiguration, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private void serializeMarketAreas(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("marketAreas");
        jsonGenerator.writeStartArray();
        for (MarketArea marketArea : virtualHubsConfiguration.getMarketAreas()) {
            serializeMarketArea(marketArea, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeMarketArea(MarketArea marketArea, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", marketArea.getCode());
        jsonGenerator.writeStringField("eic", marketArea.getEic());
        jsonGenerator.writeBooleanField("isMcParticipant", marketArea.isMcParticipant());
        jsonGenerator.writeEndObject();
    }

    private void serializeVirtualHubs(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("virtualHubs");
        jsonGenerator.writeStartArray();
        for (VirtualHub virtualHub : virtualHubsConfiguration.getVirtualHubs()) {
            serializeVirtualHub(virtualHub, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeVirtualHub(VirtualHub virtualHub, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", virtualHub.getCode());
        jsonGenerator.writeStringField("eic", virtualHub.getEic());
        jsonGenerator.writeBooleanField("isMcParticipant", virtualHub.isMcParticipant());
        jsonGenerator.writeStringField("nodeName", virtualHub.getNodeName());
        jsonGenerator.writeStringField("marketArea", virtualHub.getRelatedMa().getCode());
        jsonGenerator.writeEndObject();
    }
}
