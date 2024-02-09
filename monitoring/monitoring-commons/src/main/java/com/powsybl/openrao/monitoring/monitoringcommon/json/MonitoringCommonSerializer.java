package com.powsybl.openrao.monitoring.monitoringcommon.json;

import com.powsybl.openrao.data.cracapi.Contingency;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MonitoringCommonSerializer {
    private MonitoringCommonSerializer() {
    }

    public static void serializeAppliedRas(Map<State, Set<RemedialAction>> appliedRas, JsonGenerator jsonGenerator) throws IOException {
        for (Map.Entry<State, Set<RemedialAction>> entry : appliedRas.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getId()))
                .toList()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.INSTANT, entry.getKey().getInstant().toString());
            Optional<Contingency> optContingency = entry.getKey().getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(JsonCommonMonitoringResultConstants.CONTINGENCY, optContingency.get().getId());
            }
            jsonGenerator.writeArrayFieldStart(JsonCommonMonitoringResultConstants.REMEDIAL_ACTIONS);
            for (String remedialActionId : entry.getValue().stream().map(RemedialAction::getId).sorted().toList()) {
                jsonGenerator.writeString(remedialActionId);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }
}
