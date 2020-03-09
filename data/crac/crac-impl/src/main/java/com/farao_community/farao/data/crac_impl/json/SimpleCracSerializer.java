/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class SimpleCracSerializer extends JsonSerializer<SimpleCrac> {

    @Override
    public void serialize(SimpleCrac simpleCrac, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStringField("id", simpleCrac.getId());
        jsonGenerator.writeStringField("name", simpleCrac.getName());
        jsonGenerator.writeObjectField("networkElements", simpleCrac.getNetworkElements());
        jsonGenerator.writeObjectField("instants", simpleCrac.getInstants());
        jsonGenerator.writeObjectField("contingencies", simpleCrac.getContingencies());
        jsonGenerator.writeObjectField("states", simpleCrac.getStates());


        jsonGenerator.writeFieldName("cnecs");
        jsonGenerator.writeStartArray();
        for (Cnec cnec: simpleCrac.getCnecs()) {
            jsonGenerator.writeObject(cnec);
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeObjectField("rangeActions", simpleCrac.getRangeActions());
        jsonGenerator.writeObjectField("networkActions", simpleCrac.getNetworkActions());
    }

    @Override
    public void serializeWithType(SimpleCrac simpleCrac, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(simpleCrac, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(simpleCrac, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
