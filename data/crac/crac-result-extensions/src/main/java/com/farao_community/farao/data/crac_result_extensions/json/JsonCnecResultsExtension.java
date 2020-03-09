/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultsExtension;
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
@AutoService(ExtensionsHandler.CnecExtensionSerializer.class)
public class JsonCnecResultsExtension implements ExtensionsHandler.CnecExtensionSerializer<CnecResultsExtension> {

    @Override
    public void serialize(CnecResultsExtension cnecResults, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("cnecResults", cnecResults.getCnecResultMap());
        jsonGenerator.writeEndObject();

    }

    @Override
    public CnecResultsExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        CnecResultsExtension cnecResultsExtension = new CnecResultsExtension();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "cnecResults":
                    jsonParser.nextToken();
                    Map<String, CnecResult> resultMap = jsonParser.readValueAs(new TypeReference<Map<String, CnecResult>>() {
                    });
                    resultMap.forEach(cnecResultsExtension::addVariant);
                    break;

                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return cnecResultsExtension;
    }

    @Override
    public String getExtensionName() {
        return "CnecResultsExtension";
    }

    @Override
    public String getCategoryName() {
        return "cnec";
    }

    @Override
    public Class<? super CnecResultsExtension> getExtensionClass() {
        return CnecResultsExtension.class;
    }
}



