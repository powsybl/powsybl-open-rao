/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.google.common.hash.HashCode;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class OptimizationState {
    Instant instantBefore = null;
    Instant instantAfter = null;

    private OptimizationState(Instant instantBefore, Instant instantAfter) {
        this.instantBefore = instantBefore;
        this.instantAfter = instantAfter;
    }

    public static OptimizationState afterOptimizing(Instant instant) {
        return new OptimizationState(instant, null);
    }

    public static OptimizationState beforeOptimizing(Instant instant) {
        // TODO : restrict to last CURATIVE ?
        return new OptimizationState(null, instant);
    }

    public static OptimizationState between(Instant instantBefore, Instant instantAfter) {
        if (instantBefore.getOrder() != instantAfter.getOrder() - 1) {
            throw new FaraoException("Instants when defining OptimizationState should be subsequent");
        }
        return new OptimizationState(instantBefore, instantAfter);
    }

    public boolean isInitial() {
        return instantBefore == null && instantAfter.isPreventive();
    }

    public boolean isAfterPra() {
        return instantBefore != null && (instantBefore.isPreventive() || instantBefore.isOutage());

    }

    public boolean isAfterAra() {
        return (instantBefore != null && instantAfter != null) && (instantBefore.isAuto() || (!instantBefore.isCurative() && instantAfter.isCurative()));
    }

    public boolean isAfterCra() {
        return instantBefore != null && instantBefore.isCurative();
    }

    public boolean isIrrelevantFor(Instant instant) {
        if (instantBefore != null && instantBefore.comesAfter(instant)) {
            return true;
        }
        return false;
    }

    public Instant getOptimizedInstant() {
        return instantBefore;
    }

    public static OptimizationState min(OptimizationState optState1, OptimizationState optState2) {
        if (optState1.instantBefore == null) {
            return optState1;
        }
        if (optState2.instantBefore == null) {
            return optState2;
        }
        return optState1.instantBefore.comesBefore(optState2.instantBefore) ? optState1 : optState2;
    }

    public static OptimizationState initial(Crac crac) {
        return OptimizationState.beforeOptimizing(crac.getInstant(Instant.Kind.PREVENTIVE));
    }

    public static OptimizationState afterPra(Crac crac) {
        return OptimizationState.afterOptimizing(crac.getInstant(Instant.Kind.PREVENTIVE));
    }

    public static OptimizationState afterAra(Crac crac) {
        return OptimizationState.between(crac.getInstant(Instant.Kind.AUTO), crac.getFirstInstant(Instant.Kind.CURATIVE));
    }

    public static OptimizationState afterCra(Crac crac) {
        return OptimizationState.afterOptimizing(crac.getInstant(Instant.Kind.CURATIVE));
    }

    @Override
    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof OptimizationState)) {
            return false;
        }
        OptimizationState other = (OptimizationState) otherObj;
        if (this.isInitial()) {
            return other.isInitial();
        }
        if (this.isAfterPra()) {
            return other.isAfterPra();
        }
        if (this.isAfterAra()) {
            return other.isAfterAra();
        }
        return (instantBefore != null && other.instantBefore == this.instantBefore)
            || (instantAfter != null && other.instantAfter == this.instantAfter);
    }

    @Override
    public int hashCode() {
        if (this.isInitial()) {
            return HashCode.fromString("INITIAL_STATE___").hashCode();
        }
        if (this.isAfterPra()) {
            return HashCode.fromString("AFTER_PRA_STATE_").hashCode();
        }
        if (this.isAfterAra()) {
            return HashCode.fromString("AFTER_ARA_STATE_").hashCode();
        }
        return instantBefore.hashCode();
    }
}

/*
public enum OptimizationState_old {
    INITIAL(0, Instant.PREVENTIVE, "initial"),
    AFTER_PRA(1, Instant.PREVENTIVE, "after PRA"),
    AFTER_ARA(2, Instant.AUTO, "after ARA"),
    AFTER_CRA1(3, Instant.CURATIVE1, "after CRA1"),
    AFTER_CRA2(4, Instant.CURATIVE2, "after CRA2"),
    AFTER_CRA(5, Instant.CURATIVE, "after CRA");

    private final int order;
    private final Instant firstInstant;
    private final String name;

    OptimizationState_old(int order, Instant firstInstant, String name) {
        this.order = order;
        this.firstInstant = firstInstant;
        this.name = name;
    }

    private int getOrder() {
        return order;
    }

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

    public static OptimizationState beforeOptimizing(State state) {
        return beforeOptimizing(state.getInstant());
    }

    public static OptimizationState afterOptimizing(Instant instant) {
        return Arrays.stream(values()).sorted((o1, o2) -> Integer.compare(o2.getOrder(), o1.getOrder()))
            .filter(optimizationState -> !optimizationState.getFirstInstant().comesAfter(instant))
            .findFirst().orElseThrow();
    }

    public static OptimizationState afterOptimizing(State state) {
        return afterOptimizing(state.getInstant());
    }

    public Instant getFirstInstant() {
        return firstInstant;
    }

    @Override
    public String toString() {
        return name;
    }

    public static OptimizationState min(OptimizationState optimizationState1, OptimizationState optimizationState2) {
        return optimizationState1.order < optimizationState2.order ? optimizationState1 : optimizationState2;
    }
}
*/
