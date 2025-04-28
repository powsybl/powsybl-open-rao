/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonRangeActionsOptimizationParameters {

    private JsonRangeActionsOptimizationParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RANGE_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(PST_RA_MIN_IMPACT_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getPstRAMinImpactThreshold());
        jsonGenerator.writeNumberField(HVDC_RA_MIN_IMPACT_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getHvdcRAMinImpactThreshold());
        jsonGenerator.writeNumberField(INJECTION_RA_MIN_IMPACT_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getInjectionRAMinImpactThreshold());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PST_RA_MIN_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                case HVDC_RA_MIN_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                case INJECTION_RA_MIN_IMPACT_THRESHOLD:
                    jsonParser.nextToken();
                    raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize range action optimization parameters: unexpected field in %s (%s)", RANGE_ACTIONS_OPTIMIZATION, jsonParser.getCurrentName()));
            }
        }
    }
}
