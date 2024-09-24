/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonTopoOptimizationParameters {

    private JsonTopoOptimizationParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(TOPOLOGICAL_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(MAX_PREVENTIVE_SEARCH_TREE_DEPTH, parameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth());
        jsonGenerator.writeNumberField(MAX_AUTO_SEARCH_TREE_DEPTH, parameters.getTopoOptimizationParameters().getMaxAutoSearchTreeDepth());
        jsonGenerator.writeNumberField(MAX_CURATIVE_SEARCH_TREE_DEPTH, parameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth());
        jsonGenerator.writeFieldName(PREDEFINED_COMBINATIONS);
        jsonGenerator.writeStartArray();
        for (List<String> naIdCombination : parameters.getTopoOptimizationParameters().getPredefinedCombinations()) {
            jsonGenerator.writeStartArray();
            for (String naId : naIdCombination) {
                jsonGenerator.writeString(naId);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeBooleanField(SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT, parameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        jsonGenerator.writeNumberField(MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS, parameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MAX_PREVENTIVE_SEARCH_TREE_DEPTH:
                    jsonParser.nextToken();
                    searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(jsonParser.getIntValue());
                    break;
                case MAX_AUTO_SEARCH_TREE_DEPTH:
                    jsonParser.nextToken();
                    searchTreeParameters.getTopoOptimizationParameters().setMaxAutoSearchTreeDepth(jsonParser.getIntValue());
                    break;
                case MAX_CURATIVE_SEARCH_TREE_DEPTH:
                    jsonParser.nextToken();
                    searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(jsonParser.getIntValue());
                    break;
                case PREDEFINED_COMBINATIONS:
                    searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(readListOfListOfString(jsonParser));
                    break;
                case SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT:
                    jsonParser.nextToken();
                    searchTreeParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(jsonParser.getBooleanValue());
                    break;
                case MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS:
                    jsonParser.nextToken();
                    searchTreeParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(jsonParser.getIntValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize topological optimization parameters: unexpected field in %s (%s)", TOPOLOGICAL_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
            }
        }
    }

    private static List<List<String>> readListOfListOfString(JsonParser jsonParser) throws IOException {
        List<List<String>> parsedListOfList = new ArrayList<>();
        jsonParser.nextToken();
        while (!jsonParser.nextToken().isStructEnd()) {
            parsedListOfList.add(jsonParser.readValueAs(ArrayList.class));
        }
        return parsedListOfList;
    }
}
