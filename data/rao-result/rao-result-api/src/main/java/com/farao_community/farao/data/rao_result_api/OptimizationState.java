/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public enum OptimizationState {
    INITIAL(Instant.PREVENTIVE),
    AFTER_PRA(Instant.PREVENTIVE),
    AFTER_ARA(Instant.AUTO),
    AFTER_CRA(Instant.CURATIVE);

    private final Instant firstInstant;

    OptimizationState(Instant firstInstant) {
        this.firstInstant = firstInstant;
    }

    /**
     * Returns the OptimizationState that corresponds to the situation before optimizing a given instant
     */
    public static OptimizationState beforeOptimizing(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
            case OUTAGE:
                return INITIAL;
            case AUTO:
                return AFTER_PRA;
            case CURATIVE:
                return AFTER_ARA;
            default:
                throw new FaraoException(String.format("Unknown instant %s", instant));
        }
    }

    /**
     * Returns the OptimizationState that corresponds to the situation before optimizing a given state
     */
    public static OptimizationState beforeOptimizing(State state) {
        return beforeOptimizing(state.getInstant());
    }

    /**
     * Returns the OptimizationState that corresponds to the situation after optimizing a given instant
     */
    public static OptimizationState afterOptimizing(Instant instant) {
        switch (instant) {
            case PREVENTIVE:
            case OUTAGE:
                return AFTER_PRA;
            case AUTO:
                return AFTER_ARA;
            case CURATIVE:
                return AFTER_CRA;
            default:
                throw new FaraoException(String.format("Unknown instant %s", instant));
        }
    }

    /**
     * Returns the OptimizationState that corresponds to the situation after optimizing a given state
     */
    public static OptimizationState afterOptimizing(State state) {
        return afterOptimizing(state.getInstant());
    }

    /**
     * Returns the first instant for which the optimization state is relevant
     * Instants coming before this first instant cannot be used with this optimization state
     */
    public Instant getFirstInstant() {
        return firstInstant;
    }
}
