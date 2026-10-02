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
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.AMPERE_UNIT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.COMMERCIAL_FLOW;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.FLOW;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.FLOWCNEC_ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.FLOWCNEC_RESULTS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.LEFT_SIDE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.LOOP_FLOW;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MARGIN;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MEGAWATT_UNIT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.RELATIVE_MARGIN;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.RIGHT_SIDE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.SIDE_ONE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.SIDE_TWO;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ZONAL_PTDF_SUM;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.deserializeOptimizedInstant;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class FlowCnecResultArrayDeserializer {

    public static final String UNEXPECTED_FIELD = "Cannot deserialize RaoResult: unexpected field in %s (%s)";

    private FlowCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {
        FlowResult flowResult = new FlowResult();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(FLOWCNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", FLOWCNEC_RESULTS, FLOWCNEC_ID));
            }

            String flowCnecId = jsonParser.nextTextValue();
            FlowCnec flowCnec = crac.getFlowCnec(flowCnecId);

            if (flowCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: flowCnec with id %s does not exist in the Crac", flowCnecId));
            }
            deserializeFlowCnecResult(jsonParser, flowResult, flowCnec, jsonFileVersion, crac);
        }

        raoResult.addExtension(FlowResult.class, flowResult);
    }

    private static void deserializeFlowCnecResult(JsonParser jsonParser, FlowResult flowResult, FlowCnec flowCnec, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.currentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            deserializeElementaryFlowCnecResult(jsonParser, flowResult, flowCnec, optimizedInstant, jsonFileVersion);
        }
    }

    private static void deserializeElementaryFlowCnecResult(JsonParser jsonParser, FlowResult flowResult, FlowCnec flowCnec, Instant optimizedInstant, String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case MEGAWATT_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, flowResult, flowCnec, Unit.MEGAWATT, optimizedInstant, jsonFileVersion);
                    break;
                case AMPERE_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnit(jsonParser, flowResult, flowCnec, Unit.AMPERE, optimizedInstant, jsonFileVersion);
                    break;
                case ZONAL_PTDF_SUM:
                    checkSideHandlingVersion(jsonFileVersion, ZONAL_PTDF_SUM);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    flowResult.addPtdfZonalSumMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.ONE);
                    flowResult.addPtdfZonalSumMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.TWO);
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.currentName()));
            }
        }
    }

    private static void deserializeElementaryFlowCnecResultForUnit(JsonParser jsonParser,
                                                                   FlowResult flowResult,
                                                                   FlowCnec flowCnec,
                                                                   Unit unit,
                                                                   Instant optimizedInstant,
                                                                   String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case MARGIN, RELATIVE_MARGIN:
                    jsonParser.nextToken();
                    break;
                case SIDE_ONE:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, flowResult, flowCnec, unit, TwoSides.ONE, optimizedInstant);
                    break;
                case SIDE_TWO:
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, flowResult, flowCnec, unit, TwoSides.TWO, optimizedInstant);
                    break;
                case LEFT_SIDE:
                    Utils.checkDeprecatedField(LEFT_SIDE, FLOWCNEC_RESULTS, jsonFileVersion, "1.4");
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, flowResult, flowCnec, unit, TwoSides.ONE, optimizedInstant);
                    break;
                case RIGHT_SIDE:
                    Utils.checkDeprecatedField(RIGHT_SIDE, FLOWCNEC_RESULTS, jsonFileVersion, "1.4");
                    jsonParser.nextToken();
                    deserializeElementaryFlowCnecResultForUnitAndSide(jsonParser, flowResult, flowCnec, unit, TwoSides.TWO, optimizedInstant);
                    break;
                case FLOW:
                    checkSideHandlingVersion(jsonFileVersion, FLOW);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    flowResult.addFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.ONE, unit);
                    flowResult.addFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.TWO, unit);
                    break;
                case COMMERCIAL_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, COMMERCIAL_FLOW);
                    // For older versions, suppose both sides are used
                    jsonParser.nextToken();
                    flowResult.addCommercialFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.ONE, unit);
                    flowResult.addCommercialFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, TwoSides.TWO, unit);
                    break;
                case LOOP_FLOW:
                    checkSideHandlingVersion(jsonFileVersion, LOOP_FLOW);
                    jsonParser.nextToken();
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.currentName()));
            }
        }
    }

    private static void checkSideHandlingVersion(String jsonFileVersion, String fieldName) {
        Utils.checkDeprecatedField(fieldName, FLOWCNEC_RESULTS, jsonFileVersion, "1.1");
    }

    private static void deserializeElementaryFlowCnecResultForUnitAndSide(JsonParser jsonParser,
                                                                          FlowResult flowResult,
                                                                          FlowCnec flowCnec,
                                                                          Unit unit,
                                                                          TwoSides side,
                                                                          Instant optimizedInstant) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.currentName()) {
                case FLOW:
                    jsonParser.nextToken();
                    flowResult.addFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, side, unit);
                    break;
                case COMMERCIAL_FLOW:
                    jsonParser.nextToken();
                    flowResult.addCommercialFlowMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, side, unit);
                    break;
                case LOOP_FLOW:
                    jsonParser.nextToken();
                    break;
                case ZONAL_PTDF_SUM:
                    jsonParser.nextToken();
                    if (!unit.equals(Unit.MEGAWATT)) {
                        throw new OpenRaoException(String.format("%s can only be defined in the MEGAWATT section", ZONAL_PTDF_SUM));
                    }
                    flowResult.addPtdfZonalSumMeasurement(jsonParser.getDoubleValue(), optimizedInstant, flowCnec, side);
                    break;
                default:
                    throw new OpenRaoException(String.format(UNEXPECTED_FIELD, FLOWCNEC_RESULTS, jsonParser.currentName()));
            }
        }
    }
}
