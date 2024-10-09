/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.threshold.Threshold;
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecSerializer<I extends VoltageCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I voltageCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, voltageCnec.getId());
        gen.writeStringField(NAME, voltageCnec.getName());
        gen.writeStringField(NETWORK_ELEMENT_ID, voltageCnec.getNetworkElement().getId());
        gen.writeStringField(OPERATOR, voltageCnec.getOperator());
        gen.writeStringField(BORDER, voltageCnec.getBorder());
        gen.writeStringField(INSTANT, voltageCnec.getState().getInstant().getId());
        Optional<Contingency> optContingency = voltageCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(OPTIMIZED, voltageCnec.isOptimized());
        gen.writeObjectField(MONITORED, voltageCnec.isMonitored());
        gen.writeNumberField(RELIABILITY_MARGIN, voltageCnec.getReliabilityMargin());

        serializeThresholds(voltageCnec, gen);

        JsonUtil.writeExtensions(voltageCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeThresholds(VoltageCnec voltageCnec, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(THRESHOLDS);
        List<Threshold> sortedListOfThresholds = voltageCnec.getThresholds().stream()
            .sorted(new ThresholdComparator())
            .toList();
        for (Threshold threshold : sortedListOfThresholds) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }
}
