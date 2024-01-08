/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.data.rao_result_json.serializers;

import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.OptimizationStepsExecuted;
import com.powsybl.open_rao.data.rao_result_api.RaoResult;
import com.powsybl.open_rao.search_tree_rao.result.impl.FailedRaoResultImpl;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Set;

import static com.powsybl.open_rao.data.rao_result_json.RaoResultJsonConstants.*;

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

        if (!(raoResult instanceof FailedRaoResultImpl)) {
            OptimizationStepsExecuted optimizationStepsExecuted = raoResult.getOptimizationStepsExecuted();
            jsonGenerator.writeStringField(OPTIMIZATION_STEPS_EXECUTED, serializeOptimizedStepsExecuted(optimizationStepsExecuted));
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
