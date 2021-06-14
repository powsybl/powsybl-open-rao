/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_impl.ElementaryFlowCnecResult;
import com.farao_community.farao.data.rao_result_impl.FlowCnecResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class FlowCnecResultArrayDeserializer {

    private FlowCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(FLOWCNEC_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: Each %s must start with an %s field", FLOWCNEC_RESULTS, FLOWCNEC_ID));
            }

            String flowCnecId = jsonParser.nextTextValue();
            FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);

            if (flowCnec == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: flowCnec with id %s does not exist in the Crac", flowCnecId));
            }
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(flowCnec);
            deserializeFlowCnecResult(jsonParser, flowCnecResult);
        }
    }

    private static void deserializeFlowCnecResult(JsonParser jsonParser, FlowCnecResult flowCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryFlowCnecResult eFlowCnecResult;
            switch (jsonParser.getCurrentName()) {
                case INITIAL_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult);
                    break;
                case AFTER_PRA_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_PRA);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult);
                    break;
                case AFTER_CRA_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_CRA);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult);
                    break;
                default:
                    throw new FaraoException(String.format("Unexpected field in flowCnecResult (%s), an optimization state is expected", jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryFlowCnecResult(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MEGAWATT_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, eFlowCnecResult, Unit.MEGAWATT);
                    break;
                case AMPERE_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, eFlowCnecResult, Unit.AMPERE);
                    break;
                case ZONAL_PTDF_SUM:
                    jsonParser.nextToken();
                    eFlowCnecResult.setPtdfZonalSum(jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException("Unexpected field in flowCnecResult (%s)");
            }
        }
    }

    private static void deserializeElementaryFlowCnecResultForUnit(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult, Unit unit) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setFlow(jsonParser.getDoubleValue(), unit);
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    eFlowCnecResult.setMargin(jsonParser.getDoubleValue(), unit);
                    break;
                case RELATIVE_MARGIN:
                    jsonParser.nextToken();
                    eFlowCnecResult.setRelativeMargin(jsonParser.getDoubleValue(), unit);
                    break;
                case COMMERCIAL_FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setCommercialFlow(jsonParser.getDoubleValue(), unit);
                    break;
                case LOOP_FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setLoopFlow(jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new FaraoException(String.format("Unexpected field in flowCnecResult (%s)", jsonParser.getCurrentName()));
            }
        }
    }
}