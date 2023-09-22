/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_impl.VoltageCnecResult;
import com.farao_community.farao.data.rao_result_impl.ElementaryVoltageCnecResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class VoltageCnecResultArrayDeserializer {

    private VoltageCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(VOLTAGECNEC_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", VOLTAGECNEC_RESULTS, VOLTAGECNEC_ID));
            }

            String voltageCnecId = jsonParser.nextTextValue();
            VoltageCnec voltageCnec = crac.getVoltageCnec(voltageCnecId);

            if (voltageCnec == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: voltageCnec with id %s does not exist in the Crac", voltageCnecId));
            }
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(voltageCnec);
            deserializeVoltageCnecResult(jsonParser, voltageCnecResult, crac);
        }
    }

    private static void deserializeVoltageCnecResult(JsonParser jsonParser, VoltageCnecResult voltageCnecResult, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryVoltageCnecResult eVoltageCnecResult;
            switch (jsonParser.getCurrentName()) {
                case INITIAL_OPT_STATE:
                    jsonParser.nextToken();
                    eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.initial(crac));
                    deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
                    break;
                case AFTER_PRA_OPT_STATE:
                    jsonParser.nextToken();
                    eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.afterPra(crac));
                    deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
                    break;
                case AFTER_ARA_OPT_STATE:
                    jsonParser.nextToken();
                    eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.afterAra(crac));
                    deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
                    break;
                case AFTER_CRA_OPT_STATE:
                    jsonParser.nextToken();
                    eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(OptimizationState.afterCra(crac));
                    deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s), an optimization state is expected", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryVoltageCnecResult(JsonParser jsonParser, ElementaryVoltageCnecResult eVoltageCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case KILOVOLT_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryVoltageCnecResultForUnit(jsonParser, eVoltageCnecResult, Unit.KILOVOLT);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryVoltageCnecResultForUnit(JsonParser jsonParser, ElementaryVoltageCnecResult eVoltageCnecResult, Unit unit) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case VOLTAGE:
                    jsonParser.nextToken();
                    eVoltageCnecResult.setVoltage(jsonParser.getDoubleValue(), unit);
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    eVoltageCnecResult.setMargin(jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
