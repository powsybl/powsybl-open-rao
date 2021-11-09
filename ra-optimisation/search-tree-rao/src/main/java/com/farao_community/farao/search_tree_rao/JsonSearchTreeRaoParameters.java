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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        jsonGenerator.writeNumberField("preventive-leaves-in-parallel", searchTreeRaoParameters.getPreventiveLeavesInParallel());
        jsonGenerator.writeNumberField("curative-leaves-in-parallel", searchTreeRaoParameters.getCurativeLeavesInParallel());
        jsonGenerator.writeObjectField("preventive-rao-stop-criterion", searchTreeRaoParameters.getPreventiveRaoStopCriterion());
        jsonGenerator.writeObjectField("curative-rao-stop-criterion", searchTreeRaoParameters.getCurativeRaoStopCriterion());
        jsonGenerator.writeNumberField("curative-rao-min-obj-improvement", searchTreeRaoParameters.getCurativeRaoMinObjImprovement());
        jsonGenerator.writeBooleanField("skip-network-actions-far-from-most-limiting-element", searchTreeRaoParameters.getSkipNetworkActionsFarFromMostLimitingElement());
        jsonGenerator.writeNumberField("max-number-of-boundaries-for-skipping-network-actions", searchTreeRaoParameters.getMaxNumberOfBoundariesForSkippingNetworkActions());
        jsonGenerator.writeNumberField("max-curative-ra", searchTreeRaoParameters.getMaxCurativeRa());
        jsonGenerator.writeNumberField("max-curative-tso", searchTreeRaoParameters.getMaxCurativeTso());
        jsonGenerator.writeObjectField("max-curative-topo-per-tso", searchTreeRaoParameters.getMaxCurativeTopoPerTso());
        jsonGenerator.writeObjectField("max-curative-pst-per-tso", searchTreeRaoParameters.getMaxCurativePstPerTso());
        jsonGenerator.writeObjectField("max-curative-ra-per-tso", searchTreeRaoParameters.getMaxCurativeRaPerTso());
        jsonGenerator.writeBooleanField("curative-rao-optimize-operators-not-sharing-cras", searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras());
        jsonGenerator.writeObjectField("second-preventive-optimization-condition", searchTreeRaoParameters.getSecondPreventiveOptimizationCondition());

        jsonGenerator.writeFieldName("network-action-combinations");
        jsonGenerator.writeStartArray();
        for (List<String> naIdCombination : searchTreeRaoParameters.getNetworkActionIdCombinations()) {
            jsonGenerator.writeStartArray();
            for (String naId : naIdCombination) {
                jsonGenerator.writeString(naId);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndArray();
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
                case "preventive-leaves-in-parallel":
                    parameters.setPreventiveLeavesInParallel(jsonParser.getValueAsInt());
                    break;
                case "curative-leaves-in-parallel":
                    parameters.setCurativeLeavesInParallel(jsonParser.getValueAsInt());
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
                case "skip-network-actions-far-from-most-limiting-element":
                    parameters.setSkipNetworkActionsFarFromMostLimitingElement(jsonParser.getValueAsBoolean());
                    break;
                case "max-number-of-boundaries-for-skipping-network-actions":
                    parameters.setMaxNumberOfBoundariesForSkippingNetworkActions(jsonParser.getValueAsInt());
                    break;
                case "max-curative-ra":
                    jsonParser.nextToken();
                    parameters.setMaxCurativeRa(jsonParser.getValueAsInt());
                    break;
                case "max-curative-tso":
                    jsonParser.nextToken();
                    parameters.setMaxCurativeTso(jsonParser.getValueAsInt());
                    break;
                case "max-curative-topo-per-tso":
                    jsonParser.nextToken();
                    parameters.setMaxCurativeTopoPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case "max-curative-pst-per-tso":
                    jsonParser.nextToken();
                    parameters.setMaxCurativePstPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case "max-curative-ra-per-tso":
                    jsonParser.nextToken();
                    parameters.setMaxCurativeRaPerTso(readStringToPositiveIntMap(jsonParser));
                    break;
                case "curative-rao-optimize-operators-not-sharing-cras":
                    parameters.setCurativeRaoOptimizeOperatorsNotSharingCras(jsonParser.getValueAsBoolean());
                    break;
                case "second-preventive-optimization-condition":
                    parameters.setSecondPreventiveOptimizationCondition(getSecondPreventiveRaoConditionFromString(jsonParser.nextTextValue()));
                    break;
                case "network-action-combinations":
                    parameters.setNetworkActionIdCombinations(readListOfListOfString(jsonParser));
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        return parameters;
    }

    private Map<String, Integer> readStringToPositiveIntMap(JsonParser jsonParser) throws IOException {
        HashMap<String, Integer> map = jsonParser.readValueAs(HashMap.class);
        // Check types
        map.forEach((Object o, Object o2) -> {
            if (!(o instanceof String) || !(o2 instanceof Integer)) {
                throw new FaraoException("Unexpected key or value type in a Map<String, Integer> parameter!");
            }
            if ((int) o2 < 0) {
                throw new FaraoException("Unexpected negative integer!");
            }
        });
        return map;
    }

    private List<List<String>> readListOfListOfString(JsonParser jsonParser) throws IOException {
        List<List<String>> parsedListOfList = new ArrayList<>();
        jsonParser.nextToken();
        while (!jsonParser.nextToken().isStructEnd()) {
            parsedListOfList.add(jsonParser.readValueAs(ArrayList.class));
        }
        return parsedListOfList;
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

    private SearchTreeRaoParameters.SecondPreventiveRaoCondition getSecondPreventiveRaoConditionFromString(String string) {
        try {
            return SearchTreeRaoParameters.SecondPreventiveRaoCondition.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown second preventive RAO stop criterion: %s", string));
        }
    }
}
