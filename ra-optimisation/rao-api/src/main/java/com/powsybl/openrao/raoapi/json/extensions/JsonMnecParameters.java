/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.powsybl.openrao.commons.OpenRaoException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonMnecParameters {

    private JsonMnecParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<MnecParameters> optionalMnecParameters = parameters.getMnecParameters();
        if (optionalMnecParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(MNEC_PARAMETERS);
            jsonGenerator.writeNumberField(VIOLATION_COST, optionalMnecParameters.get().getViolationCost());
            jsonGenerator.writeNumberField(CONSTRAINT_ADJUSTMENT_COEFFICIENT, optionalMnecParameters.get().getConstraintAdjustmentCoefficient());
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        MnecParameters mnecParameters = new MnecParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case VIOLATION_COST:
                    jsonParser.nextToken();
                    mnecParameters.setViolationCost(jsonParser.getDoubleValue());
                    break;
                case CONSTRAINT_ADJUSTMENT_COEFFICIENT:
                    jsonParser.nextToken();
                    mnecParameters.setConstraintAdjustmentCoefficient(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize mnec parameters: unexpected field in %s (%s)", MNEC_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        searchTreeParameters.setMnecParameters(mnecParameters);
    }

}
