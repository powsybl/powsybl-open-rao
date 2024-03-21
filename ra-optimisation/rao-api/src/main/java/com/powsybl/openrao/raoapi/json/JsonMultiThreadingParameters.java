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
final class JsonMultiThreadingParameters {

    private JsonMultiThreadingParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(MULTI_THREADING);
        jsonGenerator.writeNumberField(CONTINGENCY_SCENARIOS_IN_PARALLEL, parameters.getMultithreadingParameters().getContingencyScenariosInParallel());
        jsonGenerator.writeNumberField(PREVENTIVE_LEAVES_IN_PARALLEL, parameters.getMultithreadingParameters().getPreventiveLeavesInParallel());
        jsonGenerator.writeNumberField(AUTO_LEAVES_IN_PARALLEL, parameters.getMultithreadingParameters().getAutoLeavesInParallel());
        jsonGenerator.writeNumberField(CURATIVE_LEAVES_IN_PARALLEL, parameters.getMultithreadingParameters().getCurativeLeavesInParallel());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case CONTINGENCY_SCENARIOS_IN_PARALLEL:
                    jsonParser.nextToken();
                    raoParameters.getMultithreadingParameters().setContingencyScenariosInParallel(jsonParser.getIntValue());
                    break;
                case PREVENTIVE_LEAVES_IN_PARALLEL:
                    jsonParser.nextToken();
                    raoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(jsonParser.getIntValue());
                    break;
                case AUTO_LEAVES_IN_PARALLEL:
                    jsonParser.nextToken();
                    raoParameters.getMultithreadingParameters().setAutoLeavesInParallel(jsonParser.getIntValue());
                    break;
                case CURATIVE_LEAVES_IN_PARALLEL:
                    jsonParser.nextToken();
                    raoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(jsonParser.getIntValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize multi-threading parameters: unexpected field in %s (%s)", MULTI_THREADING, jsonParser.getCurrentName()));
            }
        }
    }
}
