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

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonFbConstraintCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<FbConstraintCracCreationParameters> {

    private static final String TIMESTAMP = "timestamp";

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
        jsonGenerator.writeEndObject();
    }

    @Override
    public FbConstraintCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, FbConstraintCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals(TIMESTAMP)) {
                jsonParser.nextToken();
                parameters.setTimestamp(OffsetDateTime.parse(jsonParser.readValueAs(String.class)));
                break;
            } else {
                throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    private void serializeTimestamp(OffsetDateTime timestamp, JsonGenerator jsonGenerator) throws IOException {
        if (timestamp != null) {
            jsonGenerator.writeStringField(TIMESTAMP, timestamp.toString());
        }
    }

    @Override
    public FbConstraintCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new FbConstraintCracCreationParameters());
    }
}
