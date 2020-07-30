/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonCnecLoopFlowExtensionSerializer implements ExtensionsHandler.ExtensionSerializer<Cnec, CnecLoopFlowExtension> {

    @Override
    public void serialize(CnecLoopFlowExtension cnecLoopFlowExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("inputThreshold", cnecLoopFlowExtension.getInputThreshold());
        jsonGenerator.writeObjectField("inputThresholdUnit", cnecLoopFlowExtension.getInputThresholdUnit().name());
        jsonGenerator.writeEndObject();
    }

    @Override
    public CnecLoopFlowExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        double inputThreshold = Double.NaN;
        Unit unit = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case "inputThreshold":
                    inputThreshold = jsonParser.getValueAsDouble();
                    break;
                case "inputThresholdUnit":
                    unit = Unit.valueOf(jsonParser.nextTextValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }
        if (Double.isNaN(inputThreshold)|| unit == null) {
            throw new FaraoException("The CnecLoopFlowExtension should include both the 'inputThreshold' and the 'inputThresholdUnit' fields");
        }

        return new CnecLoopFlowExtension(inputThreshold, unit);
    }

    @Override
    public String getExtensionName() {
        return "CnecLoopFlowExtension";
    }

    @Override
    public String getCategoryName() {
        return "cnec-extension";
    }

    @Override
    public Class<? super CnecLoopFlowExtension> getExtensionClass() {
        return CnecLoopFlowExtension.class;
    }
}
