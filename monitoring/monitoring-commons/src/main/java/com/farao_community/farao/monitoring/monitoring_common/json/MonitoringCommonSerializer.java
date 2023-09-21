package com.farao_community.farao.monitoring.monitoring_common.json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MonitoringCommonSerializer {
    private MonitoringCommonSerializer() {
    }

    public static void serializeAppliedRas(Map<State, Set<NetworkAction>> appliedRas, JsonGenerator jsonGenerator) throws IOException {
        for (Map.Entry<State, Set<NetworkAction>> entry : appliedRas.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                .collect(Collectors.toList())) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.INSTANT, entry.getKey().getInstant().toString());
            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.CONTINGENCY, optContingency.get().getId());
            }
            jsonGenerator.writeArrayFieldStart(JsonCommonMonitoringResultConstants.REMEDIAL_ACTIONS);
            for (NetworkAction networkAction : entry.getValue().stream().sorted(Comparator.comparing(NetworkAction::getId)).collect(Collectors.toList())) {
                jsonGenerator.writeString(networkAction.getId());
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
