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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.KILOVOLT_UNIT;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MARGIN;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MAX_VOLTAGE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.MIN_VOLTAGE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.VOLTAGE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.VOLTAGECNEC_ID;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.VOLTAGECNEC_RESULTS;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.deserializeOptimizedInstant;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.getPrimaryVersionNumber;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.getSubVersionNumber;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class VoltageCnecResultArrayDeserializer {

    private VoltageCnecResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {
        VoltageResult voltageResult = new VoltageResult();

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(VOLTAGECNEC_ID)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", VOLTAGECNEC_RESULTS, VOLTAGECNEC_ID));
            }

            String voltageCnecId = jsonParser.nextTextValue();
            VoltageCnec voltageCnec = crac.getVoltageCnec(voltageCnecId);

            if (voltageCnec == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: voltageCnec with id %s does not exist in the Crac", voltageCnecId));
            }
            deserializeVoltageCnecResult(jsonParser, voltageCnec, voltageResult, jsonFileVersion, crac);
        }

        raoResult.addExtension(VoltageResult.class, voltageResult);
    }

    private static void deserializeVoltageCnecResult(JsonParser jsonParser, VoltageCnec voltageCnec, VoltageResult voltageResult, String jsonFileVersion, Crac crac) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            Instant optimizedInstant = deserializeOptimizedInstant(jsonParser.currentName(), jsonFileVersion, crac);
            jsonParser.nextToken();
            deserializeElementaryVoltageCnecResult(jsonParser, voltageCnec, voltageResult, optimizedInstant, jsonFileVersion);
        }
    }

    private static void deserializeElementaryVoltageCnecResult(JsonParser jsonParser, VoltageCnec voltageCnec, VoltageResult voltageResult, Instant optimizedInstant, String jsonFileVersion) throws IOException {
        while (!jsonParser.nextToken().isStructEnd()) {
            if (!jsonParser.currentName().equals(KILOVOLT_UNIT)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.currentName()));
            } else {
                jsonParser.nextToken();
                deserializeElementaryVoltageCnecResultForUnit(jsonParser, voltageCnec, voltageResult, optimizedInstant, jsonFileVersion);
            }
        }
    }

    private static void deserializeElementaryVoltageCnecResultForUnit(JsonParser jsonParser, VoltageCnec voltageCnec, VoltageResult voltageResult, Instant optimizedInstant, String jsonFileVersion) throws IOException {
        double minVoltage = Double.NaN;
        double maxVoltage = Double.NaN;
        while (!jsonParser.nextToken().isStructEnd()) {

            switch (jsonParser.currentName()) {
                case VOLTAGE:
                    int primaryVersionNumber = getPrimaryVersionNumber(jsonFileVersion);
                    int subVersionNumber = getSubVersionNumber(jsonFileVersion);
                    if (primaryVersionNumber > 1 || subVersionNumber > 5) {
                        throw new OpenRaoException("Since RaoResult version 1.6, voltage values are divided into min and max.");
                    }
                    jsonParser.nextToken();
                    minVoltage = jsonParser.getDoubleValue();
                    maxVoltage = jsonParser.getDoubleValue();
                    break;
                case MIN_VOLTAGE:
                    jsonParser.nextToken();
                    minVoltage = jsonParser.getDoubleValue();
                    break;
                case MAX_VOLTAGE:
                    jsonParser.nextToken();
                    maxVoltage = jsonParser.getDoubleValue();
                    break;
                case MARGIN:
                    jsonParser.nextToken();
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", VOLTAGECNEC_RESULTS, jsonParser.currentName()));
            }

        }
        if (!Double.isNaN(minVoltage) && !Double.isNaN(maxVoltage)) {
            voltageResult.addMeasurement(minVoltage, maxVoltage, optimizedInstant, voltageCnec, Unit.KILOVOLT);
        }
    }
}
