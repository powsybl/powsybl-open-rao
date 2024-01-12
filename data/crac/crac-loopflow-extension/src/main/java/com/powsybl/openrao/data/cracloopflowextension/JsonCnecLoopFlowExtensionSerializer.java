/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracloopflowextension;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.craciojson.ExtensionsHandler;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.util.Objects;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.deserializeUnit;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.serializeUnit;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */

@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonCnecLoopFlowExtensionSerializer implements ExtensionsHandler.ExtensionSerializer<FlowCnec, LoopFlowThreshold> {
    private static final String THRESHOLD = "inputThreshold";
    private static final String UNIT = "inputThresholdUnit";

    @Override
    public void serialize(LoopFlowThreshold cnecLoopFlowExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(THRESHOLD, cnecLoopFlowExtension.getValue());
        jsonGenerator.writeStringField(UNIT, serializeUnit(cnecLoopFlowExtension.getUnit()));
        jsonGenerator.writeEndObject();
    }

    @Override
    // TODO : replace this with an AbstractJsonExtensionDeserializer, that would have a deserializeAndAdd(json, extendable) interface
    public LoopFlowThreshold deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        double inputThreshold = Double.NaN;
        Unit unit = null;

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case THRESHOLD:
                    inputThreshold = jsonParser.getValueAsDouble();
                    break;
                case UNIT:
                    unit = deserializeUnit(jsonParser.nextTextValue());
                    break;
                default:
                    throw new OpenRaoException("Unexpected field: " + jsonParser.getCurrentName());
            }
        }

        // TODO : when constructor is replaced with adder, remove these checks
        if (Double.isNaN(inputThreshold)) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold value. Please use withValue() with a non null value");
        }
        if (Objects.isNull(unit)) {
            throw new OpenRaoException("Cannot add LoopFlowThreshold without a threshold unit. Please use withUnit() with a non null value");
        }
        if (inputThreshold < 0) {
            throw new OpenRaoException("LoopFlowThresholds must have a positive threshold.");
        }
        if (unit.equals(Unit.PERCENT_IMAX) && (inputThreshold > 1 || inputThreshold < 0)) {
            throw new OpenRaoException("LoopFlowThresholds in Unit.PERCENT_IMAX must be defined between 0 and 1, where 1 = 100%.");
        }
        if (unit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new OpenRaoException("LoopFlowThresholds can only be defined in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        return new LoopFlowThresholdImpl(inputThreshold, unit);
    }

    @Override
    public String getExtensionName() {
        return "LoopFlowThreshold";
    }

    @Override
    public String getCategoryName() {
        return "cnec-extension";
    }

    @Override
    public Class<? super LoopFlowThreshold> getExtensionClass() {
        return LoopFlowThreshold.class;
    }
}
