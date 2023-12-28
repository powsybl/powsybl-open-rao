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
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testPreventive() {
        Instant instant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        assertEquals(0, instant.getOrder());
        assertEquals(PREVENTIVE_INSTANT_ID, instant.toString());
        assertEquals(InstantKind.PREVENTIVE, instant.getKind());
    }

    @Test
    void testCombineInstants() {
        Instant preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        Instant outageInstant = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, preventiveInstant);
        Instant autoInstant = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, outageInstant);
        Instant curativeInstant = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, autoInstant);

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
        Instant preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        Instant instant = new InstantImpl("my instant", InstantKind.OUTAGE, preventiveInstant);
        assertFalse(instant.isPreventive());
        assertTrue(instant.isOutage());
        assertFalse(instant.isAuto());
        assertFalse(instant.isCurative());
    }

    @Test
    void testIsAuto() {
        Instant preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        Instant instant = new InstantImpl("my instant", InstantKind.AUTO, preventiveInstant);
        assertFalse(instant.isPreventive());
        assertFalse(instant.isOutage());
        assertTrue(instant.isAuto());
        assertFalse(instant.isCurative());
    }

    @Test
    void testIsCurative() {
        Instant preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        Instant instant = new InstantImpl("my instant", InstantKind.CURATIVE, preventiveInstant);
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
        Instant instantWithDifferentParent = new InstantImpl("my instant", InstantKind.PREVENTIVE, instant1);
        Instant instantWithDifferentKind = new InstantImpl("my instant", InstantKind.OUTAGE, instant1);
        assertEquals(instant1, instant1);
        assertNotEquals(instant1, null);
        assertEquals(instant1, instant2);
        assertNotEquals(instant1, instantWithDifferentName);
        assertNotEquals(instant1, instantWithDifferentParent);
        assertNotEquals(instantWithDifferentParent, instantWithDifferentKind);

        assertEquals(instant1.hashCode(), instant2.hashCode());
    }

    @Test
    void testGetInstantBefore() {
        InstantImpl preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        InstantImpl outageInstant = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, preventiveInstant);
        InstantImpl autoInstant = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, outageInstant);
        InstantImpl curativeInstant = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, autoInstant);

        assertEquals(null, preventiveInstant.getInstantBefore());
        assertEquals(preventiveInstant, outageInstant.getInstantBefore());
        assertEquals(outageInstant, autoInstant.getInstantBefore());
        assertEquals(autoInstant, curativeInstant.getInstantBefore());
    }
}
