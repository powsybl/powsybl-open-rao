/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.rao_api.json;

import com.farao_community.farao.rao_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;

import java.io.IOException;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultSerializer extends StdSerializer<RaoResult> {

    RaoResultSerializer() {
        super(RaoResult.class);
    }

    @Override
    public void serialize(RaoResult raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeFieldName("status");
        JsonLoadFlowParameters.serialize(raoResult.getStatus(), jsonGenerator, serializerProvider);

        jsonGenerator.writeFieldName("preOptimVariantId");
        JsonLoadFlowParameters.serialize(raoResult.getPreOptimVariantId(), jsonGenerator, serializerProvider);

        jsonGenerator.writeFieldName("postOptimVariantId");
        JsonLoadFlowParameters.serialize(raoResult.getPostOptimVariantId(), jsonGenerator, serializerProvider);

        JsonUtil.writeExtensions(raoResult, jsonGenerator, serializerProvider, JsonRaoResult.getExtensionSerializers());

        jsonGenerator.writeEndObject();
    }

}
