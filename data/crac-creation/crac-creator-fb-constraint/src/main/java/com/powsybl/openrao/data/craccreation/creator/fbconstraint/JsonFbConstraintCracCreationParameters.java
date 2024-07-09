/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonFbConstraintCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<FbConstraintCracCreationParameters> {
    private static final String OFFSET_DATE_TIME = "offset-date-time";

    @Override
    public void serialize(FbConstraintCracCreationParameters fbConstraintParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeOffsetDateTime(fbConstraintParameters.getOffsetDateTime(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public FbConstraintCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new FbConstraintCracCreationParameters());
    }

    @Override
    public FbConstraintCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, FbConstraintCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case OFFSET_DATE_TIME:
                    jsonParser.nextToken();
                    parameters.setOffsetDateTime(OffsetDateTime.parse(jsonParser.readValueAs(String.class)));
                    break;
                default:
                    throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "FbConstraintCracCreationParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super FbConstraintCracCreationParameters> getExtensionClass() {
        return FbConstraintCracCreationParameters.class;
    }

    private void serializeOffsetDateTime(OffsetDateTime offsetDateTime, JsonGenerator jsonGenerator) throws IOException {
        if (offsetDateTime != null) {
            jsonGenerator.writeStringField(OFFSET_DATE_TIME, offsetDateTime.toString());
        }
    }
}
