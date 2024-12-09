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
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonLoopFlowParameters {

    private JsonLoopFlowParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        Optional<LoopFlowParameters> optionalLoopFlowParameters = parameters.getLoopFlowParameters();
        if (optionalLoopFlowParameters.isPresent()) {
            jsonGenerator.writeObjectFieldStart(LOOP_FLOW_PARAMETERS);
            jsonGenerator.writeObjectField(PTDF_APPROXIMATION, optionalLoopFlowParameters.get().getPtdfApproximation());
            jsonGenerator.writeNumberField(CONSTRAINT_ADJUSTMENT_COEFFICIENT, optionalLoopFlowParameters.get().getConstraintAdjustmentCoefficient());
            jsonGenerator.writeNumberField(VIOLATION_COST, optionalLoopFlowParameters.get().getViolationCost());
            jsonGenerator.writeEndObject();
        }
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        LoopFlowParameters loopFlowParameters = new LoopFlowParameters();
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case PTDF_APPROXIMATION:
                    loopFlowParameters.setPtdfApproximation(stringToPtdfApproximation(jsonParser.nextTextValue()));
                    break;
                case CONSTRAINT_ADJUSTMENT_COEFFICIENT:
                    jsonParser.nextToken();
                    loopFlowParameters.setConstraintAdjustmentCoefficient(jsonParser.getDoubleValue());
                    break;
                case VIOLATION_COST:
                    jsonParser.nextToken();
                    loopFlowParameters.setViolationCost(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize loop flow parameters: unexpected field in %s (%s)", LOOP_FLOW_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        searchTreeParameters.setLoopFlowParameters(loopFlowParameters);
    }
}
