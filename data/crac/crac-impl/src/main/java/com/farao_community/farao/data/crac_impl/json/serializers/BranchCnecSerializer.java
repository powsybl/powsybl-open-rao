/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.serializers;

import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;
import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.THRESHOLDS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class BranchCnecSerializer<I extends BranchCnec> extends JsonSerializer<I> {

    @Override
    public void serialize(I branchCnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStringField(ID, branchCnec.getId());
        jsonGenerator.writeStringField(NAME, branchCnec.getName());
        jsonGenerator.writeStringField(NETWORK_ELEMENT, branchCnec.getNetworkElement().getId());
        jsonGenerator.writeStringField(OPERATOR, branchCnec.getOperator());
        jsonGenerator.writeObjectField(STATE, branchCnec.getState().getId());
        jsonGenerator.writeObjectField(OPTIMIZED, branchCnec.isOptimized());
        jsonGenerator.writeObjectField(MONITORED, branchCnec.isMonitored());

        jsonGenerator.writeFieldName(THRESHOLDS);
        jsonGenerator.writeStartArray();
        for (BranchThreshold threshold: branchCnec.getThresholds()) {
            jsonGenerator.writeObject(threshold);
        }
        jsonGenerator.writeEndArray();
    }

    public void addExtensions(I branchCnec, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        JsonUtil.writeExtensions(branchCnec, jsonGenerator, serializerProvider, ExtensionsHandler.getExtensionsSerializers());
    }
}
