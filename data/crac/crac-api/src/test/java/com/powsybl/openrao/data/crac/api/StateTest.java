/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class StateTest {
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 12, 14, 48, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 12, 15, 48, 0, 0, ZoneOffset.UTC);
    private Instant preventiveInstant;
    private Instant curativeInstant;
    private Contingency contingency;

    @BeforeEach
    void setUp() {
        preventiveInstant = Mockito.mock(Instant.class);
        Mockito.when(preventiveInstant.getKind()).thenReturn(InstantKind.PREVENTIVE);
        Mockito.when(preventiveInstant.getOrder()).thenReturn(0);
        curativeInstant = Mockito.mock(Instant.class);
        Mockito.when(curativeInstant.getKind()).thenReturn(InstantKind.CURATIVE);
        Mockito.when(curativeInstant.getOrder()).thenReturn(2);
        contingency = Mockito.mock(Contingency.class);
    }

    @Test
    void testIsPreventive() {
        assertTrue(new MockState(preventiveInstant, null, null).isPreventive());
        assertFalse(new MockState(curativeInstant, contingency, null).isPreventive());
    }

    @Test
    void testCompareTo() {
        // no timestamp
        State state1 = new MockState(preventiveInstant, null, null);
        State state2 = new MockState(curativeInstant, contingency, null);
        assertTrue(state1.compareTo(state2) < 0);

        // same timestamps
        State state3 = new MockState(preventiveInstant, null, timestamp1);
        State state4 = new MockState(curativeInstant, contingency, timestamp1);
        assertTrue(state3.compareTo(state4) < 0);

        // different timestamps
        State state5 = new MockState(preventiveInstant, null, timestamp1);
        State state6 = new MockState(preventiveInstant, null, timestamp2);
        assertTrue(state5.compareTo(state6) < 0);

        // illegal case
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> state1.compareTo(state6));
        assertEquals("Cannot compare states with no timestamp", exception.getMessage());
    }
}
