/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoExceptionTest {
    @Test
    public void testEmptyException() {
        FaraoException e = new FaraoException();
        assertNull(e.getMessage());
    }

    @Test
    public void testMessageException() {
        FaraoException messageException = new FaraoException("Test message");
        assertEquals("Test message", messageException.getMessage());
    }

    @Test
    public void testThrowableException() {
        Exception exception = new Exception("Test message");
        FaraoException throwableException = new FaraoException(exception);
        assertEquals(exception, throwableException.getCause());
    }

    @Test
    public void testMessageThrowableException() {
        Exception exception = new Exception("Test message");
        FaraoException messageThrowableException = new FaraoException("Overload of message", exception);
        assertEquals("Overload of message", messageThrowableException.getMessage());
        assertEquals(exception, messageThrowableException.getCause());
    }
}
