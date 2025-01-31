/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonTopoOptimizationParameters {

    private JsonTopoOptimizationParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(TOPOLOGICAL_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(RELATIVE_MINIMUM_IMPACT_THRESHOLD, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        jsonGenerator.writeNumberField(ABSOLUTE_MINIMUM_IMPACT_THRESHOLD, parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case RELATIVE_MINIMUM_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                case ABSOLUTE_MINIMUM_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize topological optimization parameters: unexpected field in %s (%s)", TOPOLOGICAL_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
            }
        }
    }
}
