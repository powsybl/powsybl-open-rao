/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.InterTemporalParametersExtension;

import java.io.IOException;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
@AutoService(JsonRaoParameters.ExtensionSerializer.class)
public class JsonInterTemporalParametersExtension implements JsonRaoParameters.ExtensionSerializer<InterTemporalParametersExtension> {

    @Override
    public void serialize(InterTemporalParametersExtension interTemporalParametersExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(SENSITIVITY_COMPUTATIONS_IN_PARALLEL, interTemporalParametersExtension.getSensitivityComputationsInParallel());
        jsonGenerator.writeEndObject();
    }

    @Override
    public InterTemporalParametersExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return deserializeAndUpdate(jsonParser, deserializationContext, new InterTemporalParametersExtension());
    }

    @Override
    public InterTemporalParametersExtension deserializeAndUpdate(JsonParser jsonParser, DeserializationContext deserializationContext, InterTemporalParametersExtension parameters) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case SENSITIVITY_COMPUTATIONS_IN_PARALLEL:
                    jsonParser.nextToken();
                    parameters.setSensitivityComputationsInParallel(jsonParser.getIntValue());
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize inter temporal parameters: unexpected field in %s (%s)", INTER_TEMPORAL_PARAMETERS, jsonParser.getCurrentName()));
            }
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return INTER_TEMPORAL_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super InterTemporalParametersExtension> getExtensionClass() {
        return InterTemporalParametersExtension.class;
    }
}
