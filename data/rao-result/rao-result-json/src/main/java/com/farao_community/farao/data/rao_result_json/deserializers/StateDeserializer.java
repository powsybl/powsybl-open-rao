/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class StateDeserializer {
    private StateDeserializer() {

    }

    public static State getState(Instant instant, String contingencyId, Crac crac, String parentType) {
        if (instant == null) {
            throw new FaraoException(String.format("Cannot deserialize RaoResult: no instant defined in activated states of %s", parentType));
        }
        if (instant == Instant.PREVENTIVE) {
            return crac.getPreventiveState();
        } else {
            if (contingencyId == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: no contingency defined in N-k activated states of %s", parentType));
            }
            State state = crac.getState(contingencyId, instant);
            if (state == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: State at instant %s with contingency %s not found in Crac", instant, contingencyId));
            }
            return state;
        }
    }
}
