/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.impl.CostResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.COST_RESULTS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.FUNCTIONAL_COST;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.VIRTUAL_COSTS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.deserializeOptimizedInstantId;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class CostResultMapDeserializer {

    private CostResultMapDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            String optimizedInstantId = deserializeOptimizedInstantId(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            deserializeCostResult(jsonParser, raoResult, optimizedInstantId);
        }
    }

    private static void deserializeCostResult(JsonParser jsonParser, RaoResultImpl raoResult, String optInstantId) throws IOException {

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(optInstantId);

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {

                case FUNCTIONAL_COST:
                    jsonParser.nextToken();
                    costResult.setFunctionalCost(jsonParser.getDoubleValue());
                    break;

                case VIRTUAL_COSTS:
                    jsonParser.nextToken();
                    deserializeVirtualCosts(jsonParser, costResult);
                    break;

                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", COST_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeVirtualCosts(JsonParser jsonParser, CostResult costResult) throws IOException {

        while (!jsonParser.nextToken().isStructEnd()) {
            String costName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            double costValue = jsonParser.getDoubleValue();
            costResult.setVirtualCost(costName, costValue);
        }
    }
}
