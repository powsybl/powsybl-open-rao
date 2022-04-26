/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
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
public class JsonCimCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<CimCracCreationParameters> {

    private static final String RANGE_ACTION_GROUPS = "range-action-groups";

    @Override
    public void serialize(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeRangeActionGroups(cimParameters, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public CimCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CimCracCreationParameters());
    }

    @Override
    public CimCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CimCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (RANGE_ACTION_GROUPS.equals(jsonParser.getCurrentName())) {
                jsonParser.nextToken();
                parameters.setRangeActionGroupsAsString(jsonParser.readValueAs(ArrayList.class));
            } else {
                throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "CimCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super CimCracCreationParameters> getExtensionClass() {
        return CimCracCreationParameters.class;
    }

    private void serializeRangeActionGroups(CimCracCreationParameters cimParameters, JsonGenerator jsonGenerator) throws IOException {
        serializeStringArray(RANGE_ACTION_GROUPS, cimParameters.getRangeActionGroupsAsString(), jsonGenerator);
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
}
