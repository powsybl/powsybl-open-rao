/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.IdentifiableType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class BalancingRangeActionTest extends AbstractTest {
    private BalancingRangeAction parameters;

    @BeforeEach
    void setUp() {
        parameters = new NetworkCracCreationParameters(null, List.of("cur")).getBalancingRangeAction();
    }

    @Test
    void testPredicate() {
        assertTrue(parameters.shouldIncludeInjection(generator, prevInstant));
        assertTrue(parameters.shouldIncludeInjection(generator, cur1Instant));
        assertTrue(parameters.shouldIncludeInjection(load, prevInstant));
        assertTrue(parameters.shouldIncludeInjection(load, cur1Instant));

        parameters.setInjectionPredicate((injection, instant) -> instant.isPreventive() || injection.getType() == IdentifiableType.LOAD);

        assertTrue(parameters.shouldIncludeInjection(generator, prevInstant));
        assertFalse(parameters.shouldIncludeInjection(generator, cur1Instant));
        assertTrue(parameters.shouldIncludeInjection(load, prevInstant));
        assertTrue(parameters.shouldIncludeInjection(load, cur1Instant));
    }

    @Test
    void testCosts() {
        InjectionRangeActionCosts zero = new InjectionRangeActionCosts(0, 0, 0);

        assertEquals(zero, parameters.getRaCosts(prevInstant));
        assertEquals(zero, parameters.getRaCosts(cur1Instant));

        parameters.setRaCostsProvider(instant ->
            new InjectionRangeActionCosts(10 + (instant.isPreventive() ? 10 : 0), 1, 1));

        assertEquals(new InjectionRangeActionCosts(20, 1, 1), parameters.getRaCosts(prevInstant));
        assertEquals(new InjectionRangeActionCosts(10, 1, 1), parameters.getRaCosts(cur1Instant));
    }

    @Test
    void testRange() {
        MinAndMax<Double> zeroRange = new MinAndMax<>(0., 0.);

        assertEquals(zeroRange, parameters.getRaRange(prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(cur1Instant));

        parameters.setRaRangeProvider(instant ->
            instant.isPreventive() ? new MinAndMax<>(-100., 200.) : zeroRange);

        assertEquals(new MinAndMax<>(-100., 200.), parameters.getRaRange(prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(cur1Instant));
    }
}
