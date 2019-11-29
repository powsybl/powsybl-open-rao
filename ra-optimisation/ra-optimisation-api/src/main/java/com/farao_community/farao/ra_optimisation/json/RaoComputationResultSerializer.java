/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

public class RaoComputationResultSerializer extends StdSerializer<RaoComputationResult> {

    RaoComputationResultSerializer() {
        super(RaoComputationResult.class);
    }

    @Override
    public void serialize(RaoComputationResult result, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("status", result.getStatus().name());
        jsonGenerator.writeObjectField("preContingencyResult", result.getPreContingencyResult());
        jsonGenerator.writeObjectField("contingencyResults", result.getContingencyResults());
        JsonUtil.writeExtensions(result, jsonGenerator, serializerProvider, JsonRaoComputationResult.getExtensionSerializers());
        jsonGenerator.writeEndObject();
    }
}
