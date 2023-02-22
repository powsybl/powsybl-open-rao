/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class TopoOptimizationParametersDeserializer {

    private TopoOptimizationParametersDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MAX_SEARCH_TREE_DEPTH:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(jsonParser.getIntValue());
                    break;
                case PREDEFINED_COMBINATIONS:
                    raoParameters.getTopoOptimizationParameters().setPredefinedCombinations(readListOfListOfString(jsonParser));
                    break;
                case RELATIVE_MINIMUM_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                case ABSOLUTE_MINIMUM_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                case SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(jsonParser.getBooleanValue());
                    break;
                case MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(jsonParser.getIntValue());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize topological optimization parameters: unexpected field in %s (%s)", TOPOLOGICAL_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
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
