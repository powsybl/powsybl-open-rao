/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class NetworkElementTest {

    @Test
    public void testConstructorElementTest() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        assertEquals("basicElemId", networkElement.getId());
        assertEquals("basicElemName", networkElement.getName());
        assertEquals("basicElemId", networkElement.toString());
    }

    @Test
    public void testDifferent() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-2", "name-2");

        assertNotEquals(networkElement1, networkElement2);
    }

    @Test
    public void testEqualWithDifferentNames() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-1", "name-2");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    public void testEqualWithSameNames() {
        NetworkElement networkElement1 = new NetworkElement("network-element-1", "name-1");
        NetworkElement networkElement2 = new NetworkElement("network-element-1", "name-1");

        assertEquals(networkElement1, networkElement2);
    }

    @Test
    public void testSimpleConstructor() {
        NetworkElement networkElement = new NetworkElement("network-element");

        assertEquals("network-element", networkElement.getId());
        assertEquals("network-element", networkElement.getName());
    }

    @Test
    public void testHashCode() {
        NetworkElement networkElement = new NetworkElement("network-element");

        assertEquals("network-element".hashCode(), networkElement.getId().hashCode());
    }
}
