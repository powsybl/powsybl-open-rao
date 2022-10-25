/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

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
    private final Map<State, Integer> postAraTaps;
    private final Map<State, Integer> tapPerState;

    public PstRangeActionResult() {
        super();
        postAraTaps = new HashMap<>();
        tapPerState = new HashMap<>();
    }

    public int getPreOptimizedTapOnState(State state) {
        if (state.isPreventive()) {
            return initialTap;
        } else {
            return getOptimizedTapOnState(stateBefore(state));
        }
    }

    public int getOptimizedTapOnState(State state) {
        if (tapPerState.containsKey(state)) {
            return tapPerState.get(state);
        }
        if (postAraTaps.containsKey(state)) {
            return postAraTaps.get(state);
        }
        if (Objects.isNull(state) || state.isPreventive()) { // preventiveState can be null
            if (Objects.nonNull(postPraTap)) {
                return postPraTap;
            } else {
                return initialTap;
            }
        }
        return getOptimizedTapOnState(stateBefore(state));
    }

    public void addActivationForState(State state, int tap, double setpoint) {
        tapPerState.put(state, tap);
        setpointPerState.put(state, setpoint);
        if (state.isPreventive()) {
            preventiveState = state;
        }
    }

    public void addPostAraResult(State state, int tap, double setpoint) {
        postAraTaps.put(state, tap);
        postAraSetpoints.put(state, setpoint);
    }

    public void setInitialTap(int initialTap) {
        this.initialTap = initialTap;
    }

    public void setPostPraTap(int postPraTap) {
        this.postPraTap = postPraTap;
    }
}
