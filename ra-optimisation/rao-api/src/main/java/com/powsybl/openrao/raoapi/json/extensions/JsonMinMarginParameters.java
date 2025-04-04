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
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMinMarginParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
final class JsonMinMarginParameters {

    private JsonMinMarginParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<SearchTreeRaoMinMarginParameters> minMarginParameters = parameters.getMinMarginParameters();
        if (minMarginParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(MIN_MARGIN_PARAMETERS);
            jsonGenerator.writeObjectField(OVERLOAD_PENALTY, minMarginParameters.get().getOverloadPenalty());
            jsonGenerator.writeEndObject();
        }

    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        SearchTreeRaoMinMarginParameters minMarginParameters = new SearchTreeRaoMinMarginParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case OVERLOAD_PENALTY:
                    minMarginParameters.setOverloadPenalty(jsonParser.getValueAsDouble());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize min margins parameters: unexpected field in %s (%s)", MIN_MARGIN_PARAMETERS, jsonParser.getCurrentName()));
            }
            searchTreeParameters.setMinMarginParameters(minMarginParameters);
        }
    }
}
