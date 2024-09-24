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

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonObjectiveFunctionParameters {

    private JsonObjectiveFunctionParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(OBJECTIVE_FUNCTION);
        jsonGenerator.writeNumberField(CURATIVE_MIN_OBJ_IMPROVEMENT, parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals(CURATIVE_MIN_OBJ_IMPROVEMENT)) {
                jsonParser.nextToken();
                searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(jsonParser.getValueAsDouble());
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize objective function parameters: unexpected field in %s (%s)", OBJECTIVE_FUNCTION, jsonParser.getCurrentName()));
            }
        }
    }
}
