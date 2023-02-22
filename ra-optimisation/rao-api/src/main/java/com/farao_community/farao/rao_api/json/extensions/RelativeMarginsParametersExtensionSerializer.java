/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class RelativeMarginsParametersExtensionSerializer implements JsonRaoParameters.ExtensionSerializer<RelativeMarginParametersExtension> {

    @Override
    public void serialize(RelativeMarginParametersExtension relativeMarginParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart(PTDF_BOUNDARIES);
        for (String countryPair : relativeMarginParameters.getPtdfBoundariesAsString()) {
            jsonGenerator.writeString(countryPair);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeNumberField(PTDF_SUM_LOWER_BOUND, relativeMarginParameters.getPtdfSumLowerBound());
        jsonGenerator.writeEndObject();
    }

    @Override
    public RelativeMarginParametersExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, RelativeMarginParametersExtension.loadDefault());
    }

    @Override
    public RelativeMarginParametersExtension deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, RelativeMarginParametersExtension parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PTDF_BOUNDARIES:
                    readPtdfBoundaries(jsonParser, parameters);
                    break;
                case PTDF_SUM_LOWER_BOUND:
                    jsonParser.nextToken();
                    parameters.setPtdfSumLowerBound(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize relative margins parameters: unexpected field in %s (%s)", RELATIVE_MARGINS, jsonParser.getCurrentName()));
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return RELATIVE_MARGINS_PARAMETERS_EXTENSION_NAME;
    }

    @Override
    public String getCategoryName() {
        return RELATIVE_MARGINS;
    }

    @Override
    public Class<? super RelativeMarginParametersExtension> getExtensionClass() {
        return RelativeMarginParametersExtension.class;
    }

    private void readPtdfBoundaries(JsonParser parser, RelativeMarginParametersExtension parameters) throws IOException {
        if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
            List<String> boundaries = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                boundaries.add(parser.getValueAsString());
            }
            parameters.setPtdfBoundariesFromString(boundaries);
        }
    }

}
