/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class SimpleCracSerializer extends JsonSerializer<SimpleCrac> {
    @Override
    public void serialize(SimpleCrac value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStringField(ID, value.getId());
        gen.writeStringField(NAME, value.getName());
        if (value.getNetworkDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
            gen.writeStringField(NETWORK_DATE, dateFormat.format(value.getNetworkDate().toDate()));
        }
        gen.writeArrayFieldStart(NETWORK_ELEMENTS);
        for (NetworkElement networkElement: value.getNetworkElements()) {
            gen.writeObject(networkElement);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(INSTANTS);
        for (Instant instant : value.getInstants()) {
            gen.writeObject(instant);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(CONTINGENCIES);
        for (Contingency contingency : value.getContingencies()) {
            gen.writeObject(contingency);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(STATES);
        for (State state : value.getStates()) {
            gen.writeObject(state);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(CNECS);
        for (BranchCnec cnec : value.getBranchCnecs()) {
            gen.writeObject(cnec);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(RANGE_ACTIONS);
        for (RangeAction rangeAction: value.getRangeActions()) {
            gen.writeObject(rangeAction);
        }
        gen.writeEndArray();
        gen.writeArrayFieldStart(NETWORK_ACTIONS);
        for (NetworkAction networkAction : value.getNetworkActions()) {
            gen.writeObject(networkAction);
        }
        gen.writeEndArray();

        JsonUtil.writeExtensions(value, gen, serializers, ExtensionsHandler.getExtensionsSerializers());
    }

    @Override
    public void serializeWithType(SimpleCrac value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        WritableTypeId writableTypeId = typeSer.typeId(value, JsonToken.START_OBJECT);
        typeSer.writeTypePrefix(gen, writableTypeId);
        serialize(value, gen, serializers);
        typeSer.writeTypeSuffix(gen, writableTypeId);
    }
}
