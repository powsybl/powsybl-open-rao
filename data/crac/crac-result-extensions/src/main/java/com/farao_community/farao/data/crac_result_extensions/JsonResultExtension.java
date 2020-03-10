/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */

@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonResultExtension<T extends Identifiable<T>>
    implements ExtensionsHandler.ExtensionSerializer<T, ResultExtension<T, ? extends Result<T>>> {

    @Override
    public void serialize(ResultExtension<T, ? extends Result<T>> resultExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        // serialize result per result as  the whole map somehow skip the result types
        jsonGenerator.writeFieldName("resultsPerVariant");
        jsonGenerator.writeStartObject();
        for(Map.Entry<String, ? extends Result<T>> entry : resultExtension.getResultMap().entrySet()) {
            jsonGenerator.writeFieldName(entry.getKey());
            jsonGenerator.writeObject(entry.getValue());
        }
        jsonGenerator.writeEndObject();

        jsonGenerator.writeEndObject();
    }

    @Override
    public ResultExtension<T, ? extends Result<T>> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        ResultExtension<T, ? extends Result<T>> resultExtension = new ResultExtension<>();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "resultsPerVariant":
                    jsonParser.nextToken();
                    resultExtension.setResultMap(jsonParser.readValueAs(new TypeReference<Map<String, ? extends Result<T>>>() {
                    }));
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return resultExtension;
    }

    @Override
    public String getExtensionName() {
        return "ResultExtension";
    }

    @Override
    public String getCategoryName() {
        return "identifiable";
    }

    @Override
    public Class<? super ResultExtension> getExtensionClass() {
        return ResultExtension.class;
    }
}



