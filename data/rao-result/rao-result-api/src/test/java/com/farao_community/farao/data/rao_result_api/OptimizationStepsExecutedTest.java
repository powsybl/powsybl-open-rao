/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_api;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Martin Belthle {@literal <martin.belthle at rte-france.com>}
 */

public class OptimizationStepsExecutedTest {

    @Test
    public void testFirstPrevOnly() {
        OptimizationStepsExecuted optimizationStepsExecuted = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
        assertFalse(optimizationStepsExecuted.hasRunSecondPreventive());
        assertFalse(optimizationStepsExecuted.hasFallenBackToFirstPreventiveSituation());
        assertFalse(optimizationStepsExecuted.hasFallenBackToInitialSituation());
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertTrue(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertFalse(optimizationStepsExecuted.isOverwritePossible(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
    }

    @Test
    public void testFirstPrevFellback() {
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
    public void testSecondPrevImprovedFirst() {
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
    public void testSecondPrevFellbackToStart() {
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
    public void testSecondPreventiveFellbackToFirstPrevResult() {
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
