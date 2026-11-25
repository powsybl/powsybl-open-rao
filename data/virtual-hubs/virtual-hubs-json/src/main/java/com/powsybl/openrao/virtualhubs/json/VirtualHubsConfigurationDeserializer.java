/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationDeserializer extends JsonDeserializer<VirtualHubsConfiguration> {
    @Override
    public VirtualHubsConfiguration deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
        final Map<String, SynchronousArea> synchronousAreas = new TreeMap<>();
        final Map<String, ControlArea> controlAreas = new TreeMap<>();
        final Map<String, MarketArea> marketAreas = new TreeMap<>();
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "region" -> deserializeRegion(jsonParser, configuration);
                case "synchronousAreas" -> deserializeSynchronousAreas(jsonParser, configuration, synchronousAreas);
                case "controlAreas" -> deserializeControlAreas(jsonParser, configuration, controlAreas);
                case "marketAreas" ->
                    deserializeMarketAreas(jsonParser, configuration, synchronousAreas, controlAreas, marketAreas);
                case "virtualHubs" -> deserializeVirtualHubs(jsonParser, configuration, marketAreas);
                case "borderDirections" -> deserializeBorderDirections(jsonParser, configuration);
                case "internalHvdcs" -> deserializeInternalHvdcs(jsonParser, configuration, marketAreas);
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for configuration", jsonParser.currentName()));
            }
        }
        return configuration;
    }

    private void deserializeRegion(final JsonParser jsonParser, final VirtualHubsConfiguration configuration) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        String code = null;
        String eic = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for region", jsonParser.currentName()));
            }
        }
        final Region region = new Region(code, eic);
        configuration.setRegion(region);
    }

    private void deserializeSynchronousAreas(final JsonParser jsonParser,
                                             final VirtualHubsConfiguration configuration,
                                             final Map<String, SynchronousArea> synchronousAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeSynchronousArea(jsonParser, configuration, synchronousAreas);
        }
    }

    private void deserializeParameters(final JsonParser jsonParser,
                                       final Map<String, String> parameters) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            parameters.put(jsonParser.currentName(), jsonParser.nextTextValue());
        }
    }

    private void deserializeVoltages(final JsonParser jsonParser,
                                     final List<Voltage> voltages) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeVoltage(jsonParser, voltages);
        }
    }

    private void deserializeControlAreas(final JsonParser jsonParser,
                                         final VirtualHubsConfiguration configuration,
                                         final Map<String, ControlArea> controlAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all control areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeControlArea(jsonParser, configuration, controlAreas);
        }
    }

    private void deserializeMarketAreas(final JsonParser jsonParser,
                                        final VirtualHubsConfiguration configuration,
                                        final Map<String, SynchronousArea> synchronousAreas,
                                        final Map<String, ControlArea> controlAreas,
                                        final Map<String, MarketArea> marketAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all market areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeMarketArea(jsonParser, configuration, synchronousAreas, controlAreas, marketAreas);
        }
    }

    private void deserializeVirtualHubs(final JsonParser jsonParser,
                                        final VirtualHubsConfiguration configuration,
                                        final Map<String, MarketArea> marketAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all virtual hubs
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeVirtualHub(jsonParser, configuration, marketAreas);
        }
    }

    private void deserializeBorderDirections(final JsonParser jsonParser, final VirtualHubsConfiguration configuration) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all border directions
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeBorderDirection(jsonParser, configuration);
        }
    }

    private void deserializeInternalHvdcs(final JsonParser jsonParser,
                                          final VirtualHubsConfiguration configuration,
                                          final Map<String, MarketArea> marketAreas) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all border directions
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeInternalHvdc(jsonParser, configuration, marketAreas);
        }
    }

    private void deserializeHvdcPoles(final JsonParser jsonParser,
                                      final List<HvdcPole> poles) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeHvdcPole(jsonParser, poles);
        }
    }

    private void deserializeHvdcConverters(final JsonParser jsonParser,
                                           final List<HvdcConverter> converters) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeHvdcConverter(jsonParser, converters);
        }
    }

    private void deserializeHvdcLines(final JsonParser jsonParser,
                                      final List<HvdcLine> lines) throws IOException {
        jsonParser.nextToken(); // Enter inside list and iterate on all synchronous areas
        while (!jsonParser.nextToken().isStructEnd()) {
            deserializeHvdcLine(jsonParser, lines);
        }
    }

    private void deserializeSynchronousArea(final JsonParser jsonParser,
                                            final VirtualHubsConfiguration configuration,
                                            final Map<String, SynchronousArea> synchronousAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isLive = false;
        Boolean isMainArea = false;
        Map<String, String> localLOT3Parameters = null;
        List<Voltage> initialVoltages = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "isLive" -> isLive = jsonParser.nextBooleanValue();
                case "isMainArea" -> isMainArea = jsonParser.nextBooleanValue();
                case "localLOT3Parameters" -> {
                    localLOT3Parameters = new TreeMap<>();
                    deserializeParameters(jsonParser, localLOT3Parameters);
                }
                case "initialVoltages" -> {
                    initialVoltages = new ArrayList<>();
                    deserializeVoltages(jsonParser, initialVoltages);
                }
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for synchronous area", jsonParser.currentName()));
            }
        }
        final SynchronousArea synchronousArea = new SynchronousArea(code, eic, isLive, isMainArea, localLOT3Parameters, initialVoltages);
        synchronousAreas.put(code, synchronousArea);
        configuration.addSynchronousArea(synchronousArea);
    }

    private void deserializeVoltage(final JsonParser jsonParser,
                                    final List<Voltage> voltages) throws IOException {
        BigDecimal nominal = null;
        BigDecimal initial = null;
        int slackSearchPriority = 0;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "nominal" -> {
                    jsonParser.nextToken();
                    nominal = jsonParser.getDecimalValue();
                }
                case "initial" -> {
                    jsonParser.nextToken();
                    initial = jsonParser.getDecimalValue();
                }
                case "slackSearchPriority" -> slackSearchPriority = jsonParser.nextIntValue(0);
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for voltage", jsonParser.currentName()));
            }
        }
        final Voltage voltage = new Voltage(nominal, initial, slackSearchPriority);
        voltages.add(voltage);
    }

    private void deserializeControlArea(final JsonParser jsonParser,
                                        final VirtualHubsConfiguration configuration,
                                        final Map<String, ControlArea> controlAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isMcParticipant = false;
        Boolean isTechCounterparty = false;
        Boolean isSdac = false;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "isMcParticipant" -> isMcParticipant = jsonParser.nextBooleanValue();
                case "isTechCounterparty" -> isTechCounterparty = jsonParser.nextBooleanValue();
                case "isSdac" -> isSdac = jsonParser.nextBooleanValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for control area", jsonParser.currentName()));
            }
        }
        final ControlArea controlArea = new ControlArea(code, eic, isMcParticipant, isTechCounterparty, isSdac);
        controlAreas.put(code, controlArea);
        configuration.addControlArea(controlArea);
    }

    private void deserializeMarketArea(final JsonParser jsonParser,
                                       final VirtualHubsConfiguration configuration,
                                       final Map<String, SynchronousArea> synchronousAreas,
                                       final Map<String, ControlArea> controlAreas,
                                       final Map<String, MarketArea> marketAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isMcParticipant = false;
        Boolean isAhc = false;
        Boolean isTechCounterparty = false;
        Boolean isSdac = false;
        SynchronousArea synchronousArea = null;
        ControlArea relatedCa = null;
        Boolean isItcp = false;
        Boolean isIgnoreECs = false;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "isMcParticipant" -> isMcParticipant = jsonParser.nextBooleanValue();
                case "isAhc" -> isAhc = jsonParser.nextBooleanValue();
                case "isTechCounterparty" -> isTechCounterparty = jsonParser.nextBooleanValue();
                case "isSdac" -> isSdac = jsonParser.nextBooleanValue();
                case "synchronousArea" -> synchronousArea = synchronousAreas.get(jsonParser.nextTextValue());
                case "relatedCa" -> relatedCa = controlAreas.get(jsonParser.nextTextValue());
                case "isItcp" -> isItcp = jsonParser.nextBooleanValue();
                case "isIgnoreECs" -> isIgnoreECs = jsonParser.nextBooleanValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for market area", jsonParser.currentName()));
            }
        }
        final MarketArea marketArea = new MarketArea(code, eic, isMcParticipant, isAhc, isTechCounterparty, isSdac, synchronousArea, relatedCa, isItcp, isIgnoreECs);
        marketAreas.put(code, marketArea);
        configuration.addMarketArea(marketArea);
    }

    private void deserializeVirtualHub(final JsonParser jsonParser,
                                       final VirtualHubsConfiguration configuration,
                                       final Map<String, MarketArea> marketAreas) throws IOException {
        String code = null;
        String eic = null;
        Boolean isMcParticipant = null;
        Boolean isAhc = false;
        String nodeName = null;
        MarketArea relatedMa = null;
        String oppositeHub = null;
        Boolean isSdac = false;
        BigDecimal splittingFactor = null;
        BigDecimal lf = null;
        BigDecimal rlf = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "isMcParticipant" -> isMcParticipant = jsonParser.nextBooleanValue();
                case "isAhc" -> isAhc = jsonParser.nextBooleanValue();
                case "nodeName" -> nodeName = jsonParser.nextTextValue();
                case "relatedMa" -> relatedMa = marketAreas.get(jsonParser.nextTextValue());
                case "oppositeHub" -> oppositeHub = jsonParser.nextTextValue();
                case "isSdac" -> isSdac = jsonParser.nextBooleanValue();
                case "splittingFactor" -> {
                    jsonParser.nextToken();
                    splittingFactor = jsonParser.getDecimalValue();
                }
                case "lf" -> {
                    jsonParser.nextToken();
                    lf = jsonParser.getDecimalValue();
                }
                case "rlf" -> {
                    jsonParser.nextToken();
                    rlf = jsonParser.getDecimalValue();
                }
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for virtual hub", jsonParser.currentName()));
            }
        }
        final VirtualHub virtualHub = new VirtualHub(code, eic, isMcParticipant, isAhc, nodeName, relatedMa, oppositeHub, isSdac, splittingFactor, lf, rlf);
        configuration.addVirtualHub(virtualHub);
    }

    private void deserializeBorderDirection(final JsonParser jsonParser, final VirtualHubsConfiguration configuration) throws IOException {
        String from = null;
        String to = null;
        Boolean isAhc = false;
        Boolean isSdac = false;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "from" -> from = jsonParser.nextTextValue();
                case "to" -> to = jsonParser.nextTextValue();
                case "isAhc" -> isAhc = jsonParser.nextBooleanValue();
                case "isSdac" -> isSdac = jsonParser.nextBooleanValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for border direction", jsonParser.currentName()));
            }
        }
        final BorderDirection borderDirection = new BorderDirection(from, to, isAhc, isSdac);
        configuration.addBorderDirection(borderDirection);
    }

    private void deserializeInternalHvdc(final JsonParser jsonParser,
                                         final VirtualHubsConfiguration configuration,
                                         final Map<String, MarketArea> marketAreas) throws IOException {
        String code = null;
        String eic = null;
        MarketArea relatedMa = null;
        Boolean isMthvdc = false;
        Boolean isBipolar = false;
        List<HvdcPole> poles = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "code" -> code = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "poles" -> {
                    poles = new ArrayList<>();
                    deserializeHvdcPoles(jsonParser, poles);
                }
                case "relatedMa" -> relatedMa = marketAreas.get(jsonParser.nextTextValue());
                case "isMthvdc" -> isMthvdc = jsonParser.nextBooleanValue();
                case "isBipolar" -> isBipolar = jsonParser.nextBooleanValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for internal HVDC", jsonParser.currentName()));
            }
        }
        final InternalHvdc internalHvdc = new InternalHvdc(eic, code, poles, relatedMa, isMthvdc, isBipolar);
        configuration.addInternalHvdc(internalHvdc);
    }

    private void deserializeHvdcPole(final JsonParser jsonParser,
                                     final List<HvdcPole> poles) throws IOException {
        String id = null;
        String eic = null;
        List<HvdcConverter> converters = null;
        List<HvdcLine> lines = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "id" -> id = jsonParser.nextTextValue();
                case "eic" -> eic = jsonParser.nextTextValue();
                case "converters" -> {
                    converters = new ArrayList<>();
                    deserializeHvdcConverters(jsonParser, converters);
                }
                case "lines" -> {
                    lines = new ArrayList<>();
                    deserializeHvdcLines(jsonParser, lines);
                }
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for HVDC pole", jsonParser.currentName()));
            }
        }
        final HvdcPole pole = new HvdcPole(id, eic, converters, lines);
        poles.add(pole);
    }

    private void deserializeHvdcConverter(final JsonParser jsonParser,
                                          final List<HvdcConverter> converters) throws IOException {
        String node = null;
        String station = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "node" -> node = jsonParser.nextTextValue();
                case "station" -> station = jsonParser.nextTextValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for HVDC converter", jsonParser.currentName()));
            }
        }
        final HvdcConverter converter = new HvdcConverter(node, station);
        converters.add(converter);
    }

    private void deserializeHvdcLine(final JsonParser jsonParser,
                                     final List<HvdcLine> lines) throws IOException {
        String id = null;
        String from = null;
        String to = null;
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case "id" -> id = jsonParser.nextTextValue();
                case "from" -> from = jsonParser.nextTextValue();
                case "to" -> to = jsonParser.nextTextValue();
                default ->
                    throw new VirtualHubsConfigurationDeserializationException(String.format("Attribute '%s' invalid for HVDC line", jsonParser.currentName()));
            }
        }
        final HvdcLine line = new HvdcLine(id, from, to);
        lines.add(line);
    }
}
