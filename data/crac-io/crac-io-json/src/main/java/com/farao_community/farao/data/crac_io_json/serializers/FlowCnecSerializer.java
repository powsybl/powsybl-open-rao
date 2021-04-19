/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.Optional;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecSerializer<I extends FlowCnec> extends AbstractJsonSerializer<I> {

    @Override
    public void serialize(I flowCnec, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStringField(ID, flowCnec.getId());
        gen.writeStringField(NAME, flowCnec.getName());
        gen.writeStringField(NETWORK_ELEMENT_ID, flowCnec.getNetworkElement().getId());
        gen.writeStringField(OPERATOR, flowCnec.getOperator());
        gen.writeStringField(INSTANT, serializeInstant(flowCnec.getState().getInstant()));
        Optional<Contingency> optContingency = flowCnec.getState().getContingency();
        if (optContingency.isPresent()) {
            gen.writeStringField(CONTINGENCY, optContingency.get().getId());
        }
        gen.writeObjectField(OPTIMIZED, flowCnec.isOptimized());
        gen.writeObjectField(MONITORED, flowCnec.isMonitored());

        gen.writeFieldName(THRESHOLDS);
        gen.writeStartArray();
        for (BranchThreshold threshold: flowCnec.getThresholds()) {
            gen.writeObject(threshold);
        }
        gen.writeEndArray();
    }

    public void addExtensions(I branchCnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        JsonUtil.writeExtensions(branchCnec, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }
}
