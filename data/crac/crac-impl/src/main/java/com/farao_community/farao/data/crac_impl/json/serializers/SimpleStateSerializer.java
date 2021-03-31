/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class SimpleStateSerializer extends JsonSerializer<SimpleState> {
    @Override
    public void serialize(SimpleState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(CONTINGENCY, value.getContingency().map(Identifiable::getId).orElse(null));
        gen.writeStringField(INSTANT, value.getInstant().toString());
    }

    @Override
    public void serializeWithType(SimpleState value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        WritableTypeId writableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, writableTypeId);
        serialize(value, gen, serializers);
        typeSer.writeTypeSuffix(gen, writableTypeId);
    }
}
