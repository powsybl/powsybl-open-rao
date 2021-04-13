/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_io_json.json.serializers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ContingencyImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.json.JsonSerializationNames.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ContingencyImplSerializer extends JsonSerializer<ContingencyImpl> {
    @Override
    public void serialize(ContingencyImpl value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        gen.writeArrayFieldStart(NETWORK_ELEMENTS);
        for (NetworkElement networkElement: value.getNetworkElements()) {
            gen.writeObject(networkElement.getId());
        }
        gen.writeEndArray();
    }

    @Override
    public void serializeWithType(ContingencyImpl contingency, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(contingency, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(contingency, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);

    }
}
