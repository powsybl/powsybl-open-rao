/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class FaraoExceptionTest {
    @Test
    void testEmptyException() {
        FaraoException e = new FaraoException();
        assertNull(e.getMessage());
    }

    @Test
    void testMessageException() {
        FaraoException messageException = new FaraoException("Test message");
        assertEquals("Test message", messageException.getMessage());
    }

    @Test
    void testThrowableException() {
        Exception exception = new Exception("Test message");
        FaraoException throwableException = new FaraoException(exception);
        assertEquals(exception, throwableException.getCause());
    }

    @Test
    void testMessageThrowableException() {
        Exception exception = new Exception("Test message");
        FaraoException messageThrowableException = new FaraoException("Overload of message", exception);
        assertEquals("Overload of message", messageThrowableException.getMessage());
        assertEquals(exception, messageThrowableException.getCause());
    }
}
