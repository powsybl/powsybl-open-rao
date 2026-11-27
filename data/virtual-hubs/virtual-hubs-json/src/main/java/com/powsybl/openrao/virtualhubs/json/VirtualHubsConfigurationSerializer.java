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

import static com.powsybl.openrao.virtualhubs.json.VirtualHubsConfigurationJsonFieldNames.*;

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
            jsonGenerator.writeFieldName(REGION);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(CODE, region.code());
            jsonGenerator.writeStringField(EIC, region.eic());
            jsonGenerator.writeEndObject();
        }
    }

    private void serializeSynchronousAreas(final VirtualHubsConfiguration virtualHubsConfiguration, final JsonGenerator jsonGenerator) throws IOException {
        final List<SynchronousArea> synchronousAreas = virtualHubsConfiguration.getSynchronousAreas();
        if (!synchronousAreas.isEmpty()) {
            jsonGenerator.writeFieldName(SYNCHRONOUS_AREAS);
            jsonGenerator.writeStartArray();
            for (final SynchronousArea synchronousArea : synchronousAreas) {
                serializeSynchronousArea(synchronousArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeSynchronousArea(final SynchronousArea synchronousArea, final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(CODE, synchronousArea.code());
        jsonGenerator.writeStringField(EIC, synchronousArea.eic());
        jsonGenerator.writeBooleanField(IS_LIVE, synchronousArea.isLive());
        jsonGenerator.writeBooleanField(IS_MAIN_AREA, synchronousArea.isMainArea());
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
        jsonGenerator.writeFieldName(LOCAL_LOT_3_PARAMETERS);
        jsonGenerator.writeStartObject();
        for (final Map.Entry<String, String> parameter : localLOT3Parameters.entrySet()) {
            jsonGenerator.writeStringField(parameter.getKey(), parameter.getValue());
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeInitialVoltages(final List<Voltage> initialVoltages,
                                                 final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(INITIAL_VOLTAGES);
        jsonGenerator.writeStartArray();
        for (final Voltage voltage : initialVoltages) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField(NOMINAL, voltage.nominal());
            jsonGenerator.writeNumberField(INITIAL, voltage.initial());
            jsonGenerator.writeNumberField(SLACK_SEARCH_PRIORITY, voltage.slackSearchPriority());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeControlAreas(final VirtualHubsConfiguration virtualHubsConfiguration, final JsonGenerator jsonGenerator) throws IOException {
        final List<ControlArea> controlAreas = virtualHubsConfiguration.getControlAreas();
        if (!controlAreas.isEmpty()) {
            jsonGenerator.writeFieldName(CONTROL_AREAS);
            jsonGenerator.writeStartArray();
            for (ControlArea controlArea : controlAreas) {
                serializeControlArea(controlArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeControlArea(final ControlArea controlArea, final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(CODE, controlArea.code());
        jsonGenerator.writeStringField(EIC, controlArea.eic());
        jsonGenerator.writeBooleanField(IS_MC_PARTICIPANT, controlArea.isMcParticipant());
        jsonGenerator.writeBooleanField(IS_TECH_COUNTERPARTY, controlArea.isTechCounterparty());
        jsonGenerator.writeBooleanField(IS_SDAC, controlArea.isSdac());
        jsonGenerator.writeEndObject();
    }

    private void serializeMarketAreas(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<MarketArea> marketAreas = virtualHubsConfiguration.getMarketAreas();
        if (!marketAreas.isEmpty()) {
            jsonGenerator.writeFieldName(MARKET_AREAS);
            jsonGenerator.writeStartArray();
            for (MarketArea marketArea : marketAreas) {
                serializeMarketArea(marketArea, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeMarketArea(MarketArea marketArea, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(CODE, marketArea.code());
        jsonGenerator.writeStringField(EIC, marketArea.eic());
        jsonGenerator.writeBooleanField(IS_MC_PARTICIPANT, marketArea.isMcParticipant());
        jsonGenerator.writeBooleanField(IS_AHC, marketArea.isAhc());
        jsonGenerator.writeBooleanField(IS_TECH_COUNTERPARTY, marketArea.isTechCounterparty());
        jsonGenerator.writeBooleanField(IS_SDAC, marketArea.isSdac());
        if (marketArea.synchronousArea() != null) {
            jsonGenerator.writeStringField(SYNCHRONOUS_AREA, marketArea.synchronousArea().code());
        }
        if (marketArea.relatedCa() != null) {
            jsonGenerator.writeStringField(RELATED_CA, marketArea.relatedCa().code());
        }
        jsonGenerator.writeBooleanField(IS_ITCP, marketArea.isItcp());
        jsonGenerator.writeBooleanField(IS_IGNORE_ECS, marketArea.isIgnoreECs());
        jsonGenerator.writeEndObject();
    }

    private void serializeVirtualHubs(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<VirtualHub> virtualHubs = virtualHubsConfiguration.getVirtualHubs();
        if (!virtualHubs.isEmpty()) {
            jsonGenerator.writeFieldName(VIRTUAL_HUBS);
            jsonGenerator.writeStartArray();
            for (VirtualHub virtualHub : virtualHubs) {
                serializeVirtualHub(virtualHub, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeVirtualHub(VirtualHub virtualHub, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(CODE, virtualHub.code());
        jsonGenerator.writeStringField(EIC, virtualHub.eic());
        jsonGenerator.writeBooleanField(IS_MC_PARTICIPANT, virtualHub.isMcParticipant());
        jsonGenerator.writeBooleanField(IS_AHC, virtualHub.isAhc());
        final String nodeName = virtualHub.nodeName();
        if (nodeName != null) {
            jsonGenerator.writeStringField(NODE_NAME, nodeName);
        }
        jsonGenerator.writeStringField(RELATED_MA, virtualHub.relatedMa().code());
        final String oppositeHub = virtualHub.oppositeHub();
        if (oppositeHub != null) {
            jsonGenerator.writeStringField(OPPOSITE_HUB, oppositeHub);
        }
        jsonGenerator.writeBooleanField(IS_SDAC, virtualHub.isSdac());
        final BigDecimal splittingFactor = virtualHub.splittingFactor();
        if (splittingFactor != null) {
            jsonGenerator.writeNumberField(SPLITTING_FACTOR, splittingFactor);
        }
        final BigDecimal lf = virtualHub.lf();
        if (lf != null) {
            jsonGenerator.writeNumberField(LF, lf);
        }
        final BigDecimal rlf = virtualHub.rlf();
        if (rlf != null) {
            jsonGenerator.writeNumberField(RLF, rlf);
        }
        jsonGenerator.writeEndObject();
    }

    private void serializeBorderDirections(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<BorderDirection> borderDirections = virtualHubsConfiguration.getBorderDirections();
        if (!borderDirections.isEmpty()) {
            jsonGenerator.writeFieldName(BORDER_DIRECTIONS);
            jsonGenerator.writeStartArray();
            for (BorderDirection borderDirection : borderDirections) {
                serializeBorderDirection(borderDirection, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeBorderDirection(BorderDirection borderDirection, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(FROM, borderDirection.from());
        jsonGenerator.writeStringField(TO, borderDirection.to());
        jsonGenerator.writeBooleanField(IS_AHC, borderDirection.isAhc());
        jsonGenerator.writeBooleanField(IS_SDAC, borderDirection.isSdac());
        jsonGenerator.writeEndObject();
    }

    private void serializeInternalHvdcs(VirtualHubsConfiguration virtualHubsConfiguration, JsonGenerator jsonGenerator) throws IOException {
        final List<InternalHvdc> internalHvdcs = virtualHubsConfiguration.getInternalHvdcs();
        if (!internalHvdcs.isEmpty()) {
            jsonGenerator.writeFieldName(INTERNAL_HVDCS);
            jsonGenerator.writeStartArray();
            for (InternalHvdc internalHvdc : internalHvdcs) {
                serializeInternalHvdc(internalHvdc, jsonGenerator);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private void serializeInternalHvdc(InternalHvdc internalHvdc, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(CODE, internalHvdc.code());
        jsonGenerator.writeStringField(EIC, internalHvdc.eic());
        final List<HvdcPole> poles = internalHvdc.poles();
        if (poles != null) {
            serializeHvdcPoles(poles, jsonGenerator);
        }
        jsonGenerator.writeStringField(RELATED_MA, internalHvdc.relatedMa().code());
        jsonGenerator.writeBooleanField(IS_MTHVDC, internalHvdc.isMthvdc());
        jsonGenerator.writeBooleanField(IS_BIPOLAR, internalHvdc.isBipolar());
        jsonGenerator.writeEndObject();
    }

    private static void serializeHvdcPoles(final List<HvdcPole> poles,
                                           final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(POLES);
        jsonGenerator.writeStartArray();
        for (final HvdcPole pole : poles) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(ID, pole.id());
            jsonGenerator.writeStringField(EIC, pole.eic());
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
        jsonGenerator.writeFieldName(CONVERTERS);
        jsonGenerator.writeStartArray();
        for (final HvdcConverter converter : converters) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(NODE, converter.node());
            jsonGenerator.writeStringField(STATION, converter.station());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeHvdcLines(final List<HvdcLine> lines,
                                           final JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(LINES);
        jsonGenerator.writeStartArray();
        for (final HvdcLine line : lines) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(ID, line.id());
            jsonGenerator.writeStringField(FROM, line.from());
            jsonGenerator.writeStringField(TO, line.to());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
