/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.farao_community.farao.monitoring.voltage_monitoring.ExtremeVoltageValues;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.farao_community.farao.monitoring.voltage_monitoring.json.JsonVoltageMonitoringResultConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultDeserializer extends JsonDeserializer<VoltageMonitoringResult> {

    private Crac crac;

    private VoltageMonitoringResultDeserializer() {
        // should not be used
    }

    public VoltageMonitoringResultDeserializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public VoltageMonitoringResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String firstFieldName = jsonParser.nextFieldName();
        if (!firstFieldName.equals(TYPE) || !jsonParser.nextTextValue().equals(VOLTAGE_MONITORING_RESULT)) {
            throw new FaraoException(String.format("type of document must be specified at the beginning as %s", VOLTAGE_MONITORING_RESULT));
        }
        Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName().equals(VOLTAGE_VALUES)) {
                jsonParser.nextToken();
                readVoltageValues(jsonParser, extremeVoltageValues);
            } else {
                throw new FaraoException(String.format("Unexpected field %s in %s", jsonParser.getCurrentName(), VOLTAGE_MONITORING_RESULT));
            }
        }
        return new VoltageMonitoringResult(extremeVoltageValues);
    }

    private void readVoltageValues(JsonParser jsonParser, Map<VoltageCnec, ExtremeVoltageValues> voltageValues) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String cnecId = null;
            Double min = null;
            Double max = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case CNEC_ID:
                        cnecId = jsonParser.nextTextValue();
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        min = jsonParser.getDoubleValue();
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        max = jsonParser.getDoubleValue();
                        break;
                    default:
                        throw new FaraoException(String.format("Unexpected field %s in %s", jsonParser.currentName(), VOLTAGE_VALUES));
                }
            }
            if (cnecId == null || min == null || max == null) {
                throw new FaraoException(String.format("CNEC ID, min and max voltage values must be defined in %s", VOLTAGE_VALUES));
            }
            VoltageCnec voltageCnec = crac.getVoltageCnec(cnecId);
            if (voltageCnec == null) {
                throw new FaraoException(String.format("VoltageCnec %s does not exist in the CRAC", cnecId));
            }
            if (voltageValues.containsKey(voltageCnec)) {
                throw new FaraoException(String.format("Voltage values for VoltageCnec %s are defined more than once", cnecId));
            }
            voltageValues.put(voltageCnec, new ExtremeVoltageValues(Set.of(min, max)));
        }
    }
}
