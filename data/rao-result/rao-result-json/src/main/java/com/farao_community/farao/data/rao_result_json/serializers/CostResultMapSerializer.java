/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Objects;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class CostResultMapSerializer {

    private CostResultMapSerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(COST_RESULTS);
        serializeCostResultForOptimizationState(null, raoResult, jsonGenerator);
        for (Instant instant : crac.getSortedInstants()) {
            serializeCostResultForOptimizationState(instant, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndObject();
    }

    private static void serializeCostResultForOptimizationState(Instant optInstant, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double functionalCost = raoResult.getFunctionalCost(optInstant);
        boolean isFunctionalCostNaN = Double.isNaN(functionalCost);

        if (isFunctionalCostNaN && Double.isNaN(raoResult.getVirtualCost(optInstant))) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeInstantId(optInstant));
        if (!isFunctionalCostNaN) {
            jsonGenerator.writeNumberField(FUNCTIONAL_COST, Math.round(100.0 * functionalCost) / 100.0);
        }

        if (containAnyVirtualCostForOptimizationState(raoResult, optInstant)) {
            jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
            for (String virtualCostName : raoResult.getVirtualCostNames()) {
                double virtualCostForAGivenName = raoResult.getVirtualCost(optInstant, virtualCostName);
                if (!Double.isNaN(virtualCostForAGivenName)) {
                    jsonGenerator.writeNumberField(virtualCostName, Math.round(100.0 * virtualCostForAGivenName) / 100.0);
                }
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containAnyVirtualCostForOptimizationState(RaoResult raoResult, Instant optInstant) {
        return !Objects.isNull(raoResult.getVirtualCostNames()) &&
            raoResult.getVirtualCostNames().stream().anyMatch(costName -> !Double.isNaN(raoResult.getVirtualCost(optInstant, costName)));
    }
}
