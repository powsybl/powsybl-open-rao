/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecSerializer<I extends VoltageCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I voltageCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.ID, voltageCnec.getId());
        gen.writeStringField(JsonSerializationConstants.NAME, voltageCnec.getName());
        gen.writeStringField(JsonSerializationConstants.NETWORK_ELEMENT_ID, voltageCnec.getNetworkElementId());
        gen.writeStringField(JsonSerializationConstants.OPERATOR, voltageCnec.getOperator());
        gen.writeStringField(JsonSerializationConstants.BORDER, voltageCnec.getBorder());
        gen.writeStringField(JsonSerializationConstants.INSTANT, voltageCnec.getState().getInstant().getId());
        Optional<Contingency> optContingency = voltageCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(JsonSerializationConstants.CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(JsonSerializationConstants.OPTIMIZED, voltageCnec.isOptimized());
        gen.writeObjectField(JsonSerializationConstants.MONITORED, voltageCnec.isMonitored());
        gen.writeNumberField(JsonSerializationConstants.RELIABILITY_MARGIN, voltageCnec.getReliabilityMargin());

        serializeThresholds(voltageCnec, gen);

        JsonUtil.writeExtensions(voltageCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeThresholds(VoltageCnec voltageCnec, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.THRESHOLDS);
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.UNIT, JsonSerializationConstants.serializeUnit(Unit.KILOVOLT));
        Optional<Double> min = voltageCnec.getLowerBound(Unit.KILOVOLT);
        if (min.isPresent()) {
            gen.writeNumberField(JsonSerializationConstants.MIN, min.get() - voltageCnec.getReliabilityMargin());
        }
        Optional<Double> max = voltageCnec.getUpperBound(Unit.KILOVOLT);
        if (max.isPresent()) {
            gen.writeNumberField(JsonSerializationConstants.MAX, max.get() + voltageCnec.getReliabilityMargin());
        }
        gen.writeEndObject();
        gen.writeEndArray();
    }
}
