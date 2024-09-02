/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracapi;

import com.powsybl.commons.extensions.Extension;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class InstantTest {
    static class InstantImplTest implements Instant {

        private final int order;
        private final InstantKind instantKind;

        public InstantImplTest(int order, InstantKind instantKind) {
            this.order = order;
            this.instantKind = instantKind;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public InstantKind getKind() {
            return instantKind;
        }

        @Override
        public <E extends Extension<Instant>> void addExtension(Class<? super E> aClass, E e) {
            //not used
        }

        @Override
        public <E extends Extension<Instant>> E getExtension(Class<? super E> aClass) {
            return null;
        }

        @Override
        public <E extends Extension<Instant>> E getExtensionByName(String s) {
            return null;
        }

        @Override
        public <E extends Extension<Instant>> boolean removeExtension(Class<E> aClass) {
            return false;
        }

        @Override
        public <E extends Extension<Instant>> Collection<E> getExtensions() {
            return null;
        }
    }

    private final Instant preventiveInstant = new InstantImplTest(1, InstantKind.PREVENTIVE);
    private final Instant outageInstant = new InstantImplTest(2, InstantKind.OUTAGE);
    private final Instant autoInstant = new InstantImplTest(3, InstantKind.AUTO);
    private final Instant curativeInstant = new InstantImplTest(4, InstantKind.CURATIVE);

    @Test
    void testIsPreventive() {
        assertTrue(preventiveInstant.isPreventive());
        assertFalse(outageInstant.isPreventive());
        assertFalse(autoInstant.isPreventive());
        assertFalse(curativeInstant.isPreventive());
    }

    @Test
    void testIsOutage() {
        assertFalse(preventiveInstant.isOutage());
        assertTrue(outageInstant.isOutage());
        assertFalse(autoInstant.isOutage());
        assertFalse(curativeInstant.isOutage());
    }

    @Test
    void testIsAuto() {
        assertFalse(preventiveInstant.isAuto());
        assertFalse(outageInstant.isAuto());
        assertTrue(autoInstant.isAuto());
        assertFalse(curativeInstant.isAuto());
    }

    @Test
    void testIsCurative() {
        assertFalse(preventiveInstant.isCurative());
        assertFalse(outageInstant.isCurative());
        assertFalse(autoInstant.isCurative());
        assertTrue(curativeInstant.isCurative());
    }

    @Test
    void testComesBefore() {
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
    }

    @Test
    void testComesAfter() {
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
    void testCompareInstants() {
        assertEquals(0, preventiveInstant.compareTo(preventiveInstant));
        assertEquals(0, curativeInstant.compareTo(curativeInstant));
        assertEquals(-1, preventiveInstant.compareTo(curativeInstant));
        assertEquals(1, curativeInstant.compareTo(preventiveInstant));
    }

    @Test
    void testMin() {
        assertNull(Instant.min(null, null));
        assertNull(Instant.min(preventiveInstant, null));
        assertNull(Instant.min(null, curativeInstant));
        assertEquals(preventiveInstant, Instant.min(preventiveInstant, curativeInstant));
    }
}
