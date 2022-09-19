/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.monitoring.angle_monitoring.AngleMonitoringResult;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.monitoring.angle_monitoring.json.JsonAngleMonitoringResultConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoringResultDeserializer extends JsonDeserializer<AngleMonitoringResult> {
    private static final String UNEXPECTED_FIELD_ERROR = "Unexpected field %s in %s";

    private Crac crac;

    private AngleMonitoringResultDeserializer() {
        // should not be used
    }

    public AngleMonitoringResultDeserializer(Crac crac) {
        this.crac = crac;
    }

    @Override
    public AngleMonitoringResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String firstFieldName = jsonParser.nextFieldName();
        if (!firstFieldName.equals(TYPE) || !jsonParser.nextTextValue().equals(ANGLE_MONITORING_RESULT)) {
            throw new FaraoException(String.format("Type of document must be specified at the beginning as %s", ANGLE_MONITORING_RESULT));
        }
        AngleMonitoringResult.Status status = null;
        String secondFieldName = jsonParser.nextFieldName();
        if (!secondFieldName.equals(STATUS)) {
            throw new FaraoException("Status must be specified right after type of document.");
        } else {
            status = readStatus(jsonParser);
        }

        Set<AngleMonitoringResult.AngleResult> angleResults = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
        Map<State, Set<NetworkAction>> appliedCras = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.getCurrentName().equals(ANGLE_VALUES)) {
                jsonParser.nextToken();
                readAngleValues(jsonParser, angleResults);
            } else if (jsonParser.getCurrentName().equals(APPLIED_CRAS)) {
                jsonParser.nextToken();
                readAppliedCras(jsonParser, appliedCras);
            } else {
                throw new FaraoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.getCurrentName(), ANGLE_MONITORING_RESULT));
            }
        }
        return new AngleMonitoringResult(angleResults, appliedCras, status);
    }

    private AngleMonitoringResult.Status readStatus(JsonParser jsonParser) throws IOException {
        String statusString = jsonParser.nextTextValue();
        if (statusString.equals(AngleMonitoringResult.Status.SECURE.toString())) {
            return AngleMonitoringResult.Status.SECURE;
        } else if (statusString.equals(AngleMonitoringResult.Status.UNKNOWN.toString())) {
            return AngleMonitoringResult.Status.UNKNOWN;
        } else if (statusString.equals(AngleMonitoringResult.Status.UNSECURE.toString())) {
            return AngleMonitoringResult.Status.UNSECURE;
        } else {
            throw new FaraoException("Unexpected status: " + statusString);
        }
    }

    private void readAngleValues(JsonParser jsonParser, Set<AngleMonitoringResult.AngleResult> angleResults) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String contingencyId = null;
            Instant instant = null;
            String cnecId = null;
            Double quantity = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    case CNEC_ID:
                        cnecId = jsonParser.nextTextValue();
                        break;
                    case QUANTITY:
                        jsonParser.nextToken();
                        quantity = jsonParser.getDoubleValue();
                        break;
                    default:
                        throw new FaraoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.currentName(), ANGLE_VALUES));
                }
            }
            if (instant == null || cnecId == null || quantity == null) {
                throw new FaraoException(String.format("Instant, CNEC ID and quantity must be defined in %s", ANGLE_VALUES));
            }
            AngleCnec angleCnec = crac.getAngleCnec(cnecId);
            if (angleCnec == null) {
                throw new FaraoException(String.format("AngleCnec %s does not exist in the CRAC", cnecId));
            }
            State state = getState(instant, contingencyId);
            if (angleResults.stream().anyMatch(angleResult -> angleResult.getAngleCnec().equals(angleCnec) &&
                    angleResult.getState().equals(state))) {
                throw new FaraoException(String.format("Angle values for AngleCnec %s, instant %s and contingency %s are defined more than once", cnecId, instant.toString(), contingencyId));
            }
            angleResults.add(new AngleMonitoringResult.AngleResult(angleCnec, quantity));
        }
    }

    private void readAppliedCras(JsonParser jsonParser, Map<State, Set<NetworkAction>> appliedCras) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String contingencyId = null;
            Instant instant = null;
            Set<String> remedialActionIds = new TreeSet<>();
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
                        remedialActionIds = jsonParser.readValueAs(new TypeReference<TreeSet<String>>() {
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
            if (appliedCras.containsKey(state)) {
                throw new FaraoException(String.format("State with instant %s and contingency %s has previously been defined in %s", instant.toString(), contingencyId, REMEDIAL_ACTIONS));
            } else {
                appliedCras.put(state, getNetworkActions(remedialActionIds));
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
