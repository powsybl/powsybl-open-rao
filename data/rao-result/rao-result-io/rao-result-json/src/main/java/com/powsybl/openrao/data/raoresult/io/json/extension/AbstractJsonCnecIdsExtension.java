/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.raoresult.api.extension.AbstractCnecIdsExtension;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public abstract class AbstractJsonCnecIdsExtension<E extends AbstractCnecIdsExtension> implements RaoResultJsonUtils.ExtensionSerializer<E> {

    @Override
    public void serialize(E extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        List<String> sortedListofFlowCnecIds = extension.getCriticalCnecIds().stream().sorted().toList();

        jsonGenerator.writeStartArray();
        for (String consideredCnec : sortedListofFlowCnecIds) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(RaoResultJsonConstants.FLOWCNEC_ID, consideredCnec);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public E deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, E extension) throws IOException {
        Set<String> criticalCnecsIdSet = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.nextFieldName().equals(RaoResultJsonConstants.FLOWCNEC_ID)) {
                criticalCnecsIdSet.add(jsonParser.nextTextValue());
            }
            jsonParser.nextToken();
        }
        extension.setCriticalCnecIds(criticalCnecsIdSet);

        return extension;
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }
}
