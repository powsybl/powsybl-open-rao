/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class StateIdHelperTest {
    private Contingency contingency;
    private Instant instant;
    private OffsetDateTime timestamp;

    @BeforeEach
    void setUp() {
        contingency = Mockito.mock(Contingency.class);
        Mockito.when(contingency.getId()).thenReturn("contingency");
        instant = Mockito.mock(Instant.class);
        Mockito.when(instant.getId()).thenReturn("instant");
        timestamp = OffsetDateTime.of(2025, 2, 12, 9, 18, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void testIdWithContingencyAndTimestamp() {
        assertEquals("contingency - instant - 202502120918", StateIdHelper.getStateId(contingency, instant, timestamp));
    }

    @Test
    void testIdWithContingency() {
        assertEquals("contingency - instant", StateIdHelper.getStateId(contingency, instant, null));
    }

    @Test
    void testIdWithTimestamp() {
        assertEquals("instant - 202502120918", StateIdHelper.getStateId(instant, timestamp));
    }

    @Test
    void testBasicId() {
        assertEquals("instant", StateIdHelper.getStateId(instant, null));
    }
}
