/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeName("pst-range-result")
public class PstRangeResult extends RangeActionResult implements Result {
    private Map<String, Integer> tapPerStates;

    @JsonCreator
    public PstRangeResult(@JsonProperty("setpointPerStates") Map<String, Double> setpointPerStates,
                          @JsonProperty("tapPerStates") Map<String, Integer> tapPerStates) {
        super(setpointPerStates);
        this.tapPerStates = tapPerStates;
    }

    public PstRangeResult(Set<String> stateIds) {
        super(stateIds);
        tapPerStates = new HashMap<>();
        stateIds.forEach(state -> tapPerStates.put(state, null));
    }

    public int getTap(String stateId) {
        return tapPerStates.getOrDefault(stateId, null);
    }

    public void setTap(String stateId, int tap) {
        tapPerStates.put(stateId, tap);
    }
}
