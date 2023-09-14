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
    INITIAL(0, Instant.PREVENTIVE, "initial"),
    AFTER_PRA(1, Instant.PREVENTIVE, "after PRA"),
    AFTER_ARA(2, Instant.AUTO, "after ARA"),
    AFTER_CRA1(3, Instant.CURATIVE1, "after CRA1"),
    AFTER_CRA2(4, Instant.CURATIVE2, "after CRA2"),
    AFTER_CRA(5, Instant.CURATIVE, "after CRA");

    private final int order;
    private final Instant firstInstant;
    private final String name;

    OptimizationState(int order, Instant firstInstant, String name) {
        this.order = order;
        this.firstInstant = firstInstant;
        this.name = name;
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
            case CURATIVE1:
                return AFTER_ARA;
            case CURATIVE2:
                return AFTER_CRA1;
            case CURATIVE:
                return AFTER_CRA2;
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
            case CURATIVE1:
                return AFTER_CRA1;
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

    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns the earliest OptimizationState out of the 2 provided
     */
    public static OptimizationState min(OptimizationState optimizationState1, OptimizationState optimizationState2) {
        return optimizationState1.order < optimizationState2.order ? optimizationState1 : optimizationState2;
    }
}
