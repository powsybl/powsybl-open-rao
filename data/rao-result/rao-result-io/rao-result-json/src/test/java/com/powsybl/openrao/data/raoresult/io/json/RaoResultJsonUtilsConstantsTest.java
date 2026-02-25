/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.powsybl.openrao.commons.Unit.*;
import static com.powsybl.openrao.commons.Unit.TAP;
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.DEFAULT;
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.FAILURE;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultJsonUtilsConstantsTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void testSerializeUnit() {
        assertEquals("ampere", serializeUnit(AMPERE));
        assertEquals("degree", serializeUnit(DEGREE));
        assertEquals("megawatt", serializeUnit(MEGAWATT));
        assertEquals("kilovolt", serializeUnit(KILOVOLT));
        assertEquals("percent_imax", serializeUnit(PERCENT_IMAX));
        assertEquals("tap", serializeUnit(TAP));
    }

    @Test
    void testDeserializeUnit() {
        assertEquals(AMPERE, deserializeUnit("ampere"));
        assertEquals(DEGREE, deserializeUnit("degree"));
        assertEquals(MEGAWATT, deserializeUnit("megawatt"));
        assertEquals(KILOVOLT, deserializeUnit("kilovolt"));
        assertEquals(PERCENT_IMAX, deserializeUnit("percent_imax"));
        assertEquals(TAP, deserializeUnit("tap"));
    }

    @Test
    void testSerializeInstantId() {
        assertEquals("initial", serializeInstantId(null));
        Instant preventiveInstant = mock(Instant.class);
        Instant outageInstant = mock(Instant.class);
        Instant autoInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        when(preventiveInstant.getId()).thenReturn(PREVENTIVE_INSTANT_ID);
        when(outageInstant.getId()).thenReturn(OUTAGE_INSTANT_ID);
        when(autoInstant.getId()).thenReturn(AUTO_INSTANT_ID);
        when(curativeInstant.getId()).thenReturn(CURATIVE_INSTANT_ID);
        assertEquals(PREVENTIVE_INSTANT_ID, serializeInstantId(preventiveInstant));
        assertEquals(OUTAGE_INSTANT_ID, serializeInstantId(outageInstant));
        assertEquals(AUTO_INSTANT_ID, serializeInstantId(autoInstant));
        assertEquals(CURATIVE_INSTANT_ID, serializeInstantId(curativeInstant));
    }

    @Test
    void testDeserializeOptimizedInstant() {
        Crac crac = mock(Crac.class);
        assertEquals(INITIAL_INSTANT_ID, deserializeOptimizedInstantId(INITIAL_INSTANT_ID, "1.4", crac));
        assertEquals(PREVENTIVE_INSTANT_ID, deserializeOptimizedInstantId(PREVENTIVE_INSTANT_ID, "1.4", crac));
        assertEquals(OUTAGE_INSTANT_ID, deserializeOptimizedInstantId(OUTAGE_INSTANT_ID, "1.4", crac));
        assertEquals(AUTO_INSTANT_ID, deserializeOptimizedInstantId(AUTO_INSTANT_ID, "1.4", crac));
        assertEquals(CURATIVE_INSTANT_ID, deserializeOptimizedInstantId(CURATIVE_INSTANT_ID, "1.4", crac));
        Instant preventiveInstant = mock(Instant.class);
        Instant outageInstant = mock(Instant.class);
        Instant autoInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        when(crac.getInstant(PREVENTIVE_INSTANT_ID)).thenReturn(preventiveInstant);
        when(crac.getInstant(OUTAGE_INSTANT_ID)).thenReturn(outageInstant);
        when(crac.getInstant(AUTO_INSTANT_ID)).thenReturn(autoInstant);
        when(crac.getInstant(CURATIVE_INSTANT_ID)).thenReturn(curativeInstant);
        assertNull(deserializeOptimizedInstant(INITIAL_INSTANT_ID, "1.4", crac));
        assertEquals(preventiveInstant, deserializeOptimizedInstant(PREVENTIVE_INSTANT_ID, "1.4", crac));
        assertEquals(outageInstant, deserializeOptimizedInstant(OUTAGE_INSTANT_ID, "1.4", crac));
        assertEquals(autoInstant, deserializeOptimizedInstant(AUTO_INSTANT_ID, "1.4", crac));
        assertEquals(curativeInstant, deserializeOptimizedInstant(CURATIVE_INSTANT_ID, "1.4", crac));
    }

    @Test
    void testSerializeStatus() {
        assertEquals("default", serializeStatus(DEFAULT));
        assertEquals("failure", serializeStatus(FAILURE));
    }

    @Test
    void testDeserializeStatus() {
        assertEquals(DEFAULT, deserializeStatus("default"));
        assertEquals(FAILURE, deserializeStatus("failure"));
    }

    @Test
    void testCompareStates() {
        State state1 = Mockito.spy(State.class);
        State state2 = Mockito.spy(State.class);
        Instant preventiveInstant = mock(Instant.class);
        Instant outageInstant = mock(Instant.class);
        Instant autoInstant = mock(Instant.class);
        Instant curativeInstant = mock(Instant.class);
        when(preventiveInstant.isPreventive()).thenReturn(true);
        when(outageInstant.getOrder()).thenReturn(1);
        when(autoInstant.getOrder()).thenReturn(2);

        when(state1.getInstant()).thenReturn(outageInstant);
        when(state2.getInstant()).thenReturn(autoInstant);
        assertEquals(-1, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(1, STATE_COMPARATOR.compare(state2, state1));

        when(state1.getInstant()).thenReturn(preventiveInstant);
        when(state2.getInstant()).thenReturn(preventiveInstant);
        assertEquals(0, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(0, STATE_COMPARATOR.compare(state2, state1));

        when(state1.getInstant()).thenReturn(curativeInstant);
        Contingency co1 = mock(Contingency.class);
        when(co1.getId()).thenReturn("bbb");
        when(state1.getContingency()).thenReturn(Optional.of(co1));
        when(state2.getInstant()).thenReturn(curativeInstant);
        Contingency co2 = mock(Contingency.class);
        when(co2.getId()).thenReturn("aaa");
        when(state2.getContingency()).thenReturn(Optional.of(co2));
        assertEquals(1, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(-1, STATE_COMPARATOR.compare(state2, state1));
    }
}
