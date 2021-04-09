/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.network_action;

import com.farao_community.farao.data.crac_impl.json.JsonSerializationNames;
import com.farao_community.farao.data.crac_impl.TopologicalActionImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class TopologySerializer extends JsonSerializer<TopologicalActionImpl> {

    @Override
    public void serialize(TopologicalActionImpl topology, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObjectField(NETWORK_ELEMENT, topology.getNetworkElement().getId());
        jsonGenerator.writeStringField(JsonSerializationNames.ACTION_TYPE, topology.getActionType().toString());
    }

    @Override
    public void serializeWithType(TopologicalActionImpl topology, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(topology, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(topology, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }

}
