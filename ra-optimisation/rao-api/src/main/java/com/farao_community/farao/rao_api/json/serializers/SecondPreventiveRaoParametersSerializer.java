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
final class SecondPreventiveRaoParametersSerializer {

    private SecondPreventiveRaoParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(SECOND_PREVENTIVE_RAO);
        jsonGenerator.writeObjectField(EXECUTION_CONDITION, parameters.getSecondPreventiveRaoParameters().getExecutionCondition());
        jsonGenerator.writeBooleanField(RE_OPTIMIZE_CURATIVE_RANGE_ACTIONS, parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions());
        jsonGenerator.writeBooleanField(HINT_FROM_FIRST_PREVENTIVE_RAO, parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());
        jsonGenerator.writeEndObject();
    }
}
