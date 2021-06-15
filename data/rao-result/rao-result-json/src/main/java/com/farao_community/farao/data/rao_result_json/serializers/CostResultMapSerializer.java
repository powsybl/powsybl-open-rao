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
        serializeCostResultForOptimizationState(OptimizationState.AFTER_CRA, raoResult, jsonGenerator);
        jsonGenerator.writeEndObject();
    }

    private static void serializeCostResultForOptimizationState(OptimizationState optState, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (!containAnyResultPresentForOptimizationState(raoResult, optState)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
        if (!Double.isNaN(raoResult.getFunctionalCost(optState))) {
            jsonGenerator.writeNumberField(FUNCTIONAL_COST, raoResult.getFunctionalCost(optState));
        }

        if (containAnyVirtualCostForOptimizationState(raoResult, optState)) {
            jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
            for (String virtualCostName : raoResult.getVirtualCostNames()) {
                if (!Double.isNaN(raoResult.getVirtualCost(optState, virtualCostName))) {
                    jsonGenerator.writeNumberField(virtualCostName, raoResult.getVirtualCost(optState, virtualCostName));
                }
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containAnyResultPresentForOptimizationState(RaoResult raoResult, OptimizationState optState) {

        if (!Double.isNaN(raoResult.getFunctionalCost(optState))) {
            return true;
        }
        if (!Double.isNaN(raoResult.getVirtualCost(optState))) {
            return true;
        }
        return false;
    }

    private static boolean containAnyVirtualCostForOptimizationState(RaoResult raoResult, OptimizationState optState) {
        return !Objects.isNull(raoResult.getVirtualCostNames()) &&
            raoResult.getVirtualCostNames().stream().anyMatch(costName -> !Double.isNaN(raoResult.getVirtualCost(optState, costName)));
    }
}
