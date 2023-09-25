/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.rao_result_impl.CostResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class CostResultMapDeserializer {

    private CostResultMapDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case INITIAL_OPT_STATE:
                    jsonParser.nextToken();
                    deserializeCostResult(jsonParser, raoResult, null);
                    break;
                case AFTER_PRA_OPT_STATE:
                    jsonParser.nextToken();
                    deserializeCostResult(jsonParser, raoResult, crac.getInstant(Instant.Kind.PREVENTIVE));
                    break;
                case AFTER_ARA_OPT_STATE:
                    jsonParser.nextToken();
                    deserializeCostResult(jsonParser, raoResult, crac.getInstant(Instant.Kind.AUTO));
                    break;
                case AFTER_CRA_OPT_STATE:
                    jsonParser.nextToken();
                    deserializeCostResult(jsonParser, raoResult, crac.getInstant(Instant.Kind.CURATIVE));
                    break;
                default:
                    if (jsonParser.getCurrentName().contains("after ")) {
                        // TODO : improve this
                        Instant instant = crac.getInstant(jsonParser.getCurrentName().replace("after ", "").replace("\"", ""));
                        jsonParser.nextToken();
                        deserializeCostResult(jsonParser, raoResult, instant);
                    } else {
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s), an optimization state is expected", COST_RESULTS, jsonParser.getCurrentName()));
                    }
            }
        }
    }

    private static void deserializeCostResult(JsonParser jsonParser, RaoResultImpl raoResult, Instant optInstant) throws IOException {

        CostResult costResult = raoResult.getAndCreateIfAbsentCostResult(optInstant);

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
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", COST_RESULTS, jsonParser.getCurrentName()));
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
