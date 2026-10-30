/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.commons.UnitConverter.getFlowUnitMultiplier;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecSerializer<I extends FlowCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I flowCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.ID, flowCnec.getId());
        gen.writeStringField(JsonSerializationConstants.NAME, flowCnec.getName());
        gen.writeStringField(JsonSerializationConstants.NETWORK_ELEMENT_ID, flowCnec.getNetworkElementId());
        gen.writeStringField(JsonSerializationConstants.OPERATOR, flowCnec.getOperator());
        gen.writeStringField(JsonSerializationConstants.BORDER, flowCnec.getBorder());
        gen.writeStringField(JsonSerializationConstants.INSTANT, flowCnec.getState().getInstant().getId());
        Optional<Contingency> optContingency = flowCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(JsonSerializationConstants.CONTINGENCY_ID, optContingency.get().getId());
        }
        gen.writeObjectField(JsonSerializationConstants.OPTIMIZED, flowCnec.isOptimized());
        gen.writeObjectField(JsonSerializationConstants.MONITORED, flowCnec.isMonitored());
        gen.writeNumberField(JsonSerializationConstants.RELIABILITY_MARGIN, flowCnec.getReliabilityMargin());

        serializeThresholds(flowCnec, gen);

        JsonUtil.writeExtensions(flowCnec, gen, serializerProvider, ExtensionsHandler.getExtensionsSerializers());

        gen.writeEndObject();
    }

    private void serializeThresholds(FlowCnec flowCnec, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.THRESHOLDS);
        for (TwoSides side : TwoSides.values()) {
            for (Unit unit : List.of(Unit.MEGAWATT, Unit.AMPERE)) {

                if (flowCnec.getLowerBound(side, unit).isPresent() || flowCnec.getUpperBound(side, unit).isPresent()) {
                    gen.writeStartObject();
                    gen.writeStringField(JsonSerializationConstants.UNIT, JsonSerializationConstants.serializeUnit(unit));
                    gen.writeNumberField(JsonSerializationConstants.SIDE, JsonSerializationConstants.serializeSide(side));
                    if (flowCnec.getLowerBound(side, unit).isPresent()) {
                        gen.writeNumberField(JsonSerializationConstants.MIN,
                            flowCnec.getLowerBound(side, unit).get()
                                - flowCnec.getReliabilityMargin() * getFlowUnitMultiplier(flowCnec.getNominalVoltage(side), Unit.MEGAWATT, unit));
                    }
                    if (flowCnec.getUpperBound(side, unit).isPresent()) {
                        gen.writeNumberField(JsonSerializationConstants.MAX,
                            flowCnec.getUpperBound(side, unit).get()
                                + flowCnec.getReliabilityMargin() * getFlowUnitMultiplier(flowCnec.getNominalVoltage(side), Unit.MEGAWATT, unit));
                    }
                    gen.writeEndObject();
                }
            }
        }
        gen.writeEndArray();
    }
}
