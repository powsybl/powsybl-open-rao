/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */

class InstantImplTest {

    @Test
    void testPreventive() {
        Instant instant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        assertEquals(0, instant.getOrder());
        assertEquals("preventive", instant.toString());
        assertEquals(InstantKind.PREVENTIVE, instant.getInstantKind());
    }

    @Test
    void testCombineInstants() {
        Instant preventiveInstant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        Instant outageInstant = new InstantImpl("outage", InstantKind.OUTAGE, preventiveInstant);
        Instant autoInstant = new InstantImpl("auto", InstantKind.AUTO, outageInstant);
        Instant curativeInstant = new InstantImpl("curative", InstantKind.CURATIVE, autoInstant);

        assertEquals(0, preventiveInstant.getOrder());
        assertEquals(1, outageInstant.getOrder());
        assertEquals(2, autoInstant.getOrder());
        assertEquals(3, curativeInstant.getOrder());

        assertFalse(autoInstant.comesBefore(autoInstant));
        assertTrue(outageInstant.comesBefore(curativeInstant));
        assertFalse(outageInstant.comesBefore(preventiveInstant));
    }

    @Test
    void testInitialInstantIsProtected() {
        FaraoException exception = assertThrows(FaraoException.class, () -> new InstantImpl("initial", InstantKind.PREVENTIVE, null));
        assertEquals("Instant with id 'initial' can't be defined", exception.getMessage());
    }

    @Test
    void testIsPreventive() {
        Instant instant = new InstantImpl("my instant", InstantKind.PREVENTIVE, null);
        assertTrue(instant.isPreventive());
        assertFalse(instant.isOutage());
        assertFalse(instant.isAuto());
        assertFalse(instant.isCurative());
    }

    @Test
    void testIsOutage() {
        Instant instant = new InstantImpl("my instant", InstantKind.OUTAGE, null);
        assertFalse(instant.isPreventive());
        assertTrue(instant.isOutage());
        assertFalse(instant.isAuto());
        assertFalse(instant.isCurative());
    }

    @Test
    void testIsAuto() {
        Instant instant = new InstantImpl("my instant", InstantKind.AUTO, null);
        assertFalse(instant.isPreventive());
        assertFalse(instant.isOutage());
        assertTrue(instant.isAuto());
        assertFalse(instant.isCurative());
    }

    @Test
    void testIsCurative() {
        Instant instant = new InstantImpl("my instant", InstantKind.CURATIVE, null);
        assertFalse(instant.isPreventive());
        assertFalse(instant.isOutage());
        assertFalse(instant.isAuto());
        assertTrue(instant.isCurative());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant instant1 = new InstantImpl("my instant", InstantKind.PREVENTIVE, null);
        Instant instant2 = new InstantImpl("my instant", InstantKind.PREVENTIVE, null);
        Instant instantWithDifferentName = new InstantImpl("my other instant", InstantKind.PREVENTIVE, null);
        Instant instantWithDifferentKind = new InstantImpl("my instant", InstantKind.OUTAGE, null);
        Instant instantWithDifferentParent = new InstantImpl("my instant", InstantKind.OUTAGE, instant1);
        assertEquals(instant1, instant1);
        assertNotEquals(instant1, null);
        assertEquals(instant1, instant2);
        assertNotEquals(instant1, instantWithDifferentName);
        assertNotEquals(instant1, instantWithDifferentKind);
        assertNotEquals(instant1, instantWithDifferentParent);

        assertEquals(instant1.hashCode(), instant2.hashCode());
    }
}
