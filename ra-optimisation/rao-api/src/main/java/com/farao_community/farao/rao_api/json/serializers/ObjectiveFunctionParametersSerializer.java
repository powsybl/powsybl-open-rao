/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
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
final class ObjectiveFunctionParametersSerializer {

    private ObjectiveFunctionParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(OBJECTIVE_FUNCTION);
        jsonGenerator.writeObjectField(TYPE, parameters.getObjectiveFunctionParameters().getType());
        jsonGenerator.writeBooleanField(FORBID_COST_INCREASE, parameters.getObjectiveFunctionParameters().getForbidCostIncrease());
        jsonGenerator.writeObjectField(PREVENTIVE_STOP_CRITERION, parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion());
        jsonGenerator.writeObjectField(CURATIVE_STOP_CRITERION, parameters.getObjectiveFunctionParameters().getCurativeStopCriterion());
        jsonGenerator.writeNumberField(CURATIVE_MIN_OBJ_IMPROVEMENT, parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());
        jsonGenerator.writeEndObject();
    }
}
