/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class ComputationStatusMapSerializer {

    private ComputationStatusMapSerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        List<State> sortedListOfStates = crac.getStates().stream()
            .sorted(STATE_COMPARATOR)
            .toList();

        jsonGenerator.writeArrayFieldStart(COMPUTATION_STATUS_MAP);
        for (State state : sortedListOfStates) {
            ComputationStatus computationStatus = raoResult.getComputationStatus(state);
            if (!computationStatus.equals(ComputationStatus.DEFAULT)) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField(COMPUTATION_STATUS, raoResult.getComputationStatus(state).toString());
                jsonGenerator.writeStringField(INSTANT, serializeInstantId(state.getInstant()));
                Optional<Contingency> optContingency = state.getContingency();
                if (optContingency.isPresent()) {
                    jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());
                }
                jsonGenerator.writeEndObject();
            }
        }
        jsonGenerator.writeEndArray();
    }
}
