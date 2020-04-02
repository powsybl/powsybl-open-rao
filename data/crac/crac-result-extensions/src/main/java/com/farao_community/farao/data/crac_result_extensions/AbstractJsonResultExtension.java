/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public abstract class AbstractJsonResultExtension<T extends Identifiable<T>, S extends AbstractResultExtension<T, ? extends Result>>
    implements ExtensionsHandler.ExtensionSerializer<T, S> {

    @Override
    public void serialize(S resultExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        // serialize result per result as the whole map somehow skips the result types
        jsonGenerator.writeFieldName("resultsPerVariant");
        jsonGenerator.writeStartObject();
        for (Map.Entry<String, ? extends Result> entry : resultExtension.getResultMap().entrySet()) {
            jsonGenerator.writeFieldName(entry.getKey());
            jsonGenerator.writeObject(entry.getValue());
        }
        jsonGenerator.writeEndObject();

        jsonGenerator.writeEndObject();
    }

    protected void deserializeResultExtension(JsonParser jsonParser, S resultExtension) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals("resultsPerVariant")) {
                jsonParser.nextToken();
                resultExtension.setResultMap(jsonParser.readValueAs(new TypeReference<Map<String, ? extends Result>>() {
                }));
            } else {
                throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
    }
}



