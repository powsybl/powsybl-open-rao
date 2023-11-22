/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class PostContingencyStateTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private static final Instant INSTANT_PREV = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl(CURATIVE_INSTANT_ID, InstantKind.CURATIVE, INSTANT_AUTO);
    private Contingency contingency1;
    private Contingency contingency2;

    @BeforeEach
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
    void testEquals() {
        PostContingencyState state1 = new PostContingencyState(contingency1, INSTANT_OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, INSTANT_OUTAGE);

        assertEquals(state1, state2);
    }

    @Test
    void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(contingency1, INSTANT_OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, INSTANT_CURATIVE);

        assertNotEquals(state1, state2);
    }

    @Test
    void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, INSTANT_CURATIVE);
        PostContingencyState state2 = new PostContingencyState(contingency2, INSTANT_CURATIVE);

        assertNotEquals(state1, state2);
    }

    @Test
    void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, INSTANT_OUTAGE);
        assertEquals("contingency1 - outage", state1.toString());
    }

    @Test
    void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(contingency1, INSTANT_OUTAGE);
        PostContingencyState state2 = new PostContingencyState(contingency1, INSTANT_CURATIVE);

        assertTrue(state2.compareTo(state1) > 0);
    }
}
