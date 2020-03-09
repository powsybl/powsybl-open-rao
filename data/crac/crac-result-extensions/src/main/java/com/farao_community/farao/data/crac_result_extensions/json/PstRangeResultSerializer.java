/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(ExtensionsHandler.PstRangeExtensionSerializer.class)
public class PstRangeResultSerializer extends RangeActionResultSerializer<PstRange, PstRangeResult> implements ExtensionsHandler.PstRangeExtensionSerializer<PstRangeResult> {

    @Override
    public void serialize(PstRangeResult pstRangeResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        super.writeSetPointPerStates(pstRangeResult, jsonGenerator);
        jsonGenerator.writeFieldName("tapPerStates");
        jsonGenerator.writeStartObject();
        pstRangeResult.getStates().forEach(state -> {
            try {
                jsonGenerator.writeNumberField(state, pstRangeResult.getTap(state));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();
    }

    @Override
    public PstRangeResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectReader objectReader = JsonUtil.createObjectMapper().readerFor(PstRangeResult.class);
        return objectReader.readValue(jsonParser);
    }

    @Override
    public String getExtensionName() {
        return "PstRangeResult";
    }

    @Override
    public String getCategoryName() {
        return "pst-range";
    }

    @Override
    public Class<? super PstRangeResult> getExtensionClass() {
        return PstRangeResult.class;
    }
}
