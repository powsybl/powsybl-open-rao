/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PostContingencyStateTest {

    private Contingency contingency1;
    private Contingency contingency2;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant curativeInstant;

    @BeforeEach
    public void create() {
        Crac crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant("preventive");
        outageInstant = crac.getInstant("outage");
        curativeInstant = crac.getInstant("curative");
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
    void testEquals() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant);
        PostContingencyState state2 = new PostContingencyState(contingency1, outageInstant);

        assertEquals(state1, state2);
    }

    @Test
    void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant);
        PostContingencyState state2 = new PostContingencyState(contingency1, curativeInstant);

        assertNotEquals(state1, state2);
    }

    @Test
    void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, curativeInstant);
        PostContingencyState state2 = new PostContingencyState(contingency2, curativeInstant);

        assertNotEquals(state1, state2);
    }

    @Test
    void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant);
        assertEquals("contingency1 - outage", state1.toString());
    }

    @Test
    void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant);
        PostContingencyState state2 = new PostContingencyState(contingency1, curativeInstant);

        assertTrue(state2.compareTo(state1) > 0);
    }

    @Test
    void testCannotCreatePostContingencyStateWithPreventiveInstant() {
        FaraoException exception = assertThrows(FaraoException.class, () -> new PostContingencyState(contingency1, preventiveInstant));
        assertEquals("Instant cannot be preventive", exception.getMessage());
    }
}
