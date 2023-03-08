/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.SecondPreventiveRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonSecondPreventiveRaoParameters {

    private JsonSecondPreventiveRaoParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(SECOND_PREVENTIVE_RAO);
        jsonGenerator.writeObjectField(EXECUTION_CONDITION, parameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        jsonGenerator.writeBooleanField(RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS, parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions());
        jsonGenerator.writeBooleanField(HINT_FROM_FIRST_PREVENTIVE_RAO, parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case EXECUTION_CONDITION:
                    raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(stringToExecutionCondition(jsonParser.nextTextValue()));
                    break;
                case RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    raoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(jsonParser.getBooleanValue());
                    break;
                case HINT_FROM_FIRST_PREVENTIVE_RAO:
                    jsonParser.nextToken();
                    raoParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(jsonParser.getBooleanValue());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize second preventive rao parameters: unexpected field in %s (%s)", SECOND_PREVENTIVE_RAO, jsonParser.getCurrentName()));
            }
        }
    }

    private static SecondPreventiveRaoParameters.ExecutionCondition stringToExecutionCondition(String string) {
        try {
            return SecondPreventiveRaoParameters.ExecutionCondition.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unknown execution condition value: %s", string));
        }
    }
}
