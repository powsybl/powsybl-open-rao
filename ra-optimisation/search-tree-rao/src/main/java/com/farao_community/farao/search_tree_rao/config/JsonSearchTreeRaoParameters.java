/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.config;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonSearchTreeRaoParameters implements JsonRaoParameters.ExtensionSerializer<SearchTreeRaoParameters> {

    @Override
    public void serialize(SearchTreeRaoParameters searchTreeRaoParameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("range-action-rao", searchTreeRaoParameters.getRangeActionRao());
        jsonGenerator.writeEndObject();
    }

    @Override
    public SearchTreeRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        SearchTreeRaoParameters parameters = new SearchTreeRaoParameters();

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "range-action-rao":
                    parameters.setRangeActionRao(jsonParser.nextTextValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "SearchTreeRaoParameters";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-parameters";
    }

    @Override
    public Class<? super SearchTreeRaoParameters> getExtensionClass() {
        return SearchTreeRaoParameters.class;
    }
}
