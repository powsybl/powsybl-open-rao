/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ComputationStatusMapDeserializer {

    private ComputationStatusMapDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {
        Map<String, State> statePerId = crac.getStates().stream().collect(Collectors.toMap(State::getId, Function.identity(), (fst, snd) -> fst));

        while (!jsonParser.nextToken().isStructEnd()) {
            String stateId = jsonParser.getCurrentName();
            if (!statePerId.containsKey(stateId)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: Crac does not contain state %s", stateId));
            }
            State state = statePerId.get(stateId);
            jsonParser.nextToken();
            raoResult.setComputationStatus(state, ComputationStatus.valueOf(jsonParser.getText()));
        }
    }
}
