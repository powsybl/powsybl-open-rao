/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResult extends RangeActionResult {

    private Map<String, Integer> tapPerStates;

    public PstRangeResult(Map<String, Double> setpointPerStates, Map<String, Integer> tapPerStates) {
        super(setpointPerStates);
        this.tapPerStates = tapPerStates;
    }

    public PstRangeResult(Set<String> stateIds) {
        super(stateIds);
        tapPerStates = new HashMap<>();
        stateIds.forEach(state -> tapPerStates.put(state, null));
    }

    public Integer getTap(String stateId) {
        return tapPerStates.get(stateId);
    }

    public void setTap(String stateId, int tap) {
        tapPerStates.put(stateId, tap);
    }
}
