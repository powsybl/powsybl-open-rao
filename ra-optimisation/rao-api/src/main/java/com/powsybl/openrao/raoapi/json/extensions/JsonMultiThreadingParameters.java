/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.powsybl.openrao.commons.OpenRaoException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class JsonMultiThreadingParameters {

    private JsonMultiThreadingParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(MULTI_THREADING);
        jsonGenerator.writeNumberField(AVAILABLE_CPUS, Math.max(
            parameters.getMultithreadingParameters().getContingencyScenariosInParallel(),
            parameters.getMultithreadingParameters().getPreventiveLeavesInParallel()));
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.getCurrentName().equals(AVAILABLE_CPUS)) {
                jsonParser.nextToken();
                int availableCpus = jsonParser.getIntValue();
                searchTreeParameters.getMultithreadingParameters().setContingencyScenariosInParallel(availableCpus);
                searchTreeParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(availableCpus);
                searchTreeParameters.getMultithreadingParameters().setAutoLeavesInParallel(1);
                searchTreeParameters.getMultithreadingParameters().setCurativeLeavesInParallel(1);
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize multi-threading parameters: unexpected field in %s (%s)", MULTI_THREADING, jsonParser.getCurrentName()));
            }
        }
    }
}
