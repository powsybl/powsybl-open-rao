/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.powsybl.openrao.commons.NumberRounding.computeNumberOfRelevantDecimals;
import static com.powsybl.openrao.commons.NumberRounding.computeRelevantMarginDecimals;
import static com.powsybl.openrao.commons.NumberRounding.roundValueBasedOnMargin;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NumberRoundingTest {
    @Test
    void testComputeNumberOfRelevantDecimals() {
        assertEquals(1, computeNumberOfRelevantDecimals(-100d, 1));
        assertEquals(1, computeNumberOfRelevantDecimals(100d, 1));
        assertEquals(3, computeNumberOfRelevantDecimals(10d, 3));
        assertEquals(1, computeNumberOfRelevantDecimals(1d, 1));
        assertEquals(1, computeNumberOfRelevantDecimals(0.1, 1));
        assertEquals(2, computeNumberOfRelevantDecimals(0.01, 1));
        assertEquals(3, computeNumberOfRelevantDecimals(0.001, 1));
        assertEquals(4, computeNumberOfRelevantDecimals(0.0002, 1));
        assertEquals(5, computeNumberOfRelevantDecimals(0.00003, 1));
        assertEquals(6, computeNumberOfRelevantDecimals(-0.000008, 1));
        assertEquals(6, computeNumberOfRelevantDecimals(-1000.000008, 1));
        assertEquals(10, computeNumberOfRelevantDecimals(-1000.000008, 10));
        assertEquals(2, computeNumberOfRelevantDecimals(40.01, 1));
    }

    @Test
    void testComputeRelevantMarginDecimals() {
        assertEquals(2, computeRelevantMarginDecimals(100d, 2));
        assertEquals(1, computeRelevantMarginDecimals(-100d, 1));
        assertEquals(2, computeRelevantMarginDecimals(50.12345, 2));
        assertEquals(2, computeRelevantMarginDecimals(-50.12345, 2));
        assertEquals(3, computeRelevantMarginDecimals(-14.00001, 3));
    }

    @Test
    void testRoundValueBasedOnMargin() {
        BigDecimal roundedValueLightNegativeMargin = roundValueBasedOnMargin(40.0001, -0.0001, 2);
        assertEquals(40.0001, roundedValueLightNegativeMargin.doubleValue());
        assertEquals(4, roundedValueLightNegativeMargin.scale());
        BigDecimal roundedValueStrongNegativeMargin = roundValueBasedOnMargin(50.0001, -10.0001, 3);
        assertEquals(50.000, roundedValueStrongNegativeMargin.doubleValue());
        assertEquals(3, roundedValueStrongNegativeMargin.scale());
        BigDecimal roundedValueLightPositiveMargin = roundValueBasedOnMargin(39.9999, 0.0001, 2);
        assertEquals(40.00, roundedValueLightPositiveMargin.doubleValue());
        assertEquals(2, roundedValueLightPositiveMargin.scale());
        BigDecimal roundedValueStrongPositiveMargin = roundValueBasedOnMargin(29.9999, -10.0001, 3);
        assertEquals(30.000, roundedValueStrongPositiveMargin.doubleValue());
        assertEquals(3, roundedValueStrongPositiveMargin.scale());
    }
}
