/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MeasurementRoundingTest {
    @Test
    void testRoundValueBasedOnMargin() {
        // light negative constraints (-1 <= constraint < 0)
        assertEquals(40.0001, roundValueBasedOnMargin(40.0001, -0.0001, 2).doubleValue());
        assertEquals(4, roundValueBasedOnMargin(40.0001, -0.0001, 2).scale());

        assertEquals(40.0002, roundValueBasedOnMargin(40.0002, -0.0002, 2).doubleValue());
        assertEquals(4, roundValueBasedOnMargin(40.0002, -0.0002, 2).scale());

        // strong negative constraints (constraint < -1)
        assertEquals(50.000, roundValueBasedOnMargin(50.0001, -10.0001, 3).doubleValue());
        assertEquals(3, roundValueBasedOnMargin(50.0001, -10.0001, 3).scale());

        assertEquals(50.0, roundValueBasedOnMargin(50.00345, -10.00345, 1).doubleValue());
        assertEquals(1, roundValueBasedOnMargin(50.00345, -10.0001, 1).scale());

        // positive constraints
        assertEquals(40.00, roundValueBasedOnMargin(39.9999, 0.0001, 2).doubleValue());
        assertEquals(2, roundValueBasedOnMargin(39.9999, 0.0001, 2).scale());

        assertEquals(30.000, roundValueBasedOnMargin(29.9999, -10.0001, 3).doubleValue());
        assertEquals(3, roundValueBasedOnMargin(29.9999, -10.0001, 3).scale());

        assertEquals(30.00, roundValueBasedOnMargin(30.0, 0.0, 2).doubleValue());
        assertEquals(2, roundValueBasedOnMargin(30.0, 0.0, 2).scale());
    }
}
