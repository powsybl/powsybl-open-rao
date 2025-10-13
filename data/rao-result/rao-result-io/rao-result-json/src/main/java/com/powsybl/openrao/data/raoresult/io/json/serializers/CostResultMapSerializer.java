/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;

import java.io.IOException;
import java.util.Objects;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

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
        double virtualCost = raoResult.getVirtualCost(optInstant);
        boolean isVirtualCostNaN = Double.isNaN(virtualCost);

        if (isFunctionalCostNaN && isVirtualCostNaN) {
            return;
        }
        double margin;
        if (isFunctionalCostNaN) {
            margin = -virtualCost;
        } else if (isVirtualCostNaN) {
            margin = -functionalCost;
        } else {
            margin = -functionalCost - virtualCost;
        }

        jsonGenerator.writeObjectFieldStart(serializeInstantId(optInstant));
        if (!isFunctionalCostNaN) {
            jsonGenerator.writeNumberField(FUNCTIONAL_COST, roundValueBasedOnMargin(functionalCost, margin, 2));
        }

        if (containAnyVirtualCostForOptimizationState(raoResult, optInstant)) {
            jsonGenerator.writeObjectFieldStart(VIRTUAL_COSTS);
            for (String virtualCostName : raoResult.getVirtualCostNames()) {
                double virtualCostForAGivenName = raoResult.getVirtualCost(optInstant, virtualCostName);
                if (!Double.isNaN(virtualCostForAGivenName)) {
                    jsonGenerator.writeNumberField(virtualCostName, roundValueBasedOnMargin(virtualCostForAGivenName, margin, 2));
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
