/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class SimpleCracSerializer extends JsonSerializer<SimpleCrac> {
    @Override
    public void serialize(SimpleCrac value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStringField("id", value.getId());
        gen.writeStringField("name", value.getName());
        // ajout des field et fermer le flux a chaque fois
        // test mettre un point d'arret apres l'export dans l crac roundtri
        // ajouter serializer ds les json module
        gen.writeFieldName("networkElements");
        gen.writeStartArray();
        for (NetworkElement networkElement: value.getNetworkElements()) {
            gen.writeObject(networkElement);
        }
        gen.writeEndArray();
        gen.writeFieldName("instants");
        gen.writeStartArray();
        for (Instant instant : value.getInstants()) {
            gen.writeObject(instant);
        }
        gen.writeEndArray();
        gen.writeFieldName("states");
        gen.writeStartArray();
        for (State state : value.getStates()) {
            gen.writeObject(state);
        }
        gen.writeEndArray();
        gen.writeFieldName("cnecs");
        gen.writeStartArray();
        for (Cnec cnec : value.getCnecs()) {
            gen.writeObject(cnec);
        }
        gen.writeEndArray();
        gen.writeFieldName("contingencies");
        gen.writeStartArray();
        for (Contingency contingency : value.getContingencies()) {
            gen.writeObject(contingency);
        }
        gen.writeEndArray();
        gen.writeFieldName("rangeActions");
        gen.writeStartArray();
        for (RangeAction rangeAction: value.getRangeActions()) {
            gen.writeObject(rangeAction);
        }
        gen.writeEndArray();
        gen.writeFieldName("networkActions");
        gen.writeStartArray();
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
