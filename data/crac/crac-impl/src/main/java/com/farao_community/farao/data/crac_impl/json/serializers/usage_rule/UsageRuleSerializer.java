/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers.usage_rule;

import com.farao_community.farao.data.crac_api.UsageRule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UsageRuleSerializer<I extends UsageRule> extends JsonSerializer<I> {

    @Override
    public void serialize(I usageRule, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObjectField("usageMethod", usageRule.getUsageMethod());
        jsonGenerator.writeStringField("state", usageRule.getState().getId());
    }

    @Override
    public void serializeWithType(I usageRule, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId writableTypeId = typeSerializer.typeId(usageRule, JsonToken.START_OBJECT);
        typeSerializer.writeTypePrefix(jsonGenerator, writableTypeId);
        serialize(usageRule, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, writableTypeId);
    }
}
