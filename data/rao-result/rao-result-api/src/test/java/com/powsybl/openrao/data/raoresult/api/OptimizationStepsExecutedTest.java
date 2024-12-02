/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

class OptimizationStepsExecutedTest {

    @Test
    void testFirstPrevOnly() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
        assertFalse(optimizationStepsExecuted.hasRunSecondPreventive());
        assertFalse(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertFalse(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    void testFirstPrevFellback() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION;
        assertFalse(optimizationStepsExecuted.hasRunSecondPreventive());
        assertFalse(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertTrue(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    void testSecondPrevImprovedFirst() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST;
        assertTrue(optimizationStepsExecuted.hasRunSecondPreventive());
        assertFalse(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertFalse(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    void testSecondPrevFellbackToStart() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION;
        assertTrue(optimizationStepsExecuted.hasRunSecondPreventive());
        assertFalse(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertTrue(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    void testSecondPreventiveFellbackToFirstPrevResult() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION;
        assertTrue(optimizationStepsExecuted.hasRunSecondPreventive());
        assertTrue(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertFalse(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }
}
