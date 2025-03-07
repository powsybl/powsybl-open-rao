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
public final class MeasurementRounding {
    private MeasurementRounding() {
    }

    /**
     * Rounds a measured value with a number of decimals determined from the associated margin with a threshold.
     * This helps to increase the decimal precision in case a violation is very narrow (order of magnitude is a negative
     * power of ten) and could go unnoticed by the user when reading the results if the rounding is performed with a
     * fixed number of decimals (ex: 100.0001 being rounded to 100 would make the 0.0001 violation with respect to a
     * threshold of 100 invisible). The rule is as follows:
     *
     * <ul>
     *     <li>
     *         if the margin is positive, there is no violation and no specific rounding is required so the default
     *         number of decimals is used
     *     </li>
     *     <li>
     *         if the margin is negative but greater than 1 in absolute value, the violation is big enough for the
     *         rounding not to <i>hide</i> the violation part of the measured value so the default number of decimals is
     *         used
     *     </li>
     *     <li>
     *         if the margin is negative and strictly lower than 1 in absolute value, the number of required decimals is
     *         computed with the formula ⌈-log10(margin)⌉ and returned, except if it is lower than the default number of
     *         decimals in which case the latter is returned
     *     </li>
     * </ul>
     *
     * @param value           : the measured value to round
     * @param margin          : the margin between the measured value and the threshold
     * @param defaultDecimals : the default minimum number of decimals used to round the value
     * @return rounded value as a BigDecimal
     */
    public static BigDecimal roundValueBasedOnMargin(double value, double margin, int defaultDecimals) {
        int relevantDecimals;
        if (Double.isNaN(margin) || margin >= 0 || margin <= -1) {
            relevantDecimals = defaultDecimals;
        } else {
            // for a number x in [0, 1[, the position of the first decimal which is not a 0 is given by ⌈-log10(x)⌉
            relevantDecimals = Math.max(defaultDecimals, (int) Math.ceil(-Math.log10(Math.abs(margin))));
        }
        double boundedValue = Math.min(Double.MAX_VALUE, Math.max(-Double.MAX_VALUE, value));
        return BigDecimal.valueOf(boundedValue).setScale(relevantDecimals, RoundingMode.HALF_UP);
    }
}
