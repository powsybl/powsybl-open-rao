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
final class MultiThreadingParametersSerializer {

    private MultiThreadingParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(MULTI_THREADING);
        jsonGenerator.writeNumberField(CONTINGENCY_SCENARIOS_IN_PARALLEL, parameters.getMultithreadingParameters().getContingencyScenariosInParallel());
        jsonGenerator.writeNumberField(PREVENTIVE_LEAVES_IN_PARALLEL, parameters.getMultithreadingParameters().getPreventiveLeavesInParallel());
        jsonGenerator.writeNumberField(CURATIVE_LEAVES_IN_PARALLEL, parameters.getMultithreadingParameters().getCurativeLeavesInParallel());
        jsonGenerator.writeEndObject();
    }
}
