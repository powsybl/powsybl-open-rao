/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.serializers;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class RangeActionsOptimizationParametersSerializer {

    private RangeActionsOptimizationParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RANGE_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(MAX_MIP_ITERATIONS, parameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
        jsonGenerator.writeNumberField(PST_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getPstPenaltyCost());
        jsonGenerator.writeNumberField(PST_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
        jsonGenerator.writeObjectField(PST_MODEL, parameters.getRangeActionsOptimizationParameters().getPstModel());
        jsonGenerator.writeNumberField(HVDC_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getHvdcPenaltyCost());
        jsonGenerator.writeNumberField(HVDC_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getHvdcSensitivityThreshold());
        jsonGenerator.writeNumberField(INJECTION_RA_PENALTY_COST, parameters.getRangeActionsOptimizationParameters().getInjectionRaPenaltyCost());
        jsonGenerator.writeNumberField(INJECTION_RA_SENSITIVITY_THRESHOLD, parameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
        jsonGenerator.writeObjectFieldStart(LINEAR_OPTIMIZATION_SOLVER);
        jsonGenerator.writeObjectField(SOLVER, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver());
        jsonGenerator.writeNumberField(RELATIVE_MIP_GAP, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
        jsonGenerator.writeStringField(SOLVER_SPECIFIC_PARAMETERS, parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters());
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }
}
