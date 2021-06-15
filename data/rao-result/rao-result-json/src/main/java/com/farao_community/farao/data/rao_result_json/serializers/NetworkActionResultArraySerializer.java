/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.CONTINGENCY_ID;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionResultArraySerializer {

    private NetworkActionResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<NetworkAction> sortedListOfNetworkActions = crac.getNetworkActions().stream()
            .sorted(Comparator.comparing(NetworkAction::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(NETWORKACTION_RESULTS);
        for (NetworkAction networkAction : sortedListOfNetworkActions) {
            serializeNetworkActionResult(networkAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeNetworkActionResult(NetworkAction networkAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(NETWORKACTION_ID, networkAction.getId());

        List<State> statesWhenNetworkActionIsActivated = crac.getStates().stream()
            .filter(state -> raoResult.isActivatedDuringState(state, networkAction))
            .sorted(STATE_COMPARATOR)
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(STATES_ACTIVATED_NETWORKACTION);
        for (State state: statesWhenNetworkActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstant(state.getInstant()));
            if (state.getContingency().isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, state.getContingency().get().getId());

            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }
}
