/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MeasurementRoundingTest {
    @Test
    void testRoundValueBasedOnMargin() {
        // light negative constraints (-1 <= constraint < 0)
        assertEquals(BigDecimal.valueOf(40.0001), roundValueBasedOnMargin(40.0001, -0.0001, 2));
        assertEquals(4, roundValueBasedOnMargin(40.0001, -0.0001, 2).scale());

        assertEquals(BigDecimal.valueOf(40.0002), roundValueBasedOnMargin(40.0002, -0.0002, 2));
        assertEquals(4, roundValueBasedOnMargin(40.0002, -0.0002, 2).scale());

        // strong negative constraints (constraint < -1)
        assertEquals(BigDecimal.valueOf(50.000).setScale(3, RoundingMode.HALF_UP), roundValueBasedOnMargin(50.0001, -10.0001, 3));
        assertEquals(3, roundValueBasedOnMargin(50.0001, -10.0001, 3).scale());

        assertEquals(BigDecimal.valueOf(50.0), roundValueBasedOnMargin(50.00345, -10.00345, 1));
        assertEquals(1, roundValueBasedOnMargin(50.00345, -10.0001, 1).scale());

        // positive constraints
        assertEquals(BigDecimal.valueOf(40.0).setScale(2, RoundingMode.HALF_UP), roundValueBasedOnMargin(39.9999, 0.0001, 2));
        assertEquals(2, roundValueBasedOnMargin(39.9999, 0.0001, 2).scale());

        assertEquals(BigDecimal.valueOf(30.000).setScale(3, RoundingMode.HALF_UP), roundValueBasedOnMargin(29.9999, -10.0001, 3));
        assertEquals(3, roundValueBasedOnMargin(29.9999, -10.0001, 3).scale());

        assertEquals(BigDecimal.valueOf(30.00).setScale(2, RoundingMode.HALF_UP), roundValueBasedOnMargin(30.0, 0.0, 2));
        assertEquals(2, roundValueBasedOnMargin(30.0, 0.0, 2).scale());
    }
}
