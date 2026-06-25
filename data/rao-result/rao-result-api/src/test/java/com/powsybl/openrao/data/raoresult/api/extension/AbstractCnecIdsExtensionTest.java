/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class AbstractCnecIdsExtensionTest {

    private static class TestExtension extends AbstractCnecIdsExtension {
        @Override
        public String getName() {
            return "test";
        }

        public TestExtension(Set<String> ids) {
            super(ids);
        }

        public TestExtension() {
            super();
        }
    }

    @Test
    void testConstructorWithIds() {
        Set<String> ids = Set.of("id1", "id2");
        TestExtension extension = new TestExtension(ids);
        assertEquals(ids, extension.getCriticalCnecIds());
    }

    @Test
    void testDefaultConstructor() {
        TestExtension extension = new TestExtension();
        assertTrue(extension.getCriticalCnecIds().isEmpty());
    }

    @Test
    void testSetCriticalCnecIds() {
        TestExtension extension = new TestExtension();
        Set<String> ids = Set.of("id1", "id2");
        extension.setCriticalCnecIds(ids);
        assertEquals(ids, extension.getCriticalCnecIds());
    }

    @Test
    void testCriticalCnecsResult() {
        Set<String> ids = Set.of("id1");
        CriticalCnecsResult extension = new CriticalCnecsResult(ids);
        assertEquals("critical-cnecs-result", extension.getName());
        assertEquals(ids, extension.getCriticalCnecIds());
    }

    @Test
    void testSetNullCriticalCnecIds() {
        TestExtension extension = new TestExtension();
        extension.setCriticalCnecIds(null);
        assertNull(extension.getCriticalCnecIds());
    }

    @Test
    void testConstructorWithNullIds() {
        TestExtension extension = new TestExtension(null);
        assertNull(extension.getCriticalCnecIds());
    }
}
