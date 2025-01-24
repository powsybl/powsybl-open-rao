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
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class PowerGradientTest {
    @Test
    void buildExhaustivePowerGradientConstraint() {
        PowerGradient powerGradient = PowerGradient.builder()
            .withNetworkElementId("generator")
            .withMinValue(-100.0)
            .withMaxValue(100.0)
            .build();
        assertEquals("generator", powerGradient.getNetworkElementId());
        assertEquals(Optional.of(-100.0), powerGradient.getMinValue());
        assertEquals(Optional.of(100.0), powerGradient.getMaxValue());
    }

    @Test
    void buildPowerGradientConstraintWithoutMin() {
        PowerGradient powerGradient = PowerGradient.builder()
            .withNetworkElementId("generator")
            .withMaxValue(100.0)
            .build();
        assertEquals("generator", powerGradient.getNetworkElementId());
        assertTrue(powerGradient.getMinValue().isEmpty());
        assertEquals(Optional.of(100.0), powerGradient.getMaxValue());
    }

    @Test
    void buildPowerGradientConstraintWithoutMax() {
        PowerGradient powerGradient = PowerGradient.builder()
            .withNetworkElementId("generator")
            .withMinValue(-100.0)
            .build();
        assertEquals("generator", powerGradient.getNetworkElementId());
        assertEquals(Optional.of(-100.0), powerGradient.getMinValue());
        assertTrue(powerGradient.getMaxValue().isEmpty());
    }

    @Test
    void buildPowerGradientConstraintWithoutNetworkElement() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradient.builder().withMinValue(-100.0).withMaxValue(100.0).build());
        assertEquals("No network element id provided.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintWithoutMinAndMax() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradient.builder().withNetworkElementId("generator").build());
        assertEquals("At least one of min or max power gradient's value must be provided.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintPositiveMin() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradient.builder().withNetworkElementId("generator").withMinValue(100.0).build());
        assertEquals("The min value of the power gradient must be negative.", exception.getMessage());
    }

    @Test
    void buildPowerGradientConstraintNegativeMax() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> PowerGradient.builder().withNetworkElementId("generator").withMaxValue(-100.0).build());
        assertEquals("The max value of the power gradient must be positive.", exception.getMessage());
    }
}
