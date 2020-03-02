/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class InstantTest {

    private Instant instant;

    @Before
    public void setUp() {
        instant = new Instant("instant", 15);
    }

    @Test
    public void getSeconds() {
        assertEquals(15, instant.getSeconds());
    }

    @Test
    public void setSeconds() {
        instant.setSeconds(5);
        assertEquals(5, instant.getSeconds());
    }

    @Test
    public void testDifferentBySeconds() {
        Instant instantA = new Instant("A", 12);
        Instant instantB = new Instant("A", 20);
        assertNotEquals(instantA, instantB);
    }

    @Test
    public void testDifferentById() {
        Instant instantA = new Instant("A", 12);
        Instant instantB = new Instant("B", 12);
        assertNotEquals(instantA, instantB);
    }

    @Test
    public void testEquals() {
        Instant instantA = new Instant("A", 12);
        Instant instantB = new Instant("A", 12);
        assertEquals(instantA, instantB);
    }

    @Test
    public void testHashCode() {
        Instant instantA = new Instant("A", 12);
        assertEquals("A12".hashCode(), instantA.hashCode());
    }
}
