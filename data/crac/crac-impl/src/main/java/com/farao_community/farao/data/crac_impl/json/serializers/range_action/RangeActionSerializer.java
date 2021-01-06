/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.range_action;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_impl.json.JsonSerializationNames;
import com.farao_community.farao.data.crac_impl.json.serializers.AbstractRemedialActionSerializer;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.AbstractRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionSerializer<E extends AbstractRangeAction> extends AbstractRemedialActionSerializer<RangeAction, E> {

    @Override
    public void serialize(E abstractRangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        super.serialize(abstractRangeAction, jsonGenerator, serializerProvider);
        jsonGenerator.writeArrayFieldStart(JsonSerializationNames.RANGES);
        for (Range range : abstractRangeAction.getRanges()) {
            jsonGenerator.writeObject(range);
        }
        jsonGenerator.writeEndArray();
        Optional<String> groupId = abstractRangeAction.getGroupId();
        if (groupId.isPresent()) {
            jsonGenerator.writeStringField("groupId", groupId.get());
        }
        JsonUtil.writeExtensions(abstractRangeAction, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }

    @Override
    public void serializeWithType(E abstractRangeAction, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(abstractRangeAction, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(abstractRangeAction, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
