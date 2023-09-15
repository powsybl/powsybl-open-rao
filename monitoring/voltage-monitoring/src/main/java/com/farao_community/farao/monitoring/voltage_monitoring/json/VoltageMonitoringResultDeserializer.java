/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.voltage_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.farao_community.farao.monitoring.voltage_monitoring.ExtremeVoltageValues;
import com.farao_community.farao.monitoring.voltage_monitoring.VoltageMonitoringResult;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.monitoring.voltage_monitoring.json.JsonVoltageMonitoringResultConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoringResultDeserializer extends JsonDeserializer<VoltageMonitoringResult> {
    private static final String UNEXPECTED_FIELD_ERROR = "Unexpected field %s in %s";

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
        VoltageMonitoringResult.Status status = null;
        String secondFieldName = jsonParser.nextFieldName();
        if (!secondFieldName.equals(STATUS)) {
            throw new FaraoException("Status must be specified right after type of document.");
        } else {
            status = readStatus(jsonParser);
        }

        Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValues = new HashMap<>();
        Map<State, Set<NetworkAction>> appliedRas = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName().equals(VOLTAGE_VALUES)) {
                jsonParser.nextToken();
                readVoltageValues(jsonParser, extremeVoltageValues);
            } else if (jsonParser.getCurrentName().equals(APPLIED_RAS)) {
                jsonParser.nextToken();
                readAppliedRas(jsonParser, appliedRas);
            } else {
                throw new FaraoException(String.format("Unexpected field %s in %s", jsonParser.getCurrentName(), VOLTAGE_MONITORING_RESULT));
            }
        }
        return new VoltageMonitoringResult(extremeVoltageValues, appliedRas, status);
    }

    private VoltageMonitoringResult.Status readStatus(JsonParser jsonParser) throws IOException {
        String statusString = jsonParser.nextTextValue();
        try {
            return VoltageMonitoringResult.Status.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            throw new FaraoException(String.format("Unhandled status : %s", statusString));
        }
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

    private void readAppliedRas(JsonParser jsonParser, Map<State, Set<NetworkAction>> appliedRas) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String contingencyId = null;
            Instant instant = null;
            Set<String> remedialActionIds = new HashSet<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    case REMEDIAL_ACTIONS:
                        jsonParser.nextToken();
                        remedialActionIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                        break;
                    default:
                        throw new FaraoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.currentName(), REMEDIAL_ACTIONS));
                }
            }
            if (instant == null) {
                throw new FaraoException(String.format("Instant must be defined in %s", REMEDIAL_ACTIONS));
            }
            // Get network Actions from string
            State state = getState(instant, contingencyId);
            if (appliedRas.containsKey(state)) {
                throw new FaraoException(String.format("State with instant %s and contingency %s has previously been defined in %s", instant.toString(), contingencyId, REMEDIAL_ACTIONS));
            } else {
                appliedRas.put(state, getNetworkActions(remedialActionIds));
            }
        }
    }

    private State getState(Instant instant, String contingencyId) {
        if (Objects.isNull(contingencyId)) {
            if (instant.equals(Instant.PREVENTIVE)) {
                return crac.getPreventiveState();
            } else {
                throw new FaraoException(String.format("No contingency defined with instant %s", instant.toString()));
            }
        }
        State state = crac.getState(contingencyId, instant);
        if (Objects.isNull(state)) {
            throw new FaraoException(String.format("State with instant %s and contingency %s does not exist in CRAC", instant.toString(), contingencyId));
        }
        return state;
    }

    private Set<NetworkAction> getNetworkActions(Set<String> ids) {
        return crac.getNetworkActions().stream().filter(networkAction -> ids.contains(networkAction.getId())).collect(Collectors.toSet());
    }
}
