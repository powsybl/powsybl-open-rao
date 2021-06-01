/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecSerializer<I extends FlowCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I flowCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(ID, flowCnec.getId());
        gen.writeStringField(NAME, flowCnec.getName());
        gen.writeStringField(NETWORK_ELEMENT_ID, flowCnec.getNetworkElement().getId());
        gen.writeStringField(OPERATOR, flowCnec.getOperator());
        gen.writeStringField(INSTANT, serializeInstant(flowCnec.getState().getInstant()));
        Optional<Contingency> optContingency = flowCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(OPTIMIZED, flowCnec.isOptimized());
        gen.writeObjectField(MONITORED, flowCnec.isMonitored());
        gen.writeNumberField(FRM, flowCnec.getReliabilityMargin());

        serializeIMax(flowCnec, gen);
        serializeNominalVoltage(flowCnec, gen);
        serializeThresholds(flowCnec, gen);

        JsonUtil.writeExtensions(flowCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeIMax(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        serializeDoubleValuesOnBothSide(gen, flowCnec.getIMax(Side.LEFT), flowCnec.getIMax(Side.RIGHT), I_MAX);
    }

    private void serializeNominalVoltage(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        serializeDoubleValuesOnBothSide(gen, flowCnec.getNominalVoltage(Side.LEFT), flowCnec.getNominalVoltage(Side.RIGHT), NOMINAL_VOLTAGE);
    }

    private void serializeDoubleValuesOnBothSide(JsonGenerator gen, Double valueSideLeft, Double valueSideRight, String fieldName) throws IOException {

        if (valueSideLeft == null && valueSideRight == null) {
            return;
        }

        gen.writeArrayFieldStart(fieldName);
        if (valueSideLeft != null && valueSideLeft.equals(valueSideRight)) {
            gen.writeNumber(valueSideLeft);
        } else {
            gen.writeNumber(valueSideLeft != null ? valueSideLeft : Double.NaN);
            gen.writeNumber(valueSideRight != null ? valueSideRight : Double.NaN);
        }
        gen.writeEndArray();
    }

    private void serializeThresholds(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(THRESHOLDS);
        List<BranchThreshold> sortedListOfThresholds = flowCnec.getThresholds().stream()
            .sorted(new ThresholdComparator())
            .collect(Collectors.toList());
        for (BranchThreshold threshold: sortedListOfThresholds) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }

    private static class ThresholdComparator implements Comparator<BranchThreshold> {
        @Override
        public int compare(BranchThreshold o1, BranchThreshold o2) {
            // TODO : once all export is done, check on CORE case that the export is deterministic
            String unit1 = serializeUnit(o1.getUnit());
            String unit2 = serializeUnit(o2.getUnit());
            if (unit1.equals(unit2)) {
                if (o1.min().isPresent()) {
                    return -1;
                }
                return 1;
            } else {
                return unit1.compareTo(unit2);
            }
        }
    }
}
