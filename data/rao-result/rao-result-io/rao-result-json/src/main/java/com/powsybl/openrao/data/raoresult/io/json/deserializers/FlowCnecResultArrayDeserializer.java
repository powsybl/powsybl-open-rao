/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.impl.ElementaryFlowCnecResult;
import com.powsybl.openrao.data.raoresult.impl.FlowCnecResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

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
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", FLOWCNEC_RESULTS, FLOWCNEC_ID));
            }

            String flowCnecId = jsonParser.nextTextValue();
            FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);

            if (flowCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: flowCnec with id %s does not exist in the Crac", flowCnecId));
            }
            FlowCnecResult flowCnecResult = raoResult.getAndCreateIfAbsentFlowCnecResult(flowCnec);
            deserializeFlowCnecResult(jsonParser, flowCnecResult, jsonFileVersion, crac);
        }
    }

    private static void deserializeFlowCnecResult(JsonParser jsonParser, FlowCnecResult flowCnecResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryFlowCnecResult eFlowCnecResult;
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            eFlowCnecResult = flowCnecResult.getAndCreateIfAbsentResultForOptimizationState(optimizedInstant);
            deserializeElementaryFlowCnecResult(jsonParser, eFlowCnecResult, jsonFileVersion);
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
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    eFlowCnecResult.setPtdfZonalSum(TwoSides.ONE, jsonParser.getDoubleValue());
                    eFlowCnecResult.setPtdfZonalSum(TwoSides.TWO, jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
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
                case SIDE_ONE:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, TwoSides.ONE);
                    break;
                case SIDE_TWO:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, TwoSides.TWO);
                    break;
                case LEFT_SIDE:
                    Utils.checkDeprecatedField(LEFT_SIDE, FLOWCNEC_RESULTS, jsonFileVersion, "1.4");
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, TwoSides.ONE);
                    break;
                case RIGHT_SIDE:
                    Utils.checkDeprecatedField(RIGHT_SIDE, FLOWCNEC_RESULTS, jsonFileVersion, "1.4");
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, eFlowCnecResult, unit, TwoSides.TWO);
                    break;
                case FLOW:
                    checkSideHandlingVersion(jsonFileVersion, FLOW);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    eFlowCnecResult.setFlow(TwoSides.ONE, jsonParser.getDoubleValue(), unit);
                    eFlowCnecResult.setFlow(TwoSides.TWO, jsonParser.getDoubleValue(), unit);
                    break;
                case COMMERCIAL_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, COMMERCIAL_FLOW);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    eFlowCnecResult.setCommercialFlow(TwoSides.ONE, jsonParser.getDoubleValue(), unit);
                    eFlowCnecResult.setCommercialFlow(TwoSides.TWO, jsonParser.getDoubleValue(), unit);
                    break;
                case LOOP_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, LOOP_FLOW);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    eFlowCnecResult.setLoopFlow(TwoSides.ONE, jsonParser.getDoubleValue(), unit);
                    eFlowCnecResult.setLoopFlow(TwoSides.TWO, jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void checkSideHandlingVersion(String jsonFileVersion, String fieldName) {
        Utils.checkDeprecatedField(fieldName, FLOWCNEC_RESULTS, jsonFileVersion, "1.1");
    }

    private static void deserializeElementaryFlowCnecResultForUnitAndSide(JsonParser jsonParser, ElementaryFlowCnecResult eFlowCnecResult, Unit unit, TwoSides side) throws IOException {
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
                    if (!unit.equals(Unit.MEGAWATT)) {
                        throw new OpenRaoException(String.format("%s can only be defined in the MEGAWATT section", ZONAL_PTDF_SUM));
                    }
                    eFlowCnecResult.setPtdfZonalSum(side, jsonParser.getDoubleValue());
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
