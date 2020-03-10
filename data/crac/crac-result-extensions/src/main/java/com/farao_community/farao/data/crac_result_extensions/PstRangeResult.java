/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResult extends RangeActionResult implements Result<PstRange> {

    private Map<State, Integer> tapPerStates;

    public PstRangeResult(Set<State> states) {
        super(states);
        tapPerStates = new HashMap<>();
        states.forEach(state -> tapPerStates.put(state, null));
    }

    public int getTap(State state) {
        return tapPerStates.getOrDefault(state, null);
    }

    public void setTap(State state, int tap) {
        tapPerStates.put(state, tap);
    }

}
