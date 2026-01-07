/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class BorderDirectionTest {
    @Test
    void checkThatBorderDirectionIsCorrectlyCreated() {
        BorderDirection borderDirection = new BorderDirection("Paris", "Berlin", true);
        assertEquals("Paris", borderDirection.from());
        assertEquals("Berlin", borderDirection.to());
        assertTrue(borderDirection.isAhc());
    }

    @Test
    void checkThatBorderDirectionCreationThrowsWhenFromIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new BorderDirection(null, "To", false),
            "Null 'from' in BorderDirection creation should throw but does not"
        );
        assertEquals("BorderDirection creation does not allow null attribute 'from'", thrown.getMessage());
    }

    @Test
    void checkThatBorderDirectionCreationThrowsWhenToIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new BorderDirection("From", null, false),
            "Null 'to' in BorderDirection creation should throw but does not"
        );
        assertEquals("BorderDirection creation does not allow null attribute 'to'", thrown.getMessage());
    }
}
