/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PowerGradientConstraintTest {
    @Test
    void buildExhaustivePowerGradientConstraint() {
        PowerGradientConstraint pgc = PowerGradientConstraint.builder()
            .withNetworkElementId("generator")
            .withMinPowerGradient(-100.0)
            .withMaxPowerGradient(100.0)
            .build();
        assertEquals("generator", pgc.getNetworkElementId());
        assertEquals(Optional.of(-100.0), pgc.getMinPowerGradient());
        assertEquals(Optional.of(100.0), pgc.getMaxPowerGradient());
    }

    @Test
    void buildPowerGradientConstraintWithoutMin() {
        PowerGradientConstraint pgc = PowerGradientConstraint.builder()
            .withNetworkElementId("generator")
            .withMaxPowerGradient(100.0)
            .build();
        assertEquals("generator", pgc.getNetworkElementId());
        assertTrue(pgc.getMinPowerGradient().isEmpty());
        assertEquals(Optional.of(100.0), pgc.getMaxPowerGradient());
    }

    @Test
    void buildPowerGradientConstraintWithoutMax() {
        PowerGradientConstraint pgc = PowerGradientConstraint.builder()
            .withNetworkElementId("generator")
            .withMinPowerGradient(-100.0)
            .build();
        assertEquals("generator", pgc.getNetworkElementId());
        assertEquals(Optional.of(-100.0), pgc.getMinPowerGradient());
        assertTrue(pgc.getMaxPowerGradient().isEmpty());
    }

    @Test
    void buildPowerGradientConstraintWithoutNetworkElement() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradientConstraint.builder().withMinPowerGradient(-100.0).withMaxPowerGradient(100.0).build());
        assertEquals("The network element id of the gradient constraint must be provided.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintWithoutMinAndMax() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradientConstraint.builder().withNetworkElementId("generator").build());
        assertEquals("At least one min or max power gradient value must be provided.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintPositiveMin() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradientConstraint.builder().withNetworkElementId("generator").withMinPowerGradient(100.0).build());
        assertEquals("The min power gradient must be negative.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintNegativeMax() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradientConstraint.builder().withNetworkElementId("generator").withMaxPowerGradient(-100.0).build());
        assertEquals("The max power gradient must be positive.", exception.getMessage());
    }
}
