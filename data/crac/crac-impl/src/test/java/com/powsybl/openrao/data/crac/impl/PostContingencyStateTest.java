/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

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
            .withContingencyElement("anyNetworkElement", ContingencyElementType.LINE)
            .add();
        contingency2 = crac.newContingency()
            .withId("contingency2")
            .withContingencyElement("anyNetworkElement", ContingencyElementType.LINE)
            .add();
    }

    @Test
    void testEquals() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant, null);
        PostContingencyState state2 = new PostContingencyState(contingency1, outageInstant, null);

        assertEquals(state1, state2);
    }

    @Test
    void testNotEqualsByInstant() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant, null);
        PostContingencyState state2 = new PostContingencyState(contingency1, curativeInstant, null);

        assertNotEquals(state1, state2);
    }

    @Test
    void testNotEqualsByContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, curativeInstant, null);
        PostContingencyState state2 = new PostContingencyState(contingency2, curativeInstant, null);

        assertNotEquals(state1, state2);
    }

    @Test
    void testToStringAfterContingency() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant, null);
        assertEquals("contingency1 - outage", state1.toString());
    }

    @Test
    void testCompareTo() {
        PostContingencyState state1 = new PostContingencyState(contingency1, outageInstant, null);
        PostContingencyState state2 = new PostContingencyState(contingency1, curativeInstant, null);

        assertTrue(state2.compareTo(state1) > 0);

        PostContingencyState state3 = new PostContingencyState(contingency1, outageInstant, OffsetDateTime.of(2025, 2, 3, 9, 49, 0, 0, ZoneOffset.UTC));
        PostContingencyState state4 = new PostContingencyState(contingency1, outageInstant, OffsetDateTime.of(2025, 2, 4, 9, 49, 0, 0, ZoneOffset.UTC));

        assertTrue(state4.compareTo(state3) > 0);

        PostContingencyState state5 = new PostContingencyState(contingency1, curativeInstant, OffsetDateTime.of(2025, 2, 3, 9, 49, 0, 0, ZoneOffset.UTC));

        assertTrue(state5.compareTo(state3) > 0);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> state4.compareTo(state1));
        assertEquals("Cannot compare states with no timestamp", exception.getMessage());
    }

    @Test
    void testCannotCreatePostContingencyStateWithPreventiveInstant() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> new PostContingencyState(contingency1, preventiveInstant, null));
        assertEquals("Instant cannot be preventive", exception.getMessage());
    }

    @Test
    void testCurativeStateWithTimestamp() {
        PostContingencyState state = new PostContingencyState(contingency1, curativeInstant, OffsetDateTime.of(2025, 1, 24, 17, 30, 0, 0, ZoneOffset.UTC));
        Optional<OffsetDateTime> timestamp = state.getTimestamp();
        assertTrue(timestamp.isPresent());
        assertEquals(OffsetDateTime.of(2025, 1, 24, 17, 30, 0, 0, ZoneOffset.UTC), timestamp.get());
        assertEquals("contingency1 - curative - 202501241730", state.getId());
    }
}
