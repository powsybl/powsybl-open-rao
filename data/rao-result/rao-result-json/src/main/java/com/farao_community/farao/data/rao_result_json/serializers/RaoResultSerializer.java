/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class RaoResultSerializer extends AbstractJsonSerializer<RaoResult> {

    private Crac crac;

    RaoResultSerializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public void serialize(RaoResult raoResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        // type and version
        jsonGenerator.writeStringField(TYPE, RAO_RESULT_TYPE);
        jsonGenerator.writeStringField(VERSION, RAO_RESULT_IO_VERSION);
        jsonGenerator.writeStringField(INFO, RAO_RESULT_INFO);

        // computation status
        ComputationStatus computationStatus = raoResult.getComputationStatus();
        jsonGenerator.writeStringField(COMPUTATION_STATUS, serializeStatus(computationStatus));

        if (computationStatus == ComputationStatus.FAILURE) {
            jsonGenerator.writeEndObject();
            return;
        }

        CostResultMapSerializer.serialize(raoResult, jsonGenerator);
        FlowCnecResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
        NetworkActionResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
        PstRangeActionResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
        StandardRangeActionResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
        jsonGenerator.writeEndObject();
    }
}
