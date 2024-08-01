/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.threshold.BranchThreshold;
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

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
        gen.writeStringField(BORDER, flowCnec.getBorder());
        gen.writeStringField(INSTANT, flowCnec.getState().getInstant().getId());
        Optional<Contingency> optContingency = flowCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(OPTIMIZED, flowCnec.isOptimized());
        gen.writeObjectField(MONITORED, flowCnec.isMonitored());
        gen.writeNumberField(RELIABILITY_MARGIN, flowCnec.getReliabilityMargin());

        serializeIMax(flowCnec, gen);
        serializeNominalVoltage(flowCnec, gen);
        serializeThresholds(flowCnec, gen);

        JsonUtil.writeExtensions(flowCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeIMax(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        serializeDoubleValuesOnBothSide(gen, flowCnec.getIMax(TwoSides.ONE), flowCnec.getIMax(TwoSides.TWO), I_MAX);
    }

    private void serializeNominalVoltage(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        serializeDoubleValuesOnBothSide(gen, flowCnec.getNominalVoltage(TwoSides.ONE), flowCnec.getNominalVoltage(TwoSides.TWO), NOMINAL_VOLTAGE);
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
            .toList();
        for (BranchThreshold threshold : sortedListOfThresholds) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }
}
