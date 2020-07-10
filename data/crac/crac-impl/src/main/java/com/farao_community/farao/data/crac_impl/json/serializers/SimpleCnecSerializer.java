/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCnecSerializer extends JsonSerializer<SimpleCnec> {

    @Override
    public void serialize(SimpleCnec cnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField(ID, cnec.getId());
        jsonGenerator.writeStringField(NAME, cnec.getName());
        jsonGenerator.writeStringField(NETWORK_ELEMENT, cnec.getNetworkElement().getId());
        jsonGenerator.writeObjectField(STATE, cnec.getState().getId());
        jsonGenerator.writeObjectField(FRM, cnec.getFrm());
        jsonGenerator.writeObjectField(OPTIMIZED, cnec.isOptimized());
        jsonGenerator.writeObjectField(MONITORED, cnec.isMonitored());

        jsonGenerator.writeFieldName(THRESHOLDS);
        jsonGenerator.writeStartArray();
        for (Threshold threshold: cnec.getThresholds()) {
            jsonGenerator.writeObject(threshold);
        }
        jsonGenerator.writeEndArray();

        JsonUtil.writeExtensions(cnec, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }

    @Override
    public void serializeWithType(SimpleCnec cnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(cnec, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(cnec, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
