/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
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

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PostContingencyStateTest {
    private PostContingencyState state;

    @Before
    public void create() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        state =  new PostContingencyState(
            new ComplexContingency("contingencyId", "contingencyName", Collections.singleton(networkElement)),
            Instant.CURATIVE
        );
    }

    @Test
    public void testEquals() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.OUTAGE
        );

        PostContingencyState state2 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.OUTAGE
        );

        assertEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.OUTAGE
        );

        PostContingencyState state2 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.CURATIVE
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.PREVENTIVE
        );

        PostContingencyState state2 = new PostContingencyState(
            new ComplexContingency("contingency 2", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.PREVENTIVE
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByContingencyElements() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.PREVENTIVE
        );

        PostContingencyState state2 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-2"))),
            Instant.PREVENTIVE
        );

        assertNotEquals(state1, state2);
    }

    @Test
    public void testHashCode() {
        assertEquals("contingencyIdcurative".hashCode(), state.hashCode());
    }

    @Test
    public void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.OUTAGE
        );

        assertEquals("contingency 1 - outage", state1.toString());
    }

    @Test
    public void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.OUTAGE
        );

        PostContingencyState state2 = new PostContingencyState(
            new ComplexContingency("contingency 1", Collections.singleton(new NetworkElement("network-element-1"))),
            Instant.CURATIVE
        );

        assertTrue(state2.compareTo(state1) > 0);
    }
}
