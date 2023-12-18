/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.rao_api.json.extensions;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.rao_api.json.JsonRaoParameters;
import com.powsybl.open_rao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.powsybl.open_rao.rao_api.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonRelativeMarginsParametersExtension implements JsonRaoParameters.ExtensionSerializer<RelativeMarginsParametersExtension> {

    @Override
    public void serialize(RelativeMarginsParametersExtension relativeMarginParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart(PTDF_BOUNDARIES);
        for (String ptdfBoundary : relativeMarginParameters.getPtdfBoundariesAsString()) {
            jsonGenerator.writeString(ptdfBoundary);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeObjectField(PTDF_APPROXIMATION, relativeMarginParameters.getPtdfApproximation());
        jsonGenerator.writeNumberField(PTDF_SUM_LOWER_BOUND, relativeMarginParameters.getPtdfSumLowerBound());
        jsonGenerator.writeEndObject();
    }

    @Override
    public RelativeMarginsParametersExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new RelativeMarginsParametersExtension());
    }

    @Override
    public RelativeMarginsParametersExtension deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, RelativeMarginsParametersExtension parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PTDF_BOUNDARIES:
                    readPtdfBoundaries(jsonParser, parameters);
                    break;
                case PTDF_APPROXIMATION:
                    parameters.setPtdfApproximation(stringToPtdfApproximation(jsonParser.nextTextValue()));
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
        return RELATIVE_MARGINS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super RelativeMarginsParametersExtension> getExtensionClass() {
        return RelativeMarginsParametersExtension.class;
    }

    private void readPtdfBoundaries(JsonParser parser, RelativeMarginsParametersExtension parameters) throws IOException {
        if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
            List<String> boundaries = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                boundaries.add(parser.getValueAsString());
            }
            parameters.setPtdfBoundariesFromString(boundaries);
        }
    }

}
