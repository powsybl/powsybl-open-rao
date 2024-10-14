/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NumberRounding {
    private static final double ROUNDING_EPSILON = 1e-8; // "noise" required to avoid side effects when rounding negative powers of 10

    private NumberRounding() {
    }

    /**
     * Computes the number of relevant decimals to display for a measured constraint (flow or angle).
     * If the constraint is negative, only one decimal suffices.
     * In case of very small violations, the number of decimals must be increased so the violation can be read directly in the results.
     *
     * @param value: value of the measured constraint
     * @return number of decimals
     */
    public static int computeNumberOfRelevantDecimals(double value) {
        return Math.max(1, (int) -Math.log10(Math.abs(value) + ROUNDING_EPSILON) + 1);
    }
}
