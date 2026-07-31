/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.PTDF_BOUNDARIES;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.RELATIVE_MARGINS;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class JsonRelativeMarginsParameters {

    private JsonRelativeMarginsParameters() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        if (parameters.getRelativeMarginsParameters().isPresent()) {
            jsonGenerator.writeObjectField(RELATIVE_MARGINS, parameters.getRelativeMarginsParameters().get());
        }
    }

    static void deserialize(JsonParser jsonParser, RaoParameters raoParameters) throws IOException {
        raoParameters.setRelativeMarginsParameters(jsonParser.getCodec().readValue(jsonParser, com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters.class));
    }

}
