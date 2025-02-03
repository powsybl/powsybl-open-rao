/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersSerializer extends StdSerializer<RaoParameters> {

    public RaoParametersSerializer() {
        super(RaoParameters.class);
    }

    @Override
    public void serialize(RaoParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(VERSION, RAO_PARAMETERS_VERSION);
        JsonObjectiveFunctionParameters.serialize(parameters, jsonGenerator);
        JsonRangeActionsOptimizationParameters.serialize(parameters, jsonGenerator);
        JsonTopoOptimizationParameters.serialize(parameters, jsonGenerator);
        JsonNotOptimizedCnecsParameters.serialize(parameters, jsonGenerator);
        JsonMnecParameters.serialize(parameters, jsonGenerator);
        JsonRelativeMarginsParameters.serialize(parameters, jsonGenerator);
        JsonLoopFlowParameters.serialize(parameters, jsonGenerator);
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonRaoParameters.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
