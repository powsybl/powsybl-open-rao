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
import com.farao_community.farao.data.crac_api.cnec.Side;
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

    public static final String UNEXPECTED_FIELD = "Cannot deserialize RaoResult: unexpected field in %s (%s)";

    private FlowCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(FLOWCNEC_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", FLOWCNEC_RESULTS, FLOWCNEC_ID));
            }

            String flowCnecId = jsonParser.nextTextValue();
            FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);

            if (flowCnec == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: flowCnec with id %s does not exist in the Crac", flowCnecId));
            }
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(flowCnec);
            deserializeFlowCnecResult(jsonParser, flowCnecResult, jsonFileVersion);
        }
    }

    private static void deserializeFlowCnecResult(JsonParser jsonParser, FlowCnecResult flowCnecResult, String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryFlowCnecResult eFlowCnecResult;
            switch (jsonParser.getCurrentName()) {
                case INITIAL_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.INITIAL);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult, jsonFileVersion);
                    break;
                case AFTER_PRA_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_PRA);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult, jsonFileVersion);
                    break;
                case AFTER_ARA_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_ARA);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult, jsonFileVersion);
                    break;
                case AFTER_CRA_OPT_STATE:
                    jsonParser.nextToken();
                    eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.AFTER_CRA);
                    deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult, jsonFileVersion);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s), an optimization state is expected", FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryFlowCnecResult(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult, String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MEGAWATT_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, eFlowCnecResult, Unit.MEGAWATT, jsonFileVersion);
                    break;
                case AMPERE_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, eFlowCnecResult, Unit.AMPERE, jsonFileVersion);
                    break;
                case ZONAL_PTDF_SUM:
                    checkSideHandlingVersion(jsonFileVersion, ZONAL_PTDF_SUM);
                    // For older versions, suppose side LEFT is used
                    jsonParser.nextToken();
                    eFlowCnecResult.setPtdfZonalSum(Side.LEFT, jsonParser.getDoubleValue());
                    break;
                default:
                    throw new FaraoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryFlowCnecResultForUnit(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult, Unit unit, String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case MARGIN:
                    jsonParser.nextToken();
                    eFlowCnecResult.setMargin(jsonParser.getDoubleValue(), unit);
                    break;
                case RELATIVE_MARGIN:
                    jsonParser.nextToken();
                    eFlowCnecResult.setRelativeMargin(jsonParser.getDoubleValue(), unit);
                    break;
                case LEFT_SIDE:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, Side.LEFT);
                    break;
                case RIGHT_SIDE:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, Side.RIGHT);
                    break;
                case FLOW:
                    checkSideHandlingVersion(jsonFileVersion, FLOW);
                    // For older versions, suppose side LEFT is used
                    jsonParser.nextToken();
                    eFlowCnecResult.setFlow(Side.LEFT, jsonParser.getDoubleValue(), unit);
                    break;
                case COMMERCIAL_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, COMMERCIAL_FLOW);
                    // For older versions, suppose side LEFT is used
                    jsonParser.nextToken();
                    eFlowCnecResult.setCommercialFlow(Side.LEFT, jsonParser.getDoubleValue(), unit);
                    break;
                case LOOP_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, LOOP_FLOW);
                    // For older versions, suppose side LEFT is used
                    jsonParser.nextToken();
                    eFlowCnecResult.setLoopFlow(Side.LEFT, jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new FaraoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void checkSideHandlingVersion(String jsonFileVersion, String fieldName) {
        if (getPrimaryVersionNumber(jsonFileVersion) > 1 || getSubVersionNumber(jsonFileVersion) >= 2) {
            throw new FaraoException(String.format("Cannot deserialize RaoResult: field %s should be defined per FlowCnec side as of version 1.2", fieldName));
        }
    }

    private static void deserializeElementaryFlowCnecResultForUnitAndSide(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult, Unit unit, Side side) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setFlow(side, jsonParser.getDoubleValue(), unit);
                    break;
                case COMMERCIAL_FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setCommercialFlow(side, jsonParser.getDoubleValue(), unit);
                    break;
                case LOOP_FLOW:
                    jsonParser.nextToken();
                    eFlowCnecResult.setLoopFlow(side, jsonParser.getDoubleValue(), unit);
                    break;
                case ZONAL_PTDF_SUM:
                    jsonParser.nextToken();
                    eFlowCnecResult.setPtdfZonalSum(side, jsonParser.getDoubleValue()); //  TODO : accept this only for MEGAWATT ?
                    break;
                default:
                    throw new FaraoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
