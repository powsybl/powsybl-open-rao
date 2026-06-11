/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class VersionTest {

    @Test
    void testParseValid() {
        Version v = Version.parse("1.0");
        assertEquals(1, v.major());
        assertEquals(0, v.minor());

        v = Version.parse("10.25");
        assertEquals(10, v.major());
        assertEquals(25, v.minor());
    }

    @Test
    void testParseInvalid() {
        assertThrows(OpenRaoException.class, () -> Version.parse("0.1"));
        assertThrows(OpenRaoException.class, () -> Version.parse("1."));
        assertThrows(OpenRaoException.class, () -> Version.parse(".1"));
        assertThrows(OpenRaoException.class, () -> Version.parse("1.2.3"));
        assertThrows(OpenRaoException.class, () -> Version.parse("a.b"));
        assertThrows(OpenRaoException.class, () -> Version.parse(null));
    }

    @Test
    void testToString() {
        assertEquals("1.0", new Version(1, 0).toString());
        assertEquals("10.25", new Version(10, 25).toString());
    }

    @Test
    void testCompareTo() {
        Version v1 = new Version(1, 0);
        Version v2 = new Version(1, 1);
        Version v3 = new Version(2, 0);

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);
        assertTrue(v1.compareTo(v3) < 0);
        assertTrue(v3.compareTo(v1) > 0);
        assertEquals(0, v1.compareTo(new Version(1, 0)));
        assertEquals(new Version(1, 0), v1);
    }
}
