/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.virtualhubs.json;

import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationSerializer extends JsonSerializer<VirtualHubsConfiguration> {
    @Override
    public void serialize(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeMarketAreas(virtualHubsConfiguration, jsonGenerator);
        serializeVirtualHubs(virtualHubsConfiguration, jsonGenerator);
        serializeBorderDirections(virtualHubsConfiguration, jsonGenerator);
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
        jsonGenerator.writeStringField("code", marketArea.code());
        jsonGenerator.writeStringField("eic", marketArea.eic());
        jsonGenerator.writeBooleanField("isMcParticipant", marketArea.isMcParticipant());
        jsonGenerator.writeBooleanField("isAhc", marketArea.isAhc());
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
        jsonGenerator.writeStringField("code", virtualHub.code());
        jsonGenerator.writeStringField("eic", virtualHub.eic());
        jsonGenerator.writeBooleanField("isMcParticipant", virtualHub.isMcParticipant());
        jsonGenerator.writeBooleanField("isAhc", virtualHub.isAhc());
        jsonGenerator.writeStringField("nodeName", virtualHub.nodeName());
        jsonGenerator.writeStringField("marketArea", virtualHub.relatedMa().code());
        jsonGenerator.writeStringField("oppositeHub", virtualHub.oppositeHub());
        jsonGenerator.writeEndObject();
    }

    private void serializeBorderDirections(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("borderDirections");
        jsonGenerator.writeStartArray();
        for (BorderDirection borderDirection : virtualHubsConfiguration.getBorderDirections()) {
            serializeBorderDirection(borderDirection, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeBorderDirection(BorderDirection borderDirection, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("from", borderDirection.from());
        jsonGenerator.writeStringField("to", borderDirection.to());
        jsonGenerator.writeBooleanField("isAhc", borderDirection.isAhc());
        jsonGenerator.writeEndObject();
    }
}
