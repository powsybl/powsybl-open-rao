/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionResult {
    protected Map<State, Boolean> activationMap;

    public NetworkActionResult(Set<State> states) {
        activationMap = new HashMap<>();
        states.forEach(state -> activationMap.put(state, false));
    }

    public boolean isActivated(State state) {
        return activationMap.getOrDefault(state, false);
    }

    public void activate(State state) {
        activationMap.put(state, true);
    }

    public void deactivate(State state) {
        activationMap.put(state, false);
    }

}
