package com.powsybl.openrao.monitoring.monitoringcommon.json;

import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MonitoringCommonSerializer {
    private MonitoringCommonSerializer() {
    }

    public static void serializeAppliedRas(Map<State, Set<NetworkAction>> appliedRas, JsonGenerator jsonGenerator) throws IOException {
        for (Map.Entry<State, Set<NetworkAction>> entry : appliedRas.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                .toList()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.INSTANT, entry.getKey().getInstant().toString());
            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.CONTINGENCY, optContingency.get().getId());
            }
            jsonGenerator.writeArrayFieldStart(JsonCommonMonitoringResultConstants.REMEDIAL_ACTIONS);
            for (String networkActionId : entry.getValue().stream().map(NetworkAction::getId).sorted().toList()) {
                jsonGenerator.writeString(networkActionId);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
