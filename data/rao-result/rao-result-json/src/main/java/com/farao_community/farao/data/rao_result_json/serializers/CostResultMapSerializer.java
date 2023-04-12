/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Objects;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.VIRTUAL_COSTS;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class CostResultMapSerializer {

    private CostResultMapSerializer() {
    }

    static void serialize(RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeObjectFieldStart(COST_RESULTS);
        serializeCostResultForOptimizationState(OptimizationState.INITIAL, raoResult, jsonGenerator);
        serializeCostResultForOptimizationState(OptimizationState.AFTER_PRA, raoResult, jsonGenerator);
        serializeCostResultForOptimizationState(OptimizationState.AFTER_ARA, raoResult, jsonGenerator);
        serializeCostResultForOptimizationState(OptimizationState.AFTER_CRA, raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void serializeCostResultForOptimizationState(OptimizationState optState, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        double functionalCost = raoResult.getFunctionalCost(optState);
        boolean isFunctionalCostNaN = Double.isNaN(functionalCost);

        if (isFunctionalCostNaN && Double.isNaN(raoResult.getVirtualCost(optState))) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
        if (!isFunctionalCostNaN) {
            jsonGenerator.writeNumberField(FUNCTIONAL_COST, Math.round(100.0 * functionalCost) / 100.0);
        }

        if (containAnyVirtualCostForOptimizationState(raoResult, optState)) {
            jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
            for (String virtualCostName : raoResult.getVirtualCostNames()) {
                double virtualCostForAGivenName = raoResult.getVirtualCost(optState, virtualCostName);
                if (!Double.isNaN(virtualCostForAGivenName) && !("sensitivity-failure-cost".equals(virtualCostName) && Math.abs(virtualCostForAGivenName) <= 10e-10)) {
                    jsonGenerator.writeNumberField(virtualCostName, Math.round(100.0 * virtualCostForAGivenName) / 100.0);
                }
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containAnyVirtualCostForOptimizationState(RaoResult raoResult, OptimizationState optState) {
        return !Objects.isNull(raoResult.getVirtualCostNames()) &&
                raoResult.getVirtualCostNames().stream().anyMatch(costName -> !Double.isNaN(raoResult.getVirtualCost(optState, costName)));
    }
}
