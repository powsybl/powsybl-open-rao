/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Set;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class RaoResultSerializer extends AbstractJsonSerializer<RaoResult> {

    private final Crac crac;
    private final Set<Unit> flowUnits;

    RaoResultSerializer(Crac crac, Set<Unit> flowUnits) {
        this.crac = crac;
        this.flowUnits = flowUnits;
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
        jsonGenerator.writeStringField(EXECUTION_DETAILS, raoResult.getExecutionDetails());

        if (raoResult.getComputationStatus() != ComputationStatus.FAILURE) {
            CostResultMapSerializer.serialize(raoResult, crac, jsonGenerator);
            ComputationStatusMapSerializer.serialize(raoResult, crac, jsonGenerator);
            FlowCnecResultArraySerializer.serialize(raoResult, crac, flowUnits, jsonGenerator);
            AngleCnecResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
            VoltageCnecResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
            NetworkActionResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
            RangeActionResultArraySerializer.serialize(raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }
}
