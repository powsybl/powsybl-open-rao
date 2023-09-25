package com.farao_community.farao.monitoring.monitoring_common.json;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.monitoring.monitoring_common.json.JsonCommonMonitoringResultConstants.*;
import static com.farao_community.farao.monitoring.monitoring_common.json.JsonCommonMonitoringResultConstants.REMEDIAL_ACTIONS;

public final class MonitoringCommonDeserializer {
    private static final String UNEXPECTED_FIELD_ERROR = "Unexpected field %s in %s";

    private MonitoringCommonDeserializer() {
    }

    public static void readAppliedRas(JsonParser jsonParser, Map<State, Set<NetworkAction>> appliedRas, Crac crac) throws IOException {
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
            State state = getState(instant, contingencyId, crac);
            if (appliedRas.containsKey(state)) {
                throw new FaraoException(String.format("State with instant %s and contingency %s has previously been defined in %s", instant.toString(), contingencyId, REMEDIAL_ACTIONS));
            } else {
                appliedRas.put(state, getNetworkActions(remedialActionIds, crac));
            }
        }
    }

    public static State getState(Instant instant, String contingencyId, Crac crac) {
        if (contingencyId == null) {
            if (instant.equals(Instant.PREVENTIVE)) {
                return crac.getPreventiveState();
            } else {
                throw new FaraoException(String.format("No contingency defined with instant %s", instant.toString()));
            }
        }
        State state = crac.getState(contingencyId, instant);
        if (state == null) {
            throw new FaraoException(String.format("State with instant %s and contingency %s does not exist in CRAC", instant.toString(), contingencyId));
        }
        return state;
    }

    private static Set<NetworkAction> getNetworkActions(Set<String> ids, Crac crac) {
        return ids.stream().map(crac::getNetworkAction).collect(Collectors.toSet());
    }
}
