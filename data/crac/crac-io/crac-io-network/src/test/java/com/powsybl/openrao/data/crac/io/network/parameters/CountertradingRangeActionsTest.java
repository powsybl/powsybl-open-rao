/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CountertradingRangeActionsTest extends AbstractTest {
    private CountertradingRangeActions parameters;

    @BeforeEach
    void setUp() {
        parameters = new NetworkCracCreationParameters(null, List.of("cur")).getCountertradingRangeActions();
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

        assertEquals(zero, parameters.getRaCosts(Country.FR, prevInstant));
        assertEquals(zero, parameters.getRaCosts(Country.FR, cur1Instant));
        assertEquals(zero, parameters.getRaCosts(Country.BE, prevInstant));
        assertEquals(zero, parameters.getRaCosts(Country.BE, cur1Instant));

        parameters.setRaCostsProvider((country, instant) ->
            country == Country.BE ?
                new InjectionRangeActionCosts(1000, 10, 0) :
                new InjectionRangeActionCosts(10 + (instant.isPreventive() ? 10 : 0), 1, 1));

        assertEquals(new InjectionRangeActionCosts(20, 1, 1), parameters.getRaCosts(Country.FR, prevInstant));
        assertEquals(new InjectionRangeActionCosts(10, 1, 1), parameters.getRaCosts(Country.FR, cur1Instant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getRaCosts(Country.BE, prevInstant));
        assertEquals(new InjectionRangeActionCosts(1000, 10, 0), parameters.getRaCosts(Country.BE, cur1Instant));
    }

    @Test
    void testRange() {
        MinAndMax<Double> zeroRange = new MinAndMax<>(0., 0.);

        assertEquals(zeroRange, parameters.getRaRange(Country.FR, prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(Country.FR, cur1Instant));
        assertEquals(zeroRange, parameters.getRaRange(Country.BE, prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(Country.BE, cur1Instant));

        parameters.setRaRangeProvider((country, instant) ->
            instant.isPreventive() ? new MinAndMax<>(country == Country.FR ? -100. : 0., 200.) : zeroRange);

        assertEquals(new MinAndMax<>(-100., 200.), parameters.getRaRange(Country.FR, prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(Country.FR, cur1Instant));
        assertEquals(new MinAndMax<>(0., 200.), parameters.getRaRange(Country.BE, prevInstant));
        assertEquals(zeroRange, parameters.getRaRange(Country.BE, cur1Instant));
    }

    @Test
    void testGlsk() {
        assertTrue(parameters.getZonalData().isEmpty());

        ZonalData<SensitivityVariableSet> zonalData = Mockito.mock(ZonalData.class);
        parameters.setGlsks(zonalData);
        assertEquals(Optional.of(zonalData), parameters.getZonalData());
    }
}
