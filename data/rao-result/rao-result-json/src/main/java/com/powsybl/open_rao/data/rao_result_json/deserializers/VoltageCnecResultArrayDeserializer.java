/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.rao_result_json.deserializers;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.commons.Unit;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.rao_result_impl.VoltageCnecResult;
import com.powsybl.open_rao.data.rao_result_impl.ElementaryVoltageCnecResult;
import com.powsybl.open_rao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.open_rao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class VoltageCnecResultArrayDeserializer {

    private VoltageCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(VOLTAGECNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", VOLTAGECNEC_RESULTS, VOLTAGECNEC_ID));
            }

            String voltageCnecId = jsonParser.nextTextValue();
            VoltageCnec voltageCnec = crac.getVoltageCnec(voltageCnecId);

            if (voltageCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: voltageCnec with id %s does not exist in the Crac", voltageCnecId));
            }
            VoltageCnecResult voltageCnecResult = raoResult.getAndCreateIfAbsentVoltageCnecResult(voltageCnec);
            deserializeVoltageCnecResult(jsonParser, voltageCnecResult, jsonFileVersion, crac);
        }
    }

    private static void deserializeVoltageCnecResult(JsonParser jsonParser, VoltageCnecResult voltageCnecResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryVoltageCnecResult eVoltageCnecResult;
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            eVoltageCnecResult = voltageCnecResult.getAndCreateIfAbsentResultForOptimizationState(optimizedInstant);
            deserializeElementaryVoltageCnecResult(jsonParser, eVoltageCnecResult);
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
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
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
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
