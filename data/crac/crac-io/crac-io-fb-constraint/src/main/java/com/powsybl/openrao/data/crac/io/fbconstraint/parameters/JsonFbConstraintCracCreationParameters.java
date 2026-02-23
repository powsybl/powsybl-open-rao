/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.fbconstraint.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonFbConstraintCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<FbConstraintCracCreationParameters> {

    private static final String TIMESTAMP = "timestamp";
    private static final String ICS_COST_UP = "ics-cost-up";
    private static final String ICS_COST_DOWN = "ics-cost-down";

    @Override
    public String getExtensionName() {
        return "FbConstraintCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super FbConstraintCracCreationParameters> getExtensionClass() {
        return FbConstraintCracCreationParameters.class;
    }

    @Override
    public void serialize(FbConstraintCracCreationParameters fbConstraintParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTimestamp(fbConstraintParameters.getTimestamp(), jsonGenerator);
        jsonGenerator.writeNumberField(ICS_COST_UP, fbConstraintParameters.getIcsCostUp());
        jsonGenerator.writeNumberField(ICS_COST_DOWN, fbConstraintParameters.getIcsCostDown());
        jsonGenerator.writeEndObject();
    }

    @Override
    public FbConstraintCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, FbConstraintCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case TIMESTAMP -> {
                    jsonParser.nextToken();
                    parameters.setTimestamp(OffsetDateTime.parse(jsonParser.readValueAs(String.class)));
                }
                case ICS_COST_UP -> {
                    jsonParser.nextToken();
                    parameters.setIcsCostUp(jsonParser.readValueAs(Double.class));
                }
                case ICS_COST_DOWN -> {
                    jsonParser.nextToken();
                    parameters.setIcsCostDown(jsonParser.readValueAs(Double.class));
                }
                default -> throw new OpenRaoException("Unexpected field: " + jsonParser.currentName());
            }
        }

        return parameters;
    }

    private void serializeTimestamp(OffsetDateTime timestamp, JsonGenerator jsonGenerator) throws IOException {
        if (timestamp != null) {
            jsonGenerator.writeStringField(TIMESTAMP, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(timestamp));
        }
    }

    @Override
    public FbConstraintCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new FbConstraintCracCreationParameters());
    }
}
