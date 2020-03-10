/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultTest {
    private static final double EPSILON = 0.01;

    private RangeActionResult rangeActionResult;
    private Set<State> states;
    private State initialState;
    private State outage1;
    private State curative1;
    private State outage2;
    private State curative2;

    @Before
    public void setUp() {
        states = new HashSet<>();
        initialState = new SimpleState(Optional.empty(), new Instant("initial", 0));
        outage1 = new SimpleState(Optional.of(new ComplexContingency("co1")), new Instant("after-co1", 10));
        curative1 = new SimpleState(Optional.of(new ComplexContingency("co1")), new Instant("curative-co1", 50));
        outage2 = new SimpleState(Optional.of(new ComplexContingency("co2")), new Instant("after-co2", 10));
        curative2 = new SimpleState(Optional.of(new ComplexContingency("co2")), new Instant("curative-co2", 50));
        states.add(initialState);
        states.add(outage1);
        states.add(curative1);
        states.add(outage2);
        states.add(curative2);
        rangeActionResult = new RangeActionResult(states);
    }

    @Test
    public void constructor() {
        assertTrue(rangeActionResult.setPointPerStates.containsKey(initialState));
        assertTrue(rangeActionResult.setPointPerStates.containsKey(outage1));
        assertTrue(rangeActionResult.setPointPerStates.containsKey(curative1));
        assertTrue(rangeActionResult.setPointPerStates.containsKey(outage2));
        assertTrue(rangeActionResult.setPointPerStates.containsKey(curative2));
        assertEquals(5, rangeActionResult.setPointPerStates.size());
    }

    @Test
    public void getSetPoint() {
        rangeActionResult.setSetPoint(outage1, 15.);
        assertEquals(Double.NaN, rangeActionResult.getSetPoint(initialState), EPSILON);
        assertEquals(15., rangeActionResult.getSetPoint(outage1), EPSILON);
    }
}
