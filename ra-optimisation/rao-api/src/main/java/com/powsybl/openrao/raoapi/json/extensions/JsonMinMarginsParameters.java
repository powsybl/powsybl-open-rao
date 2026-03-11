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
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.COSTLY_MIN_MARGIN_PARAMETERS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.SHIFTED_VIOLATION_PENALTY;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.SHIFTED_VIOLATION_THRESHOLD;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
final class JsonMinMarginsParameters {

    private JsonMinMarginsParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<SearchTreeRaoCostlyMinMarginParameters> minMarginsParameters = parameters.getMinMarginsParameters();
        if (minMarginsParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(COSTLY_MIN_MARGIN_PARAMETERS);
            jsonGenerator.writeObjectField(SHIFTED_VIOLATION_PENALTY, minMarginsParameters.get().getShiftedViolationPenalty());
            jsonGenerator.writeObjectField(SHIFTED_VIOLATION_THRESHOLD, minMarginsParameters.get().getShiftedViolationThreshold());
            jsonGenerator.writeEndObject();
        }

    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        SearchTreeRaoCostlyMinMarginParameters minMarginsParameters = new SearchTreeRaoCostlyMinMarginParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case SHIFTED_VIOLATION_PENALTY -> minMarginsParameters.setShiftedViolationPenalty(jsonParser.getValueAsDouble());
                case SHIFTED_VIOLATION_THRESHOLD -> minMarginsParameters.setShiftedViolationThreshold(jsonParser.getValueAsDouble());
                default -> throw new OpenRaoException(String.format(
                    "Cannot deserialize min margins parameters: unexpected field in %s (%s)",
                    COSTLY_MIN_MARGIN_PARAMETERS,
                    jsonParser.getCurrentName())
                );
            }
            searchTreeParameters.setMinMarginsParameters(minMarginsParameters);
        }
    }
}
