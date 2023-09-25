/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

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
        gen.writeStringField(INSTANT, serializeInstant(voltageCnec.getState().getInstant()));
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
