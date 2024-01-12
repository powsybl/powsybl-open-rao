/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltagemonitoring.json;

import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonSerializer;
import com.powsybl.openrao.monitoring.voltagemonitoring.ExtremeVoltageValues;
import com.powsybl.openrao.monitoring.voltagemonitoring.VoltageMonitoringResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import static com.powsybl.openrao.monitoring.monitoringcommon.json.JsonCommonMonitoringResultConstants.*;
import static com.powsybl.openrao.monitoring.voltagemonitoring.json.JsonVoltageMonitoringResultConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultSerializer extends JsonSerializer<VoltageMonitoringResult> {

    VoltageMonitoringResultSerializer() {
    }

    @Override
    public void serialize(VoltageMonitoringResult voltageMonitoringResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField(TYPE, VOLTAGE_MONITORING_RESULT);
        jsonGenerator.writeStringField(STATUS, voltageMonitoringResult.getStatus().toString());
        // VOLTAGE_VALUE
        jsonGenerator.writeArrayFieldStart(VOLTAGE_VALUES);
        serializeExtremeVoltageValue(voltageMonitoringResult, jsonGenerator);
        jsonGenerator.writeEndArray();
        // APPLIED_RAS
        jsonGenerator.writeArrayFieldStart(APPLIED_RAS);
        MonitoringCommonSerializer.serializeAppliedRas(voltageMonitoringResult.getAppliedRas(), jsonGenerator);
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

    private void serializeExtremeVoltageValue(VoltageMonitoringResult voltageMonitoringResult, JsonGenerator jsonGenerator) throws IOException {
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> entry :
                voltageMonitoringResult.getExtremeVoltageValues().entrySet()
                        .stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                        .toList()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(CNEC_ID, entry.getKey().getId());
            jsonGenerator.writeNumberField(MIN, entry.getValue().getMin());
            jsonGenerator.writeNumberField(MAX, entry.getValue().getMax());
            jsonGenerator.writeEndObject();
        }
    }
}
