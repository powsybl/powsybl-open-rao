/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantAdder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class InstantAdderTest {
    private SimpleCrac crac;

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
    }

    @Test(expected = FaraoException.class)
    public void testAddWithNoIdFail() {
        crac.newInstant()
                .setSeconds(0)
                .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddWithNoSecondsFail() {
        crac.newInstant()
                .setId("testId")
                .add();
    }

    @Test
    public void testAdd() {
        Instant instant1 = crac.newInstant()
                .setId("id1")
                .setSeconds(0)
                .add();
        Instant instant2 = crac.newInstant()
                .setId("id2")
                .setSeconds(10)
                .add();
        assertEquals(2, crac.getInstants().size());
        assertEquals(0, crac.getInstant("id1").getSeconds());
        assertSame(instant1, crac.getInstant("id1"));
        assertEquals(10, crac.getInstant("id2").getSeconds());
        assertSame(instant2, crac.getInstant("id2"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        InstantAdder tmp = new InstantAdderImpl(null);
    }
}
