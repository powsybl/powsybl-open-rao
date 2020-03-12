/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class NetworkActionResult implements Result {
    protected Map<String, Boolean> activationMap;

    public NetworkActionResult(Set<String> states) {
        activationMap = new HashMap<>();
        states.forEach(state -> activationMap.put(state, false));
    }

    public boolean isActivated(String state) {
        return activationMap.getOrDefault(state, false);
    }

    public void activate(String state) {
        activationMap.put(state, true);
    }

    public void deactivate(String state) {
        activationMap.put(state, false);
    }

}
