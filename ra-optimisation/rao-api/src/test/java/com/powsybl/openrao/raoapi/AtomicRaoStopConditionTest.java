/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class AtomicRaoStopConditionTest {
    @Test
    void testUserManuallyInterruptsRao() {
        AtomicBoolean stopCondition = new AtomicBoolean(false);
        AtomicRaoStopCondition atomicRaoStopCondition = new AtomicRaoStopCondition(stopCondition);

        assertFalse(atomicRaoStopCondition.shouldStop());
        assertDoesNotThrow(atomicRaoStopCondition::checkNotStopped);

        // user manually sets the stop condition to true
        stopCondition.set(true);

        assertTrue(atomicRaoStopCondition.shouldStop());
        RaoComputationInterruptedException exception = assertThrows(RaoComputationInterruptedException.class, atomicRaoStopCondition::checkNotStopped);
        assertEquals("RAO was interrupted.", exception.getMessage());
    }
}
