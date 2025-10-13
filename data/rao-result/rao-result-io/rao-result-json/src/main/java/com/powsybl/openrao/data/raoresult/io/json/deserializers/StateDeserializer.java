/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class StateDeserializer {
    private StateDeserializer() {

    }

    public static State getState(String instantId, String contingencyId, Crac crac, String parentType) {
        if (instantId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize RaoResult: no instant defined in activated states of %s", parentType));
        }
        Instant instant = crac.getInstant(instantId);
        if (instant.isPreventive()) {
            return crac.getPreventiveState();
        } else {
            if (contingencyId == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: no contingency defined in N-k activated states of %s", parentType));
            }
            State state = crac.getState(contingencyId, instant);
            if (state == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: State at instant %s with contingency %s not found in Crac", instantId, contingencyId));
            }
            return state;
        }
    }
}
