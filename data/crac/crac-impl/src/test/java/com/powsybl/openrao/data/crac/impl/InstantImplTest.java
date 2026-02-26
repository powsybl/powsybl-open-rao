/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new InstantImpl("initial", InstantKind.PREVENTIVE, null));
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

    @Test
    void testInstantBeforeAfter() {
        InstantImpl preventiveInstant = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        InstantImpl outageInstant = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, preventiveInstant);
        InstantImpl autoInstant = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, outageInstant);
        InstantImpl curativeInstant = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, autoInstant);

        // Comes before
        assertFalse(preventiveInstant.comesBefore(preventiveInstant));
        assertTrue(preventiveInstant.comesBefore(outageInstant));
        assertTrue(preventiveInstant.comesBefore(autoInstant));
        assertTrue(preventiveInstant.comesBefore(curativeInstant));

        assertFalse(outageInstant.comesBefore(outageInstant));
        assertTrue(outageInstant.comesBefore(autoInstant));
        assertTrue(outageInstant.comesBefore(curativeInstant));

        assertFalse(autoInstant.comesBefore(autoInstant));
        assertTrue(autoInstant.comesBefore(curativeInstant));

        assertFalse(curativeInstant.comesBefore(curativeInstant));

        // Comes after
        assertFalse(curativeInstant.comesAfter(curativeInstant));
        assertTrue(curativeInstant.comesAfter(autoInstant));
        assertTrue(curativeInstant.comesAfter(outageInstant));
        assertTrue(curativeInstant.comesAfter(preventiveInstant));

        assertFalse(autoInstant.comesAfter(autoInstant));
        assertTrue(autoInstant.comesAfter(outageInstant));
        assertTrue(autoInstant.comesAfter(preventiveInstant));

        assertFalse(outageInstant.comesAfter(outageInstant));
        assertTrue(outageInstant.comesAfter(preventiveInstant));

        assertFalse(preventiveInstant.comesAfter(preventiveInstant));
    }

    @Test
    void testMin() {
        InstantImpl instant1 = new InstantImpl("instant1", InstantKind.PREVENTIVE, null);
        InstantImpl instant2 = new InstantImpl("instant2", InstantKind.CURATIVE, instant1);
        assertEquals(instant1, Instant.min(instant1, instant2));
        assertNull(Instant.min(instant1, null));
        assertNull(Instant.min(null, instant2));
        assertNull(Instant.min(null, null));
    }
}
