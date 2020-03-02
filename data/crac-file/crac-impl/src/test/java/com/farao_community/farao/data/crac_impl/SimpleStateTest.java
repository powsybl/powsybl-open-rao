/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class SimpleStateTest {
    private SimpleState state;

    @Before
    public void create() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        state =  new SimpleState(
            Optional.of(new ComplexContingency("contingencyId", "contingencyName", Collections.singleton(networkElement))),
            new Instant("curative", 12)
        );
    }

    @Test
    public void getInstant() {
        assertEquals(12, state.getInstant().getSeconds());
    }

    @Test
    public void testEquals() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        assertEquals(state1, state2);
    }

    @Test
    public void testEqualsForPreventive() {
        SimpleState state1 = new SimpleState(
            Optional.empty(),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.empty(),
            new Instant("instant-1", 10)
        );

        assertEquals(state1, state2);
    }

    @Test
    public void testDifferentPreventiveAndAfterContingency() {
        SimpleState state1 = new SimpleState(
            Optional.empty(),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByInstant() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-2", 10)
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByContingency() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 2", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByContingencyElements() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("instant-1", 10)
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testHashCode() {
        assertEquals("contingencyIdcurative".hashCode(), state.hashCode());
    }

    @Test
    public void testHashCodeForPreventive() {
        SimpleState state1 = new SimpleState(
            Optional.empty(),
            new Instant("instant-1", 10)
        );

        assertEquals("preventiveinstant-1".hashCode(), state1.hashCode());
    }

    @Test
    public void testToStringForPreventive() {
        SimpleState state1 = new SimpleState(
            Optional.empty(),
            new Instant("instant-1", 10)
        );

        assertEquals("instant-1 - preventive", state1.toString());
    }

    @Test
    public void testToStringAfterContingency() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        assertEquals("instant-1 - contingency 1", state1.toString());
    }

    @Test
    public void testCompareTo() {
        SimpleState state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-1", 10)
        );

        SimpleState state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1")))),
            new Instant("instant-2", 15)
        );

        assertTrue(state2.compareTo(state1) > 0);
    }
}
