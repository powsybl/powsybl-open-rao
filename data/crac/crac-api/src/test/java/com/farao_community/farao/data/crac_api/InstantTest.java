/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import org.junit.Test;

import static org.junit.Assert.*;
import static com.farao_community.farao.data.crac_api.Instant.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class InstantTest {

    @Test
    public void testPreventive() {
        Instant instant = Instant.PREVENTIVE;
        assertEquals(0, instant.getOrder());
        assertEquals("preventive", instant.toString());
    }

    @Test
    public void testOutage() {
        Instant instant = Instant.OUTAGE;
        assertEquals(1, instant.getOrder());
        assertEquals("outage", instant.toString());
    }

    @Test
    public void testAuto() {
        Instant instant = Instant.AUTO;
        assertEquals(2, instant.getOrder());
        assertEquals("auto", instant.toString());
    }

    @Test
    public void testCurative() {
        Instant instant = Instant.CURATIVE;
        assertEquals(3, instant.getOrder());
        assertEquals("curative", instant.toString());
    }

    @Test
    public void testComesBefore() {
        assertFalse(PREVENTIVE.comesBefore(PREVENTIVE));
        assertTrue(PREVENTIVE.comesBefore(OUTAGE));
        assertTrue(PREVENTIVE.comesBefore(AUTO));
        assertTrue(PREVENTIVE.comesBefore(CURATIVE));

        assertFalse(OUTAGE.comesBefore(PREVENTIVE));
        assertFalse(OUTAGE.comesBefore(OUTAGE));
        assertTrue(OUTAGE.comesBefore(AUTO));
        assertTrue(OUTAGE.comesBefore(CURATIVE));

        assertFalse(AUTO.comesBefore(PREVENTIVE));
        assertFalse(AUTO.comesBefore(OUTAGE));
        assertFalse(AUTO.comesBefore(AUTO));
        assertTrue(AUTO.comesBefore(CURATIVE));

        assertFalse(CURATIVE.comesBefore(PREVENTIVE));
        assertFalse(CURATIVE.comesBefore(OUTAGE));
        assertFalse(CURATIVE.comesBefore(AUTO));
        assertFalse(CURATIVE.comesBefore(CURATIVE));
    }
}
