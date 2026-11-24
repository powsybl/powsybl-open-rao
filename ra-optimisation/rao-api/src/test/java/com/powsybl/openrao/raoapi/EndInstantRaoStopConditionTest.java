/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class EndInstantRaoStopConditionTest {
    @Test
    void testRaoInterruptedAfterOneSecond() throws InterruptedException {
        EndInstantRaoStopCondition raoStopCondition = new EndInstantRaoStopCondition(Instant.now().plusSeconds(1));

        assertFalse(raoStopCondition.shouldStop());
        assertDoesNotThrow(raoStopCondition::checkNotStopped);

        // wait one second
        TimeUnit.SECONDS.sleep(1);

        assertTrue(raoStopCondition.shouldStop());
        RaoInterruptedException exception = assertThrows(RaoInterruptedException.class, raoStopCondition::checkNotStopped);
        assertEquals("RAO was interrupted.", exception.getMessage());
    }
}
