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
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class MultiThreadingParametersDeserializer {

    private MultiThreadingParametersDeserializer() {
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
