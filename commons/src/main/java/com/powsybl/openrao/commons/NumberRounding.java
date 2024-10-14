/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class NumberRounding {
    private static final double ROUNDING_EPSILON = 1e-8; // "noise" required to avoid side effects when rounding negative powers of 10

    private NumberRounding() {
    }

    /**
     * Computes the number of relevant decimals of a value.
     *
     * @param value: value to round
     * @return number of decimals
     */
    public static int computeNumberOfRelevantDecimals(double value) {
        return Math.max(1, (int) -Math.log10(Math.abs(value) + ROUNDING_EPSILON) + 1);
    }

    /**
     * Computes the number of relevant decimals of a value and rounds it with this number of decimals.
     *
     * @param value: value to round
     * @param minimumDecimals: minimum number of decimals to display
     * @return number of decimals
     */
    public static double roundDoubleValue(double value, int minimumDecimals) {
        return BigDecimal.valueOf(value).setScale(Math.max(minimumDecimals, computeNumberOfRelevantDecimals(value)), RoundingMode.HALF_UP).doubleValue();
    }
}
