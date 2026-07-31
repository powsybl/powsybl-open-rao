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
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.HVDC_RA_MIN_IMPACT_THRESHOLD;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.INJECTION_RA_MIN_IMPACT_THRESHOLD;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.PST_RA_MIN_IMPACT_THRESHOLD;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RANGE_ACTIONS_OPTIMIZATION;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonRangeActionsOptimizationParameters {

    private JsonRangeActionsOptimizationParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectField(RANGE_ACTIONS_OPTIMIZATION, parameters.getRangeActionsOptimizationParameters());
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        raoParameters.setRangeActionsOptimizationParameters(jsonParser.getCodec().readValue(jsonParser, com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters.class));
    }
}
