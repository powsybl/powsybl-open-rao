/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import org.junit.Test;

import java.util.Optional;

import static com.farao_community.farao.commons.Unit.TAP;
import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.crac_api.Instant.*;
import static com.farao_community.farao.data.rao_result_api.ComputationStatus.*;
import static com.farao_community.farao.data.rao_result_api.OptimizationState.*;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoResultJsonConstantsTest {

    @Test
    public void testSerializeUnit() {
        assertEquals("ampere", serializeUnit(AMPERE));
        assertEquals("degree", serializeUnit(DEGREE));
        assertEquals("megawatt", serializeUnit(MEGAWATT));
        assertEquals("kilovolt", serializeUnit(KILOVOLT));
        assertEquals("percent_imax", serializeUnit(PERCENT_IMAX));
        assertEquals("tap", serializeUnit(TAP));
    }

    @Test
    public void testDeserializeUnit() {
        assertEquals(AMPERE, deserializeUnit("ampere"));
        assertEquals(DEGREE, deserializeUnit("degree"));
        assertEquals(MEGAWATT, deserializeUnit("megawatt"));
        assertEquals(KILOVOLT, deserializeUnit("kilovolt"));
        assertEquals(PERCENT_IMAX, deserializeUnit("percent_imax"));
        assertEquals(TAP, deserializeUnit("tap"));
    }

    @Test
    public void testSerializeInstant() {
        assertEquals("preventive", serializeInstant(PREVENTIVE));
        assertEquals("outage", serializeInstant(OUTAGE));
        assertEquals("auto", serializeInstant(AUTO));
        assertEquals("curative", serializeInstant(CURATIVE));
    }

    @Test
    public void testDeserializeInstant() {
        assertEquals(PREVENTIVE, deserializeInstant("preventive"));
        assertEquals(OUTAGE, deserializeInstant("outage"));
        assertEquals(AUTO, deserializeInstant("auto"));
        assertEquals(CURATIVE, deserializeInstant("curative"));
    }

    @Test
    public void testSerializeOptimizationState() {
        assertEquals("initial", serializeOptimizationState(INITIAL));
        assertEquals("afterPRA", serializeOptimizationState(AFTER_PRA));
        assertEquals("afterARA", serializeOptimizationState(AFTER_ARA));
        assertEquals("afterCRA", serializeOptimizationState(AFTER_CRA));
    }

    @Test
    public void testDeserializeOptimizationState() {
        assertEquals(INITIAL, deserializeOptimizationState("initial"));
        assertEquals(AFTER_PRA, deserializeOptimizationState("afterPRA"));
        assertEquals(AFTER_ARA, deserializeOptimizationState("afterARA"));
        assertEquals(AFTER_CRA, deserializeOptimizationState("afterCRA"));
    }

    @Test
    public void testSerializeStatus() {
        assertEquals("default", serializeStatus(DEFAULT));
        assertEquals("fallback", serializeStatus(FALLBACK));
        assertEquals("failure", serializeStatus(FAILURE));
    }

    @Test
    public void testDeserializeStatus() {
        assertEquals(DEFAULT, deserializeStatus("default"));
        assertEquals(FALLBACK, deserializeStatus("fallback"));
        assertEquals(FAILURE, deserializeStatus("failure"));
    }

    @Test
    public void testCompareStates() {
        State state1 = mock(State.class);
        State state2 = mock(State.class);

        when(state1.getInstant()).thenReturn(OUTAGE);
        when(state2.getInstant()).thenReturn(AUTO);
        assertEquals(-1, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(1, STATE_COMPARATOR.compare(state2, state1));

        when(state1.getInstant()).thenReturn(PREVENTIVE);
        when(state2.getInstant()).thenReturn(PREVENTIVE);
        assertEquals(0, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(0, STATE_COMPARATOR.compare(state2, state1));

        when(state1.getInstant()).thenReturn(CURATIVE);
        Contingency co1 = mock(Contingency.class);
        when(co1.getId()).thenReturn("bbb");
        when(state1.getContingency()).thenReturn(Optional.of(co1));
        when(state2.getInstant()).thenReturn(CURATIVE);
        Contingency co2 = mock(Contingency.class);
        when(co2.getId()).thenReturn("aaa");
        when(state2.getContingency()).thenReturn(Optional.of(co2));
        assertEquals(1, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(-1, STATE_COMPARATOR.compare(state2, state1));
    }
}
