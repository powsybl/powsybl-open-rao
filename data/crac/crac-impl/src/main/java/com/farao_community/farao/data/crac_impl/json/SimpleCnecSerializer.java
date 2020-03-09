/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCnecSerializer extends JsonSerializer<SimpleCnec> {


    @Override
    public void serialize(SimpleCnec cnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {


            jsonGenerator.writeStringField("id", cnec.getId());
            jsonGenerator.writeStringField("name", cnec.getName());
            jsonGenerator.writeStringField("networkElement", cnec.getNetworkElement().getId());
            jsonGenerator.writeObjectField("state", cnec.getState().getId());

            jsonGenerator.writeFieldName("thresholds");
            jsonGenerator.writeStartArray();
            for (AbstractThreshold threshold : cnec.getThresholds()) {
                jsonGenerator.writeObject(threshold);
            }
            jsonGenerator.writeEndArray();

            JsonUtil.writeExtensions(cnec, jsonGenerator, serializerProvider, ExtensionsHandler.getCnecExtensionSerializers());

    }


    @Override
    public void serializeWithType(SimpleCnec cnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(cnec, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(cnec, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
/*
    @Override
    public String property() {
        return "id";
    }

    @Override
    public Class<? extends ObjectIdGenerator<?>> generator() {
        return ObjectIdGenerators.PropertyGenerator.class;
    }

    @Override
    public Class<? extends ObjectIdResolver> resolver() {
        return SimpleObjectIdResolver.class;
    }

    @Override
    public Class<?> scope() {
        return Object.class;
    }*/
}
