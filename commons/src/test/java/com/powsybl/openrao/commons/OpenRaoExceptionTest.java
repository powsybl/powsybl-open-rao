/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class OpenRaoExceptionTest {
    @Test
    void testEmptyException() {
        OpenRaoException e = new OpenRaoException();
        assertNull(e.getMessage());
    }

    @Test
    void testMessageException() {
        OpenRaoException messageException = new OpenRaoException("Test message");
        assertEquals("Test message", messageException.getMessage());
    }

    @Test
    void testThrowableException() {
        Exception exception = new Exception("Test message");
        OpenRaoException throwableException = new OpenRaoException(exception);
        assertEquals(exception, throwableException.getCause());
    }

    @Test
    void testMessageThrowableException() {
        Exception exception = new Exception("Test message");
        OpenRaoException messageThrowableException = new OpenRaoException("Overload of message", exception);
        assertEquals("Overload of message", messageThrowableException.getMessage());
        assertEquals(exception, messageThrowableException.getCause());
    }
}
