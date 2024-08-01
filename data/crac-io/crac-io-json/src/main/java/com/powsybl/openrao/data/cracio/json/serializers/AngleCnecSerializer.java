/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
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
public class AngleCnecSerializer<I extends AngleCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I angleCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, angleCnec.getId());
        gen.writeStringField(NAME, angleCnec.getName());
        gen.writeStringField(EXPORTING_NETWORK_ELEMENT_ID, angleCnec.getExportingNetworkElement().getId());
        gen.writeStringField(IMPORTING_NETWORK_ELEMENT_ID, angleCnec.getImportingNetworkElement().getId());
        gen.writeStringField(OPERATOR, angleCnec.getOperator());
        gen.writeStringField(BORDER, angleCnec.getBorder());
        gen.writeStringField(INSTANT, angleCnec.getState().getInstant().getId());
        Optional<Contingency> optContingency = angleCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(OPTIMIZED, angleCnec.isOptimized());
        gen.writeObjectField(MONITORED, angleCnec.isMonitored());
        gen.writeNumberField(RELIABILITY_MARGIN, angleCnec.getReliabilityMargin());

        serializeThresholds(angleCnec, gen);

        JsonUtil.writeExtensions(angleCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeThresholds(AngleCnec angleCnec, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(THRESHOLDS);
        List<Threshold> sortedListOfThresholds = angleCnec.getThresholds().stream()
            .sorted(new ThresholdComparator())
            .toList();
        for (Threshold threshold : sortedListOfThresholds) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }
}
