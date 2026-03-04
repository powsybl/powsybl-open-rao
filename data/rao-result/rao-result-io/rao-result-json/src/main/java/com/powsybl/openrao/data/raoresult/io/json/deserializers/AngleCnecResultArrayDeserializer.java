/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleExtension;
import com.powsybl.openrao.data.raoresult.impl.ElementaryAngleCnecResult;
import com.powsybl.openrao.data.raoresult.impl.AngleCnecResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class AngleCnecResultArrayDeserializer {

    private AngleCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {
        AngleExtension angleExtension = new AngleExtension();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(ANGLECNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", ANGLECNEC_RESULTS, ANGLECNEC_ID));
            }

            String angleCnecId = jsonParser.nextTextValue();
            AngleCnec angleCnec = crac.getAngleCnec(angleCnecId);

            if (angleCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: angleCnec with id %s does not exist in the Crac", angleCnecId));
            }
            AngleCnecResult angleCnecResult = raoResult.getAndCreateIfAbsentAngleCnecResult(angleCnec);
            deserializeAngleCnecResult(jsonParser, angleCnec, angleCnecResult, angleExtension, jsonFileVersion, crac);
        }

        raoResult.addExtension(AngleExtension.class, angleExtension);
        angleExtension.setExtendable(raoResult);
    }

    private static void deserializeAngleCnecResult(JsonParser jsonParser, AngleCnec angleCnec, AngleCnecResult angleCnecResult, AngleExtension angleExtension, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            ElementaryAngleCnecResult eAngleCnecResult;
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.getCurrentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            eAngleCnecResult = angleCnecResult.getAndCreateIfAbsentResultForOptimizationState(optimizedInstant);
            deserializeElementaryAngleCnecResult(jsonParser, eAngleCnecResult);
            angleExtension.addAngle(eAngleCnecResult.getAngle(Unit.DEGREE), optimizedInstant, angleCnec, Unit.DEGREE);
        }
    }

    private static void deserializeElementaryAngleCnecResult(JsonParser jsonParser, ElementaryAngleCnecResult eAngleCnecResult) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (!jsonParser.getCurrentName().equals(DEGREE_UNIT)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.getCurrentName()));
            } else {
                jsonParser.nextToken();
                deserializeElementaryAngleCnecResultForUnit(jsonParser, eAngleCnecResult, Unit.DEGREE);
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
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", ANGLECNEC_RESULTS, jsonParser.getCurrentName()));
            }
        }
    }
}
