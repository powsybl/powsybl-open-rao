/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions.json;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(ExtensionsHandler.RangeActionExtensionSerializer.class)
public class RangeActionResultSerializer<I extends RangeAction<I>, E extends RangeActionResult<I>> implements ExtensionsHandler.RangeActionExtensionSerializer<I, E> {
    protected static final String SET_POINT_PER_STATES = "setPointPerStates";

    @Override
    public void serialize(E rangeActionResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        writeSetPointPerStates(rangeActionResult, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    protected void writeSetPointPerStates(E rangeActionResult, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeFieldName(SET_POINT_PER_STATES);
        jsonGenerator.writeStartObject();
        rangeActionResult.getStates().forEach(state -> {
            try {
                jsonGenerator.writeNumberField(state, rangeActionResult.getSetPoint(state));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        jsonGenerator.writeEndObject();
    }

    @Override
    public E deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectReader objectReader = JsonUtil.createObjectMapper().readerFor(RangeActionResult.class);
        return objectReader.readValue(jsonParser);
    }

    @Override
    public String getExtensionName() {
        return "RangeActionResult";
    }

    @Override
    public String getCategoryName() {
        return "range-action";
    }

    @Override
    public Class<? super E> getExtensionClass() {
        return RangeActionResult.class;
    }
}
