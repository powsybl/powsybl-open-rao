/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

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
        jsonGenerator.writeNumberField("maximum-search-depth", searchTreeRaoParameters.getMaximumSearchDepth());
        jsonGenerator.writeNumberField("relative-network-action-minimum-impact-threshold", searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold());
        jsonGenerator.writeNumberField("absolute-network-action-minimum-impact-threshold", searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold());
        jsonGenerator.writeNumberField("leaves-in-parallel", searchTreeRaoParameters.getLeavesInParallel());
        jsonGenerator.writeObjectField("preventive-rao-stop-criterion", searchTreeRaoParameters.getPreventiveRaoStopCriterion());
        jsonGenerator.writeObjectField("curative-rao-stop-criterion", searchTreeRaoParameters.getCurativeRaoStopCriterion());
        jsonGenerator.writeNumberField("curative-rao-min-obj-improvement", searchTreeRaoParameters.getCurativeRaoMinObjImprovement());
        jsonGenerator.writeEndObject();
    }

    @Override
    public SearchTreeRaoParameters deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new SearchTreeRaoParameters());
    }

    @Override
    public SearchTreeRaoParameters deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, SearchTreeRaoParameters parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "maximum-search-depth":
                    parameters.setMaximumSearchDepth(jsonParser.getValueAsInt());
                    break;
                case "relative-network-action-minimum-impact-threshold":
                    parameters.setRelativeNetworkActionMinimumImpactThreshold(jsonParser.getValueAsDouble());
                    break;
                case "absolute-network-action-minimum-impact-threshold":
                    parameters.setAbsoluteNetworkActionMinimumImpactThreshold(jsonParser.getValueAsDouble());
                    break;
                case "leaves-in-parallel":
                    parameters.setLeavesInParallel(jsonParser.getValueAsInt());
                    break;
                case "preventive-rao-stop-criterion":
                    parameters.setPreventiveRaoStopCriterion(getPreventiveRaoStopCriterionFromString(jsonParser.nextTextValue()));
                    break;
                case "curative-rao-stop-criterion":
                    parameters.setCurativeRaoStopCriterion(getCurativeRaoStopCriterionFromString(jsonParser.nextTextValue()));
                    break;
                case "curative-rao-min-obj-improvement":
                    parameters.setCurativeRaoMinObjImprovement(jsonParser.getValueAsDouble());
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
        return "rao-parameters";
    }

    @Override
    public Class<? super SearchTreeRaoParameters> getExtensionClass() {
        return SearchTreeRaoParameters.class;
    }

    private SearchTreeRaoParameters.PreventiveRaoStopCriterion getPreventiveRaoStopCriterionFromString(String stopCriterion) {
        try {
            return SearchTreeRaoParameters.PreventiveRaoStopCriterion.valueOf(stopCriterion);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown preventive RAO stop criterion: %s", stopCriterion));
        }
    }

    private SearchTreeRaoParameters.CurativeRaoStopCriterion getCurativeRaoStopCriterionFromString(String string) {
        try {
            return SearchTreeRaoParameters.CurativeRaoStopCriterion.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown curative RAO stop criterion: %s", string));
        }
    }
}
