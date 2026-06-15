/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ANGLE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ANGLECNEC_ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.ANGLECNEC_RESULTS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.DEGREE_UNIT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MARGIN;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.deserializeOptimizedInstant;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class AngleCnecResultArrayDeserializer {

    private AngleCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {
        AngleResult angleResult = new AngleResult();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(ANGLECNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", ANGLECNEC_RESULTS, ANGLECNEC_ID));
            }

            String angleCnecId = jsonParser.nextTextValue();
            AngleCnec angleCnec = crac.getAngleCnec(angleCnecId);

            if (angleCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: angleCnec with id %s does not exist in the Crac", angleCnecId));
            }
            deserializeAngleCnecResult(jsonParser, angleCnec, angleResult, jsonFileVersion, crac);
        }

        raoResult.addExtension(AngleResult.class, angleResult);
    }

    private static void deserializeAngleCnecResult(JsonParser jsonParser, AngleCnec angleCnec, AngleResult angleResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.currentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            deserializeElementaryAngleCnecResult(jsonParser, angleCnec, angleResult, optimizedInstant);
        }
    }

    private static void deserializeElementaryAngleCnecResult(JsonParser jsonParser, AngleCnec angleCnec, AngleResult angleResult, Instant optimizedInstant) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (!jsonParser.currentName().equals(DEGREE_UNIT)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.currentName()));
            } else {
                jsonParser.nextToken();
                deserializeElementaryAngleCnecResultForUnit(jsonParser, angleCnec, angleResult, optimizedInstant);
            }
        }
    }

    private static void deserializeElementaryAngleCnecResultForUnit(JsonParser jsonParser, AngleCnec angleCnec, AngleResult angleResult, Instant optimizedInstant) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            double angle = Double.NaN;
            switch (jsonParser.currentName()) {
                case ANGLE:
                    jsonParser.nextToken();
                    angle = jsonParser.getDoubleValue();
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.currentName()));
            }
            if (!Double.isNaN(angle)) {
                angleResult.addMeasurement(angle, optimizedInstant, angleCnec, Unit.DEGREE);
            }
        }
    }
}
