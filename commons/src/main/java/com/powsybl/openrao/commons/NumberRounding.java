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
        return value == (int) value ? 1 : Math.max(1, (int) -Math.log10(Math.abs(value - (int) value) + ROUNDING_EPSILON) + 1);
    }

    public static int computeRelevantMarginDecimals(double margin, int defaultDecimals) {
        if (margin >= 0) {
            return defaultDecimals; // no violation so no need to be more specific in terms of decimals
        }
        // otherwise, if the violation is greater than 1 in absolute value, it is sufficiently high to be seen even if the value is rounded
        return margin > -1 ? computeNumberOfRelevantDecimals(margin) : defaultDecimals;
    }

    public static BigDecimal roundValueBasedOnMargin(double value, double margin, int defaultDecimals) {
        return BigDecimal.valueOf(value).setScale(computeRelevantMarginDecimals(margin, defaultDecimals), RoundingMode.HALF_UP);
    }
}
