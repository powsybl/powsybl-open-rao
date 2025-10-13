/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class NetworkActionResultArraySerializer {

    private NetworkActionResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<NetworkAction> sortedListOfNetworkActions = crac.getNetworkActions().stream()
            .sorted(Comparator.comparing(NetworkAction::getId))
            .toList();

        jsonGenerator.writeArrayFieldStart(NETWORKACTION_RESULTS);
        for (NetworkAction networkAction : sortedListOfNetworkActions) {
            serializeNetworkActionResult(networkAction, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeNetworkActionResult(NetworkAction networkAction, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<State> statesWhenNetworkActionIsActivated = crac.getStates().stream()
                .filter(state -> safeIsActivatedDuringState(raoResult, state, networkAction))
                .sorted(STATE_COMPARATOR)
                .toList();

        if (statesWhenNetworkActionIsActivated.isEmpty()) {
            return;
        }

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(NETWORKACTION_ID, networkAction.getId());
        jsonGenerator.writeArrayFieldStart(STATES_ACTIVATED);
        for (State state : statesWhenNetworkActionIsActivated) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(INSTANT, serializeInstantId(state.getInstant()));
            Optional<Contingency> optContingency = state.getContingency();
            if (optContingency.isPresent()) {
                jsonGenerator.writeStringField(CONTINGENCY_ID, optContingency.get().getId());

            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

    private static boolean safeIsActivatedDuringState(RaoResult raoResult, State state, NetworkAction networkAction) {
        // isActivatedDuringState might throw an exception, for instance if the RAO was run one one state only, and the
        // state in argument of this method is not the same state.
        try {
            return raoResult.isActivatedDuringState(state, networkAction);
        } catch (OpenRaoException e) {
            return false;
        }
    }
}
