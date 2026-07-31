/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.ENFORCE_CURATIVE_SECURITY;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.OBJECTIVE_FUNCTION;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.TYPE;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonObjectiveFunctionParameters {

    private JsonObjectiveFunctionParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectField(OBJECTIVE_FUNCTION, parameters.getObjectiveFunctionParameters());
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        raoParameters.setObjectiveFunctionParameters(jsonParser.getCodec().readValue(jsonParser, com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters.class));
    }

    private static ObjectiveFunctionParameters.ObjectiveFunctionType stringToObjectiveFunction(String string) {
        try {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.valueOf(string);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Unknown objective function type value: %s", string));
        }
    }

}
