/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.monitoring.angle_monitoring.json.JsonAngleMonitoringResultConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResultSerializer extends JsonSerializer<AngleMonitoringResult> {

    AngleMonitoringResultSerializer() {

    }

    @Override
    public void serialize(AngleMonitoringResult angleMonitoringResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField(TYPE, ANGLE_MONITORING_RESULT);
        jsonGenerator.writeStringField(STATUS, angleMonitoringResult.getStatus().toString());
        // ANGLE_VALUES
        jsonGenerator.writeArrayFieldStart(ANGLE_VALUES);
        serializeAngleValues(angleMonitoringResult, jsonGenerator);
        jsonGenerator.writeEndArray();
        // APPLIED_CRAS
        jsonGenerator.writeArrayFieldStart(APPLIED_CRAS);
        serializeAppliedCras(angleMonitoringResult, jsonGenerator);
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

    private void serializeAngleValues(AngleMonitoringResult angleMonitoringResult, JsonGenerator jsonGenerator) throws IOException {
        for (AngleMonitoringResult.AngleResult angleResult : angleMonitoringResult.getAngleCnecsWithAngle().stream().sorted(Comparator.comparing(AngleMonitoringResult.AngleResult::getId)).collect(Collectors.toList())) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, angleResult.getState().getInstant().toString());
            Optional<Contingency> optContingency = angleResult.getState().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY, optContingency.get().getId());
            }
            jsonGenerator.writeStringField(CNEC_ID, angleResult.getAngleCnec().getId());
            jsonGenerator.writeNumberField(QUANTITY, Math.round(10.0 * angleResult.getAngle()) / 10.0);
            jsonGenerator.writeEndObject();
        }
    }

    private void serializeAppliedCras(AngleMonitoringResult angleMonitoringResult, JsonGenerator jsonGenerator) throws IOException {
        for (Map.Entry<State, Set<NetworkAction>> entry : angleMonitoringResult.getAppliedCras().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                .collect(Collectors.toList())) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, entry.getKey().getInstant().toString());
            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY, optContingency.get().getId());
            }
            jsonGenerator.writeArrayFieldStart(REMEDIAL_ACTIONS);
            for (NetworkAction networkAction : entry.getValue().stream().sorted(Comparator.comparing(NetworkAction::getId)).collect(Collectors.toList())) {
                jsonGenerator.writeString(networkAction.getId());
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
