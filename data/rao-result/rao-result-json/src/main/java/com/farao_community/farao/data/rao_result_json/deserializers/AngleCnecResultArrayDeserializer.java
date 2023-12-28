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
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.rao_result_impl.ElementaryAngleCnecResult;
import com.farao_community.farao.data.rao_result_impl.AngleCnecResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class AngleCnecResultArrayDeserializer {

    private AngleCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(ANGLECNEC_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", ANGLECNEC_RESULTS, ANGLECNEC_ID));
            }

            String angleCnecId = jsonParser.nextTextValue();
            AngleCnec angleCnec = crac.getAngleCnec(angleCnecId);

            if (angleCnec == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: angleCnec with id %s does not exist in the Crac", angleCnecId));
            }
            AngleCnecResult angleCnecResult = raoResult.getAndCreateIfAbsentAngleCnecResult(angleCnec);
            deserializeAngleCnecResult(jsonParser, angleCnecResult, jsonFileVersion, crac);
        }
    }

    private static void deserializeAngleCnecResult(JsonParser jsonParser, AngleCnecResult angleCnecResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryAngleCnecResult eAngleCnecResult;
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            eAngleCnecResult = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(optimizedInstant);
            deserializeElementaryAngleCnecResult(jsonParser, eAngleCnecResult);
        }
    }

    private static void deserializeElementaryAngleCnecResult(JsonParser jsonParser, ElementaryAngleCnecResult eAngleCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case DEGREE_UNIT:
                    jsonParser.nextToken();
                    deserializeElementaryAngleCnecResultForUnit(jsonParser, eAngleCnecResult, Unit.DEGREE);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }

    private static void deserializeElementaryAngleCnecResultForUnit(JsonParser jsonParser, ElementaryAngleCnecResult eAngleCnecResult, Unit unit) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            switch (jsonParser.getCurrentName()) {
                case ANGLE:
                    jsonParser.nextToken();
                    eAngleCnecResult.setAngle(jsonParser.getDoubleValue(), unit);
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    eAngleCnecResult.setMargin(jsonParser.getDoubleValue(), unit);
                    break;
                default:
                    throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
