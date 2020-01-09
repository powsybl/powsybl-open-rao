/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.ra_optimisation.json.JsonRaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(JsonRaoComputationResult.ExtensionSerializer.class)
public class JsonLinearRangeActionRaoResult implements  JsonRaoComputationResult.ExtensionSerializer<LinearRangeActionRaoResult> {

    @Override
    public void serialize(LinearRangeActionRaoResult linearRangeActionRaoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        // no specific results in LinearRangeActionRaoResult yet
        jsonGenerator.writeEndObject();

    }

    @Override
    public LinearRangeActionRaoResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        // no specific results in LinearRangeActionRaoResult yet
        return new LinearRangeActionRaoResult();
    }

    @Override
    public String getExtensionName() {
        return "LinearRangeActionRaoResult";
    }

    @Override
    public String getCategoryName() {
        return "rao-computation-result";
    }

    @Override
    public Class<? super LinearRangeActionRaoResult> getExtensionClass() {
        return LinearRangeActionRaoResult.class;
    }

}
