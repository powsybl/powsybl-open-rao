/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.serializers;

import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

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
        ObjectiveFunctionParametersSerializer.serialize(parameters, jsonGenerator);
        RangeActionsOptimizationParametersSerializer.serialize(parameters, jsonGenerator);
        TopoOptimizationParametersSerializer.serialize(parameters, jsonGenerator);
        MultiThreadingParametersSerializer.serialize(parameters, jsonGenerator);
        SecondPreventiveRaoParametersSerializer.serialize(parameters, jsonGenerator);
        RaUsageLimitsPerContingencyParametersSerializer.serialize(parameters, jsonGenerator);
        NotOptimizedCnecsParametersSerializer.serialize(parameters, jsonGenerator);
        LoadFlowAndSensitivityComputationParametersSerializer.serialize(parameters, jsonGenerator, serializerProvider);
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonRaoParameters.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
