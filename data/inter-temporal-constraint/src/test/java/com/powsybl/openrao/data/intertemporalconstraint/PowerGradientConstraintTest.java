/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import org.junit.jupiter.api.Test;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PowerGradientConstraintTest {
    @Test
    void testPowerGradientConstraints() {
        PowerGradientConstraint pgc1 = new PowerGradientConstraint("generator", 200.0, VariationDirection.UP);
        assertEquals("generator", pgc1.networkElementId());
        assertEquals(200.0, pgc1.powerGradient());
        assertEquals(VariationDirection.UP, pgc1.variationDirection());

        PowerGradientConstraint pgc2 = new PowerGradientConstraint("load", 50.0, VariationDirection.DOWN);
        assertEquals("load", pgc2.networkElementId());
        assertEquals(50.0, pgc2.powerGradient());
        assertEquals(VariationDirection.DOWN, pgc2.variationDirection());
    }

    @Test
    void testConstraintWithNegativeGradientThrowsException() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new PowerGradientConstraint("generator", -100.0, VariationDirection.DOWN));
        assertEquals("powerGradient must be a positive value. For a decreasing variation, use VariationDirection.DOWN as the third parameter of the constructor.", exception.getMessage());
    }
}
