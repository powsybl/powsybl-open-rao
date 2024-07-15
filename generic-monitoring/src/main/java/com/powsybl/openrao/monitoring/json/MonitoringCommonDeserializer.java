package com.powsybl.openrao.monitoring.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.openrao.monitoring.json.JsonCommonMonitoringResultConstants.*;

public final class MonitoringCommonDeserializer {
    private static final String UNEXPECTED_FIELD_ERROR = "Unexpected field %s in %s";

    private MonitoringCommonDeserializer() {
    }

    public static void readAppliedRas(JsonParser jsonParser, Map<State, Set<RemedialAction<?>>> appliedRas, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String contingencyId = null;
            Instant instant = null;
            Set<String> remedialActionIds = new HashSet<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case INSTANT -> instant = crac.getInstant(jsonParser.nextTextValue());
                    case CONTINGENCY -> contingencyId = jsonParser.nextTextValue();
                    case REMEDIAL_ACTIONS -> {
                        jsonParser.nextToken();
                        remedialActionIds = jsonParser.readValueAs(new TypeReference<HashSet<String>>() {
                        });
                    }
                    default ->
                        throw new OpenRaoException(String.format(UNEXPECTED_FIELD_ERROR, jsonParser.currentName(), REMEDIAL_ACTIONS));
                }
            }
            if (instant == null) {
                throw new OpenRaoException(String.format("Instant must be defined in %s", REMEDIAL_ACTIONS));
            }
            // Get network Actions from string
            State state = getState(instant, contingencyId, crac);
            if (appliedRas.containsKey(state)) {
                throw new OpenRaoException(String.format("State with instant %s and contingency %s has previously been defined in %s", instant.getId(), contingencyId, REMEDIAL_ACTIONS));
            } else {
                appliedRas.put(state, getNetworkActions(remedialActionIds, crac));
            }
        }
    }

    public static State getState(Instant instant, String contingencyId, Crac crac) {
        if (contingencyId == null) {
            if (instant.isPreventive()) {
                return crac.getPreventiveState();
            } else {
                throw new OpenRaoException(String.format("No contingency defined with instant %s", instant));
            }
        }
        State state = crac.getState(contingencyId, instant);
        if (state == null) {
            throw new OpenRaoException(String.format("State with instant %s and contingency %s does not exist in CRAC", instant.getId(), contingencyId));
        }
        return state;
    }

    private static Set<RemedialAction<?>> getNetworkActions(Set<String> ids, Crac crac) {
        return ids.stream().map(crac::getNetworkAction).collect(Collectors.toSet());
    }
}
