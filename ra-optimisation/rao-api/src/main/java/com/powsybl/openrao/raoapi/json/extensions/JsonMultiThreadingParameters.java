/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
final class JsonMultiThreadingParameters {

    private JsonMultiThreadingParameters() {
    }

    static void serialize(OpenRaoSearchTreeParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(MULTI_THREADING);
        jsonGenerator.writeNumberField(AVAILABLE_CPUS, parameters.getMultithreadingParameters().getAvailableCPUs());
        jsonGenerator.writeEndObject();
    }

    static void deserialize(JsonParser jsonParser, OpenRaoSearchTreeParameters searchTreeParameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (jsonParser.currentName().equals(AVAILABLE_CPUS)) {
                jsonParser.nextToken();
                int availableCpus = jsonParser.getIntValue();
                searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(availableCpus);
            } else {
                throw new OpenRaoException(String.format("Cannot deserialize multi-threading parameters: unexpected field in %s (%s)", MULTI_THREADING, jsonParser.currentName()));
            }
        }
    }
}
