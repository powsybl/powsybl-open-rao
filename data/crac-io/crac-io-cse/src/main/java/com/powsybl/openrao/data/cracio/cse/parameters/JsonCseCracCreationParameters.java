/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonCseCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<CseCracCreationParameters> {

    private static final String RANGE_ACTION_GROUPS = "range-action-groups";
    private static final String BUS_BAR_CHANGE_SWITCHES = "bus-bar-change-switches";
    private static final String REMEDIAL_ACTION_ID = "remedial-action-id";
    private static final String SWITCH_PAIRS = "switch-pairs";
    private static final String OPEN = "open";
    private static final String CLOSE = "close";

    private static final String UNEXPECTED_FIELD = "Unexpected field: %s";

    @Override
    public void serialize(CseCracCreationParameters cseParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeRangeActionGroups(cseParameters, jsonGenerator);
        serializeBusBarChangeSwitchesSet(cseParameters, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public CseCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CseCracCreationParameters());
    }

    @Override
    public CseCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CseCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case RANGE_ACTION_GROUPS:
                    jsonParser.nextToken();
                    parameters.setRangeActionGroupsAsString(jsonParser.readValueAs(ArrayList.class));
                    break;
                case BUS_BAR_CHANGE_SWITCHES:
                    jsonParser.nextToken();
                    parameters.setBusBarChangeSwitchesSet(deserializeBusBarChangeSwitchesSet(jsonParser));
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, jsonParser.getCurrentName()));
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CseCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CseCracCreationParameters> getExtensionClass() {
        return CseCracCreationParameters.class;
    }

    private void serializeRangeActionGroups(CseCracCreationParameters cseParameters, JsonGenerator jsonGenerator) throws IOException {
        serializeStringArray(RANGE_ACTION_GROUPS, cseParameters.getRangeActionGroupsAsString(), jsonGenerator);
    }

    private void serializeBusBarChangeSwitchesSet(CseCracCreationParameters cseParameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(BUS_BAR_CHANGE_SWITCHES);
        jsonGenerator.writeStartArray();
        for (BusBarChangeSwitches busBarChangeSwitches : cseParameters.getBusBarChangeSwitchesSet()) {
            serializeBusBarChangeSwitches(busBarChangeSwitches, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private void serializeBusBarChangeSwitches(BusBarChangeSwitches busBarChangeSwitches, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(REMEDIAL_ACTION_ID, busBarChangeSwitches.getRemedialActionId());
        jsonGenerator.writeArrayFieldStart(SWITCH_PAIRS);
        for (SwitchPairId switchPairId : busBarChangeSwitches.getSwitchPairs()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(OPEN, switchPairId.getSwitchToOpenId());
            jsonGenerator.writeStringField(CLOSE, switchPairId.getSwitchToCloseId());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private void serializeStringArray(String fieldName, List<String> content, JsonGenerator jsonGenerator) throws IOException {
        if (!content.isEmpty()) {
            jsonGenerator.writeArrayFieldStart(fieldName);
            for (String string : content) {
                jsonGenerator.writeString(string);
            }
            jsonGenerator.writeEndArray();
        }
    }

    private Set<BusBarChangeSwitches> deserializeBusBarChangeSwitchesSet(JsonParser jsonParser) throws IOException {
        Set<String> ids = new HashSet<>();
        Set<BusBarChangeSwitches> set = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String remedialActionId = null;
            Set<SwitchPairId> switchPairs = new HashSet<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case REMEDIAL_ACTION_ID:
                        remedialActionId = jsonParser.nextTextValue();
                        break;
                    case SWITCH_PAIRS:
                        jsonParser.nextToken();
                        switchPairs = deserializeSwitchPairIds(jsonParser);
                        break;
                    default:
                        throw new OpenRaoException(String.format(UNEXPECTED_FIELD, jsonParser.getCurrentName()));
                }
            }
            if (remedialActionId == null) {
                throw new OpenRaoException(String.format("Missing remedial action ID in %s", BUS_BAR_CHANGE_SWITCHES));
            }
            if (ids.contains(remedialActionId)) {
                throw new OpenRaoException(String.format("Remedial action %s has two or more associated %s lists", remedialActionId, BUS_BAR_CHANGE_SWITCHES));
            }
            ids.add(remedialActionId);
            set.add(new BusBarChangeSwitches(remedialActionId, switchPairs));
        }
        return set;
    }

    private Set<SwitchPairId> deserializeSwitchPairIds(JsonParser jsonParser) throws IOException {
        Set<SwitchPairId> switchPairs = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String switchToOpen = null;
            String switchToClose = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case OPEN:
                        switchToOpen = jsonParser.nextTextValue();
                        break;
                    case CLOSE:
                        switchToClose = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new OpenRaoException(String.format(UNEXPECTED_FIELD, jsonParser.getCurrentName()));
                }
            }
            if (switchToOpen == null || switchToClose == null) {
                throw new OpenRaoException("Missing switch to open or switch to close in switch pair");
            }
            switchPairs.add(new SwitchPairId(switchToOpen, switchToClose));
        }
        return switchPairs;
    }
}
