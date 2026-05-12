/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.ADD_UNSECURE_CNECS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.FAST_RAO_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.MARGIN_LIMIT;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.NUMBER_OF_CNECS_TO_ADD;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonFastRaoParameters implements JsonRaoParameters.ExtensionSerializer<FastRaoParameters> {

    @Override
    public void serialize(FastRaoParameters fastRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(NUMBER_OF_CNECS_TO_ADD, fastRaoParameters.getNumberOfCnecsToAdd());
        jsonGenerator.writeBooleanField(ADD_UNSECURE_CNECS, fastRaoParameters.getAddUnsecureCnecs());
        jsonGenerator.writeNumberField(MARGIN_LIMIT, fastRaoParameters.getMarginLimit());
        jsonGenerator.writeEndObject();
    }

    @Override
    public FastRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        FastRaoParameters fastRaoParameters = new FastRaoParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case NUMBER_OF_CNECS_TO_ADD -> {
                    jsonParser.nextToken();
                    fastRaoParameters.setNumberOfCnecsToAdd(jsonParser.getIntValue());
                }
                case ADD_UNSECURE_CNECS -> {
                    jsonParser.nextToken();
                    fastRaoParameters.setAddUnsecureCnecs(jsonParser.getBooleanValue());
                }
                case MARGIN_LIMIT -> {
                    jsonParser.nextToken();
                    fastRaoParameters.setMarginLimit(jsonParser.getDoubleValue());
                }
                default -> throw new OpenRaoException(String.format("Cannot deserialize fast rao parameters: unexpected field in %s (%s)", FAST_RAO_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        return fastRaoParameters;
    }

    @Override
    public String getExtensionName() {
        return "fast-rao-parameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super FastRaoParameters> getExtensionClass() {
        return FastRaoParameters.class;
    }
}
