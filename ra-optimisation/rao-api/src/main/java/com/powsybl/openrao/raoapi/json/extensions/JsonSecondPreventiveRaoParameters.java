/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonSecondPreventiveRaoParameters {

    private JsonSecondPreventiveRaoParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(SECOND_PREVENTIVE_RAO);
        jsonGenerator.writeObjectField(EXECUTION_CONDITION, parameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        jsonGenerator.writeBooleanField(RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS, parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions());
        jsonGenerator.writeBooleanField(HINT_FROM_FIRST_PREVENTIVE_RAO, parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case EXECUTION_CONDITION:
                    searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(stringToExecutionCondition(jsonParser.nextTextValue()));
                    break;
                case RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    searchTreeParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(jsonParser.getBooleanValue());
                    break;
                case HINT_FROM_FIRST_PREVENTIVE_RAO:
                    jsonParser.nextToken();
                    searchTreeParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(jsonParser.getBooleanValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize second preventive rao parameters: unexpected field in %s (%s)", SECOND_PREVENTIVE_RAO, jsonParser.getCurrentName()));
            }
        }
    }

    private static SecondPreventiveRaoParameters.ExecutionCondition stringToExecutionCondition(String string) {
        try {
            return SecondPreventiveRaoParameters.ExecutionCondition.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown execution condition value: %s", string));
        }
    }
}
