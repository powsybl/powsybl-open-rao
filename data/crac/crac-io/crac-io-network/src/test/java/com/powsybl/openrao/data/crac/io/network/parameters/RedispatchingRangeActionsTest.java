/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class RedispatchingRangeActionsTest extends AbstractTest {
    private RedispatchingRangeActions parameters;

    @BeforeEach
    void setUp() {
        parameters = new NetworkCracCreationParameters(null, List.of("cur")).getRedispatchingRangeActions();
    }

    @Test
    void testIncludeAllInjections() {
        assertTrue(parameters.includeAllInjections());

        parameters.setIncludeAllInjections(false);
        assertFalse(parameters.includeAllInjections());
    }

    @Test
    void testPredicate() {
        assertTrue(parameters.shouldCreateRedispatchingAction(generator, prevInstant));
        assertTrue(parameters.shouldCreateRedispatchingAction(generator, cur1Instant));
        assertFalse(parameters.shouldCreateRedispatchingAction(load, prevInstant));
        assertFalse(parameters.shouldCreateRedispatchingAction(load, cur1Instant));

        parameters.setRdRaPredicate((injection, instant) -> instant.isPreventive() || injection.getType() == IdentifiableType.LOAD);

        assertTrue(parameters.shouldCreateRedispatchingAction(generator, prevInstant));
        assertFalse(parameters.shouldCreateRedispatchingAction(generator, cur1Instant));
        assertTrue(parameters.shouldCreateRedispatchingAction(load, prevInstant));
        assertTrue(parameters.shouldCreateRedispatchingAction(load, cur1Instant));
    }

    @Test
    void testCosts() {
        InjectionRangeActionCosts zero = new InjectionRangeActionCosts(0, 0, 0);

        assertEquals(zero, parameters.getRaCosts(generator, prevInstant));
        assertEquals(zero, parameters.getRaCosts(generator, cur1Instant));
        assertEquals(zero, parameters.getRaCosts(load, prevInstant));
        assertEquals(zero, parameters.getRaCosts(load, cur1Instant));

        parameters.setRaCostsProvider((injection, instant) ->
            injection.getType() ==
                IdentifiableType.LOAD ?
                new InjectionRangeActionCosts(1000, 10, 0) :
                new InjectionRangeActionCosts(10 + (instant.isPreventive() ? 10 : 0), 1, 1));

        assertEquals(new InjectionRangeActionCosts(20, 1, 1), parameters.getRaCosts(generator, prevInstant));
        assertEquals(new InjectionRangeActionCosts(10, 1, 1), parameters.getRaCosts(generator, cur1Instant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getRaCosts(load, prevInstant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getRaCosts(load, cur1Instant));
    }

    @Test
    void testRange() {
        MinAndMax<Double> nullRange = new MinAndMax<>(null, null);

        assertEquals(nullRange, parameters.getRaRange(generator, prevInstant));
        assertEquals(nullRange, parameters.getRaRange(generator, cur1Instant));
        assertEquals(nullRange, parameters.getRaRange(load, prevInstant));
        assertEquals(nullRange, parameters.getRaRange(load, cur1Instant));

        parameters.setRaRangeProvider((injection, instant) ->
            instant.isPreventive() ?
                new MinAndMax<>(injection.getType() == IdentifiableType.GENERATOR ? -100. : 0., 200.)
                : nullRange
        );

        assertEquals(new MinAndMax<>(-100., 200.), parameters.getRaRange(generator, prevInstant));
        assertEquals(nullRange, parameters.getRaRange(generator, cur1Instant));
        assertEquals(new MinAndMax<>(0., 200.), parameters.getRaRange(load, prevInstant));
        assertEquals(nullRange, parameters.getRaRange(load, cur1Instant));
    }

    @Test
    void testCombinations() {
        assertTrue(parameters.getGeneratorCombinations().isEmpty());

        Map<String, Set<String>> combis = Map.of("combi1", Set.of("gen1", "gen2"), "combi2", Set.of("gen3"));
        parameters.setGeneratorCombinations(combis);
        assertEquals(combis, parameters.getGeneratorCombinations());

        Map<String, Set<String>> combiWithDuplicate = Map.of("combi1", Set.of("gen1", "gen2"), "combi2", Set.of("gen1"));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.setGeneratorCombinations(combiWithDuplicate));
        assertEquals("A generator can only be used once in generator combinations.", exception.getMessage());
    }

    @Test
    void testCombinationRange() {
        MinAndMax<Double> nullRange = new MinAndMax<>(null, null);
        assertEquals(nullRange, parameters.getCombinationRange("combi1", prevInstant));
        assertEquals(nullRange, parameters.getCombinationRange("combi1", cur1Instant));
        assertEquals(nullRange, parameters.getCombinationRange("combi2", cur1Instant));
        assertEquals(nullRange, parameters.getCombinationRange("combi2", cur1Instant));

        parameters.setCombinationRangeProvider((combi, instant) ->
            instant.isPreventive() ?
                new MinAndMax<>(Objects.equals(combi, "combi1") ? -100. : 0., 200.)
                : nullRange
        );

        assertEquals(new MinAndMax<>(-100., 200.), parameters.getCombinationRange("combi1", prevInstant));
        assertEquals(nullRange, parameters.getCombinationRange("combi1", cur1Instant));
        assertEquals(new MinAndMax<>(0., 200.), parameters.getCombinationRange("combi2", prevInstant));
        assertEquals(nullRange, parameters.getCombinationRange("combi2", cur1Instant));
    }

    @Test
    void testCombinationCosts() {
        InjectionRangeActionCosts zero = new InjectionRangeActionCosts(0, 0, 0);

        assertEquals(zero, parameters.getCombinationCosts("combi1", prevInstant));
        assertEquals(zero, parameters.getCombinationCosts("combi1", cur1Instant));
        assertEquals(zero, parameters.getCombinationCosts("combi2", prevInstant));
        assertEquals(zero, parameters.getCombinationCosts("combi2", cur1Instant));

        parameters.setCombinationCostsProvider((combi, instant) ->
            Objects.equals(combi, "combi2") ?
                new InjectionRangeActionCosts(1000, 10, 0) :
                new InjectionRangeActionCosts(10 + (instant.isPreventive() ? 10 : 0), 1, 1));

        assertEquals(new InjectionRangeActionCosts(20, 1, 1), parameters.getCombinationCosts("combi1", prevInstant));
        assertEquals(new InjectionRangeActionCosts(10, 1, 1), parameters.getCombinationCosts("combi1", cur1Instant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getCombinationCosts("combi2", prevInstant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getCombinationCosts("combi2", cur1Instant));
    }
}
