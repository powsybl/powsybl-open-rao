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

    private static final Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
    private static final Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
    private static final Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);
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
        PostContingencyState state1 = new PostContingencyState(contingency1, instantOutage);
        PostContingencyState state2 = new PostContingencyState(contingency1, instantOutage);

        assertEquals(state1, state2);
    }

    @Test
    void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(contingency1, instantOutage);
        PostContingencyState state2 = new PostContingencyState(contingency1, instantCurative);

        assertNotEquals(state1, state2);
    }

    @Test
    void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, instantCurative);
        PostContingencyState state2 = new PostContingencyState(contingency2, instantCurative);

        assertNotEquals(state1, state2);
    }

    @Test
    void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, instantOutage);
        assertEquals("contingency1 - outage", state1.toString());
    }

    @Test
    void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(contingency1, instantOutage);
        PostContingencyState state2 = new PostContingencyState(contingency1, instantCurative);

        assertTrue(state2.compareTo(state1) > 0);
    }
}
