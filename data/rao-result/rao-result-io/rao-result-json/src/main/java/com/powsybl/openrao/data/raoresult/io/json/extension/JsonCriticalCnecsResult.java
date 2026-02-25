/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonCriticalCnecsResult implements RaoResultJsonUtils.ExtensionSerializer<CriticalCnecsResult> {

    @Override
    public void serialize(CriticalCnecsResult criticalCnecsResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        List<String> sortedListofFlowCnecIds = criticalCnecsResult.getCriticalCnecIds().stream().sorted().toList();

        jsonGenerator.writeStartArray();
        for (String consideredCnec : sortedListofFlowCnecIds) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(RaoResultJsonConstants.FLOWCNEC_ID, consideredCnec);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

    }

    @Override
    public CriticalCnecsResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new CriticalCnecsResult());
    }

    public CriticalCnecsResult deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, CriticalCnecsResult criticalCnecsResult) throws IOException {

        Set<String> criticalCnecsIdSet = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (jsonParser.nextFieldName().equals(RaoResultJsonConstants.FLOWCNEC_ID)) {
                criticalCnecsIdSet.add(jsonParser.nextTextValue());
            }
            jsonParser.nextToken();
        }
        criticalCnecsResult.setCriticalCnecIds(criticalCnecsIdSet);

        return criticalCnecsResult;
    }

    @Override
    public String getExtensionName() {
        return "critical-cnecs-result";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super CriticalCnecsResult> getExtensionClass() {
        return CriticalCnecsResult.class;
    }
}
