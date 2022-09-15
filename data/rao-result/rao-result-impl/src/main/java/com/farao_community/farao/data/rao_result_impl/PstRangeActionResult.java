/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionResult extends RangeActionResult {

    private int initialTap;
    private Integer postPraTap;
    private final Map<State, Integer> tapPerState;

    public PstRangeActionResult() {
        super();
        tapPerState = new HashMap<>();
    }

    public int getPreOptimizedTapOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState) && tapPerState.containsKey(preventiveState)) {
            return tapPerState.get(preventiveState);
        } else if (!state.getInstant().equals(Instant.PREVENTIVE) && postPraTap != null) {
            return postPraTap;
        }
        return initialTap;
    }

    public int getOptimizedTapOnState(State state) {
        // does not handle RA applicable on OUTAGE instant
        // does only handle RA applicable in PREVENTIVE and CURATIVE instant
        if (tapPerState.containsKey(state)) {
            return tapPerState.get(state);
        } else if (!state.getInstant().equals(Instant.PREVENTIVE) && Objects.nonNull(preventiveState) && tapPerState.containsKey(preventiveState)) {
            return tapPerState.get(preventiveState);
        } else if (postPraTap != null) {
            return postPraTap;
        }
        return initialTap;
    }

    public void addActivationForState(State state, int tap, double setpoint) {
        tapPerState.put(state, tap);
        setpointPerState.put(state, setpoint);
        if (state.isPreventive()) {
            preventiveState = state;
        }
    }

    public void setInitialTap(int initialTap) {
        this.initialTap = initialTap;
    }

    public void setPostPraTap(int postPraTap) {
        this.postPraTap = postPraTap;
    }
}
