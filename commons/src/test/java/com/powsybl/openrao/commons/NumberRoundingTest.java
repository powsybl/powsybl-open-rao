/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.commons.NumberRounding.computeNumberOfRelevantDecimals;
import static com.powsybl.openrao.commons.NumberRounding.roundDoubleValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class NumberRoundingTest {
    @Test
    void testComputeNumberOfRelevantDecimals() {
        assertEquals(1, computeNumberOfRelevantDecimals(-100d));
        assertEquals(1, computeNumberOfRelevantDecimals(100d));
        assertEquals(1, computeNumberOfRelevantDecimals(10d));
        assertEquals(1, computeNumberOfRelevantDecimals(1d));
        assertEquals(1, computeNumberOfRelevantDecimals(0.1));
        assertEquals(2, computeNumberOfRelevantDecimals(0.01));
        assertEquals(3, computeNumberOfRelevantDecimals(0.001));
        assertEquals(4, computeNumberOfRelevantDecimals(0.0002));
        assertEquals(5, computeNumberOfRelevantDecimals(0.00003));
        assertEquals(6, computeNumberOfRelevantDecimals(-0.000008));
    }

    @Test
    void testRoundDouble() {
        assertEquals(100d, roundDoubleValue(100d, 1));
        assertEquals(0.0003, roundDoubleValue(0.0003, 1));
        assertEquals(0.0001, roundDoubleValue(0.000123456, 1));
        assertEquals(0.0001235, roundDoubleValue(0.000123456, 7));
    }
}
