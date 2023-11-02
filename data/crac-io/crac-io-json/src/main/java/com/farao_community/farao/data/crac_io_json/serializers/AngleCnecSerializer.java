/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

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
        gen.writeStringField(INSTANT, serializeInstant(angleCnec.getState().getInstant()));
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
            .collect(Collectors.toList());
        for (Threshold threshold: sortedListOfThresholds) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }
}
