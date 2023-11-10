/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.InstantImpl;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.farao_community.farao.commons.Unit.TAP;
import static com.farao_community.farao.commons.Unit.*;
import static com.farao_community.farao.data.rao_result_api.ComputationStatus.DEFAULT;
import static com.farao_community.farao.data.rao_result_api.ComputationStatus.FAILURE;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RaoResultJsonConstantsTest {

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
        InstantImpl prevInstant = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
        InstantImpl outageInstant = new InstantImpl("outage", InstantKind.OUTAGE, prevInstant);
        InstantImpl autoInstant = new InstantImpl("auto", InstantKind.AUTO, outageInstant);
        InstantImpl curativeInstant = new InstantImpl("curative", InstantKind.CURATIVE, autoInstant);
        assertEquals("preventive", serializeInstantId(prevInstant));
        assertEquals("outage", serializeInstantId(outageInstant));
        assertEquals("auto", serializeInstantId(autoInstant));
        assertEquals("curative", serializeInstantId(curativeInstant));
    }

    @Test
    void testDeserializeInstant() {
        // TODO remove this test ?
        assertNull(deserializeInstantId("initial"));
        assertEquals("preventive", deserializeInstantId("preventive"));
        assertEquals("outage", deserializeInstantId("outage"));
        assertEquals("auto", deserializeInstantId("auto"));
        assertEquals("curative", deserializeInstantId("curative"));
    }

    @Test
    void testDeserializeOptimizedInstant() {
        // TODO remove this test ?
        assertNull(deserializeOptimizedInstantId("initial", "1.4"));
        assertEquals("preventive", deserializeOptimizedInstantId("preventive", "1.4"));
        assertEquals("outage", deserializeOptimizedInstantId("outage", "1.4"));
        assertEquals("auto", deserializeOptimizedInstantId("auto", "1.4"));
        assertEquals("curative", deserializeOptimizedInstantId("curative", "1.4"));
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
        Instant prevInstant = Mockito.mock(Instant.class);
        Instant outageInstant = Mockito.mock(Instant.class);
        Instant autoInstant = Mockito.mock(Instant.class);
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(prevInstant.getOrder()).thenReturn(0);
        when(outageInstant.getOrder()).thenReturn(1);
        when(autoInstant.getOrder()).thenReturn(2);
        when(curativeInstant.getOrder()).thenReturn(3);
        when(prevInstant.getInstantKind()).thenReturn(InstantKind.PREVENTIVE);
        when(outageInstant.getInstantKind()).thenReturn(InstantKind.OUTAGE);
        when(autoInstant.getInstantKind()).thenReturn(InstantKind.AUTO);
        when(curativeInstant.getInstantKind()).thenReturn(InstantKind.CURATIVE);

        when(state1.getInstant()).thenReturn(outageInstant);
        when(state2.getInstant()).thenReturn(autoInstant);
        assertEquals(-1, STATE_COMPARATOR.compare(state1, state2));
        assertEquals(1, STATE_COMPARATOR.compare(state2, state1));

        when(state1.getInstant()).thenReturn(prevInstant);
        when(state2.getInstant()).thenReturn(prevInstant);
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

    @Test
    void testSerializeOptimizedStepsExecuted() {
        assertEquals("The RAO only went through first preventive", serializeOptimizedStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("First preventive fellback to initial situation", serializeOptimizedStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("Second preventive improved first preventive results", serializeOptimizedStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST));
        assertEquals("Second preventive fellback to initial situation", serializeOptimizedStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION));
        assertEquals("Second preventive fellback to first preventive results", serializeOptimizedStepsExecuted(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION));
    }

    @Test
    void testDeserializeOptimizedStepsExecuted() {
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, deserializeOptimizedStepsExecuted("The RAO only went through first preventive"));
        assertEquals(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, deserializeOptimizedStepsExecuted("First preventive fellback to initial situation"));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST, deserializeOptimizedStepsExecuted("Second preventive improved first preventive results"));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION, deserializeOptimizedStepsExecuted("Second preventive fellback to initial situation"));
        assertEquals(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION, deserializeOptimizedStepsExecuted("Second preventive fellback to first preventive results"));
    }
}
