/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PostContingencyStateTest {

    private Contingency contingency1;
    private Contingency contingency2;

    @Before
    public void create() {
        Crac crac = new CracImplFactory().create("cracId");
        contingency1 = crac.newContingency()
            .withId("contingency1")
            .withNetworkElement("anyNetworkElement")
            .add();
        contingency2 = crac.newContingency()
            .withId("contingency2")
            .withNetworkElement("anyNetworkElement")
            .add();
    }

    @Test
    public void testEquals() {
        PostContingencyState state1 = new PostContingencyState(contingency1, Instant.OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, Instant.OUTAGE);

        assertEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(contingency1, Instant.OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, Instant.CURATIVE);

        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, Instant.CURATIVE);
        PostContingencyState state2 = new PostContingencyState(contingency2, Instant.CURATIVE);

        assertNotEquals(state1, state2);
    }

    @Test
    public void testHashCode() {
        State state = new PostContingencyState(contingency1, Instant.CURATIVE);
        assertEquals("contingency1curative".hashCode(), state.hashCode());
    }

    @Test
    public void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, Instant.OUTAGE);
        assertEquals("contingency1 - outage", state1.toString());
    }

    @Test
    public void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(contingency1, Instant.OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, Instant.CURATIVE);

        assertTrue(state2.compareTo(state1) > 0);
    }
}
