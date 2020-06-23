/*
 * Copyright (c) 2018, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParametersSerializer extends StdSerializer<RaoParameters> {

    RaoParametersSerializer() {
        super(RaoParameters.class);
    }

    @Override
    public void serialize(RaoParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("version", RaoParameters.VERSION);
        jsonGenerator.writeBooleanField("rao-with-loop-flow-limitation", parameters.isRaoWithLoopFlowLimitation());
        jsonGenerator.writeBooleanField("loopflow-approximation", parameters.isLoopflowApproximation());
        jsonGenerator.writeNumberField("loopflow-constraint-adjustment-coefficient", parameters.getLoopflowConstraintAdjustmentCoefficient());
        jsonGenerator.writeNumberField("loopflow-violation-cost", parameters.getLoopflowViolationCost());
        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonRaoParameters.getExtensionSerializers());

        jsonGenerator.writeEndObject();
    }
}
