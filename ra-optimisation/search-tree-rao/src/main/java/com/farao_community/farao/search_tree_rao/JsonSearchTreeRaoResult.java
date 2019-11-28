/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@AutoService(JsonRaoComputationResult.ExtensionSerializer.class)
public class JsonSearchTreeRaoResult implements  JsonRaoComputationResult.ExtensionSerializer<SearchTreeRaoResult> {

    @Override
    public void serialize(SearchTreeRaoResult searchTreeRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("computationStatus", searchTreeRaoResult.getComputationStatus());
        jsonGenerator.writeObjectField("stopCriterion", searchTreeRaoResult.getStopCriterion());
        jsonGenerator.writeEndObject();
    }

    @Override
    public SearchTreeRaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        SearchTreeRaoResult resultExtension = new SearchTreeRaoResult();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "computationStatus":
                    jsonParser.nextToken();
                    resultExtension.setComputationStatus(jsonParser.getValueAsString());
                    break;
                case "stopCriterion":
                    jsonParser.nextToken();
                    resultExtension.setStopCriterion(jsonParser.getValueAsString());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        return resultExtension;
    }

    @Override
    public String getExtensionName() {
        return "SearchTreeRaoResult";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-result";
    }

    @Override
    public Class<? super SearchTreeRaoResult> getExtensionClass() {
        return SearchTreeRaoResult.class;
    }
}
