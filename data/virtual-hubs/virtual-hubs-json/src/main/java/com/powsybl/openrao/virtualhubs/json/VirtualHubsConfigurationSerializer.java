/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.ControlArea;
import com.powsybl.openrao.virtualhubs.HvdcConverter;
import com.powsybl.openrao.virtualhubs.HvdcLine;
import com.powsybl.openrao.virtualhubs.HvdcPole;
import com.powsybl.openrao.virtualhubs.InternalHvdc;
import com.powsybl.openrao.virtualhubs.MarketArea;
import com.powsybl.openrao.virtualhubs.Region;
import com.powsybl.openrao.virtualhubs.SynchronousArea;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.Voltage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationSerializer extends JsonSerializer<VirtualHubsConfiguration> {
    @Override
    public void serialize(final VirtualHubsConfiguration virtualHubsConfiguration,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeRegion(virtualHubsConfiguration, jsonGenerator);
        serializeSynchronousAreas(virtualHubsConfiguration, jsonGenerator);
        serializeControlAreas(virtualHubsConfiguration, jsonGenerator);
        serializeMarketAreas(virtualHubsConfiguration, jsonGenerator);
        serializeVirtualHubs(virtualHubsConfiguration, jsonGenerator);
        serializeBorderDirections(virtualHubsConfiguration, jsonGenerator);
        serializeInternalHvdcs(virtualHubsConfiguration, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private void serializeRegion(final VirtualHubsConfiguration virtualHubsConfiguration, final JsonGenerator jsonGenerator) throws IOException {
        final Region region = virtualHubsConfiguration.getRegion();
        if (region != null) {
            jsonGenerator.writeFieldName("region");
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("code", region.code());
            jsonGenerator.writeStringField("eic", region.eic());
            jsonGenerator.writeEndObject();
        }
    }

    private void serializeSynchronousAreas(final VirtualHubsConfiguration virtualHubsConfiguration, final JsonGenerator jsonGenerator) throws IOException {
        final List<SynchronousArea> synchronousAreas = virtualHubsConfiguration.getSynchronousAreas();
        if (!synchronousAreas.isEmpty()) {
            jsonGenerator.writeFieldName("synchronousAreas");
            jsonGenerator.writeStartArray();
            for (final SynchronousArea synchronousArea : synchronousAreas) {
                serializeSynchronousArea(synchronousArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeSynchronousArea(final SynchronousArea synchronousArea, final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", synchronousArea.code());
        jsonGenerator.writeStringField("eic", synchronousArea.eic());
        jsonGenerator.writeBooleanField("isLive", synchronousArea.isLive());
        jsonGenerator.writeBooleanField("isMainArea", synchronousArea.isMainArea());
        final Map<String, String> localLOT3Parameters = synchronousArea.localLOT3Parameters();
        if (localLOT3Parameters != null) {
            serializeLocalLOT3Parameters(localLOT3Parameters, jsonGenerator);
        }
        final List<Voltage> initialVoltages = synchronousArea.initialVoltages();
        if (initialVoltages != null) {
            serializeInitialVoltages(initialVoltages, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeLocalLOT3Parameters(final Map<String, String> localLOT3Parameters,
                                                     final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("localLOT3Parameters");
        jsonGenerator.writeStartObject();
        for (final Map.Entry<String, String> parameter : localLOT3Parameters.entrySet()) {
            jsonGenerator.writeStringField(parameter.getKey(), parameter.getValue());
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeInitialVoltages(final List<Voltage> initialVoltages,
                                                 final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("initialVoltages");
        jsonGenerator.writeStartArray();
        for (final Voltage voltage : initialVoltages) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("nominal", voltage.nominal());
            jsonGenerator.writeNumberField("initial", voltage.initial());
            jsonGenerator.writeNumberField("slackSearchPriority", voltage.slackSearchPriority());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeControlAreas(final VirtualHubsConfiguration virtualHubsConfiguration, final JsonGenerator jsonGenerator) throws IOException {
        final List<ControlArea> controlAreas = virtualHubsConfiguration.getControlAreas();
        if (!controlAreas.isEmpty()) {
            jsonGenerator.writeFieldName("controlAreas");
            jsonGenerator.writeStartArray();
            for (ControlArea controlArea : controlAreas) {
                serializeControlArea(controlArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeControlArea(final ControlArea controlArea, final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", controlArea.code());
        jsonGenerator.writeStringField("eic", controlArea.eic());
        jsonGenerator.writeBooleanField("isMcParticipant", controlArea.isMcParticipant());
        jsonGenerator.writeBooleanField("isTechCounterparty", controlArea.isTechCounterparty());
        jsonGenerator.writeBooleanField("isSdac", controlArea.isSdac());
        jsonGenerator.writeEndObject();
    }

    private void serializeMarketAreas(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<MarketArea> marketAreas = virtualHubsConfiguration.getMarketAreas();
        if (!marketAreas.isEmpty()) {
            jsonGenerator.writeFieldName("marketAreas");
            jsonGenerator.writeStartArray();
            for (MarketArea marketArea : marketAreas) {
                serializeMarketArea(marketArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeMarketArea(MarketArea marketArea, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", marketArea.code());
        jsonGenerator.writeStringField("eic", marketArea.eic());
        jsonGenerator.writeBooleanField("isMcParticipant", marketArea.isMcParticipant());
        jsonGenerator.writeBooleanField("isAhc", marketArea.isAhc());
        jsonGenerator.writeBooleanField("isTechCounterparty", marketArea.isTechCounterparty());
        jsonGenerator.writeBooleanField("isSdac", marketArea.isSdac());
        if (marketArea.synchronousArea() != null) {
            jsonGenerator.writeStringField("synchronousArea", marketArea.synchronousArea().code());
        }
        if (marketArea.relatedCa() != null) {
            jsonGenerator.writeStringField("relatedCa", marketArea.relatedCa().code());
        }
        jsonGenerator.writeBooleanField("isItcp", marketArea.isItcp());
        jsonGenerator.writeBooleanField("isIgnoreECs", marketArea.isIgnoreECs());
        jsonGenerator.writeEndObject();
    }

    private void serializeVirtualHubs(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<VirtualHub> virtualHubs = virtualHubsConfiguration.getVirtualHubs();
        if (!virtualHubs.isEmpty()) {
            jsonGenerator.writeFieldName("virtualHubs");
            jsonGenerator.writeStartArray();
            for (VirtualHub virtualHub : virtualHubs) {
                serializeVirtualHub(virtualHub, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeVirtualHub(VirtualHub virtualHub, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", virtualHub.code());
        jsonGenerator.writeStringField("eic", virtualHub.eic());
        jsonGenerator.writeBooleanField("isMcParticipant", virtualHub.isMcParticipant());
        jsonGenerator.writeBooleanField("isAhc", virtualHub.isAhc());
        final String nodeName = virtualHub.nodeName();
        if (nodeName != null) {
            jsonGenerator.writeStringField("nodeName", nodeName);
        }
        jsonGenerator.writeStringField("relatedMa", virtualHub.relatedMa().code());
        final String oppositeHub = virtualHub.oppositeHub();
        if (oppositeHub != null) {
            jsonGenerator.writeStringField("oppositeHub", oppositeHub);
        }
        jsonGenerator.writeBooleanField("isSdac", virtualHub.isSdac());
        final BigDecimal splittingFactor = virtualHub.splittingFactor();
        if (splittingFactor != null) {
            jsonGenerator.writeNumberField("splittingFactor", splittingFactor);
        }
        final BigDecimal lf = virtualHub.lf();
        if (lf != null) {
            jsonGenerator.writeNumberField("lf", lf);
        }
        final BigDecimal rlf = virtualHub.rlf();
        if (rlf != null) {
            jsonGenerator.writeNumberField("rlf", rlf);
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeBorderDirections(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<BorderDirection> borderDirections = virtualHubsConfiguration.getBorderDirections();
        if (!borderDirections.isEmpty()) {
            jsonGenerator.writeFieldName("borderDirections");
            jsonGenerator.writeStartArray();
            for (BorderDirection borderDirection : borderDirections) {
                serializeBorderDirection(borderDirection, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeBorderDirection(BorderDirection borderDirection, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("from", borderDirection.from());
        jsonGenerator.writeStringField("to", borderDirection.to());
        jsonGenerator.writeBooleanField("isAhc", borderDirection.isAhc());
        jsonGenerator.writeBooleanField("isSdac", borderDirection.isSdac());
        jsonGenerator.writeEndObject();
    }

    private void serializeInternalHvdcs(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<InternalHvdc> internalHvdcs = virtualHubsConfiguration.getInternalHvdcs();
        if (!internalHvdcs.isEmpty()) {
            jsonGenerator.writeFieldName("internalHvdcs");
            jsonGenerator.writeStartArray();
            for (InternalHvdc internalHvdc : internalHvdcs) {
                serializeInternalHvdc(internalHvdc, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeInternalHvdc(InternalHvdc internalHvdc, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("code", internalHvdc.code());
        jsonGenerator.writeStringField("eic", internalHvdc.eic());
        final List<HvdcPole> poles = internalHvdc.poles();
        if (poles != null) {
            serializeHvdcPoles(poles, jsonGenerator);
        }
        jsonGenerator.writeStringField("relatedMa", internalHvdc.relatedMa().code());
        jsonGenerator.writeBooleanField("isMthvdc", internalHvdc.isMthvdc());
        jsonGenerator.writeBooleanField("isBipolar", internalHvdc.isBipolar());
        jsonGenerator.writeEndObject();
    }

    private static void serializeHvdcPoles(final List<HvdcPole> poles,
                                           final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("poles");
        jsonGenerator.writeStartArray();
        for (final HvdcPole pole : poles) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("id", pole.id());
            jsonGenerator.writeStringField("eic", pole.eic());
            final List<HvdcConverter> converters = pole.converters();
            if (converters != null) {
                serializeHvdcConverters(converters, jsonGenerator);
            }
            final List<HvdcLine> lines = pole.lines();
            if (lines != null) {
                serializeHvdcLines(lines, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeHvdcConverters(final List<HvdcConverter> converters,
                                           final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("converters");
        jsonGenerator.writeStartArray();
        for (final HvdcConverter converter : converters) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("node", converter.node());
            jsonGenerator.writeStringField("station", converter.station());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeHvdcLines(final List<HvdcLine> lines,
                                           final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName("lines");
        jsonGenerator.writeStartArray();
        for (final HvdcLine line : lines) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("id", line.id());
            jsonGenerator.writeStringField("from", line.from());
            jsonGenerator.writeStringField("to", line.to());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
