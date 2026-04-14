/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@AutoService(JsonCracCreationParameters.ExtensionSerializer.class)
public class JsonSweNcCracCreationParameters implements JsonCracCreationParameters.ExtensionSerializer<SweNcCracCreationParameters> {

    private static final String TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE = "tsos-which-do-not-use-patl-in-final-state";

    @Override
    public void serialize(SweNcCracCreationParameters ncParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        serializeTsosWhichDoNotUsePatlInFinalState(ncParameters.getTsosWhichDoNotUsePatlInFinalState(), jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    @Override
    public SweNcCracCreationParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, SweNcCracCreationParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.currentName().equals(TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE)) {
                jsonParser.nextToken();
                parameters.setTsosWhichDoNotUsePatlInFinalState(jsonParser.readValueAs(new TypeReference<HashSet<String>>() {}));
            } else {
                throw new OpenRaoException("Unexpected field: " + jsonParser.currentName());
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "SweNcCracCreatorParameters";
    }

    @Override
    public String getCategoryName() {
        return "crac-creation-parameters";
    }

    @Override
    public Class<? super SweNcCracCreationParameters> getExtensionClass() {
        return SweNcCracCreationParameters.class;
    }

    private void serializeTsosWhichDoNotUsePatlInFinalState(Set<String> usePatlInFinalState, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE);
        jsonGenerator.writeStartArray();
        usePatlInFinalState.forEach(tso -> {
            try {
                jsonGenerator.writeString(tso);
            } catch (IOException e) {
                throwSerializationError(TSOS_WHICH_DO_NOT_USE_PATL_IN_FINAL_STATE, e);
            }
        });
        jsonGenerator.writeEndArray();
    }

    @Override
    public SweNcCracCreationParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new SweNcCracCreationParameters());
    }

    private static void throwSerializationError(String nonSerializableField, IOException e) {
        throw new OpenRaoException("Could not serialize " + nonSerializableField + " map. Reason: " + e.getMessage());
    }
}
