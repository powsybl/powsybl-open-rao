/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersCommons.*;

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
                case CURATIVE_LEAVES_IN_PARALLEL:
                    jsonParser.nextToken();
                    raoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(jsonParser.getIntValue());
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize multi-threading parameters: unexpected field in %s (%s)", MULTI_THREADING, jsonParser.getCurrentName()));
            }
        }
    }
}
