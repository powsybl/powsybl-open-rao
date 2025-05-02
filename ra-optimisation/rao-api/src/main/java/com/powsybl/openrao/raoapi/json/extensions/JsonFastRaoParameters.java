/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonFastRaoParameters {

    private JsonFastRaoParameters() {
    }

    static void serialize(FastRaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(FAST_RAO_PARAMETERS);
        jsonGenerator.writeNumberField(NUMBER_OF_CNECS_TO_ADD, parameters.getNumberOfCnecsToAdd());
        jsonGenerator.writeBooleanField(ADD_UNSECURE_CNECS, parameters.getAddUnsecureCnecs());
        jsonGenerator.writeNumberField(MARGIN_LIMIT, parameters.getMarginLimit());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, FastRaoParameters fastRaoParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case NUMBER_OF_CNECS_TO_ADD:
                    fastRaoParameters.setNumberOfCnecsToAdd(jsonParser.getIntValue());
                    break;
                case ADD_UNSECURE_CNECS:
                    jsonParser.nextToken();
                    fastRaoParameters.setAddUnsecureCnecs(jsonParser.getBooleanValue());
                    break;
                case MARGIN_LIMIT:
                    jsonParser.nextToken();
                    fastRaoParameters.setMarginLimit(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize fast rao parameters: unexpected field in %s (%s)", FAST_RAO_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
    }

}