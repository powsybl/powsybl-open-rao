/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.openrao.commons.OpenRaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CriticalElementsTest extends AbstractTest {
    private CriticalElements parameters;

    @BeforeEach
    void setUp() {
        parameters = new NetworkCracCreationParameters(null, List.of("cur1", "cur2")).getCriticalElements();
    }

    @Test
    void testThresholdDefinition() {
        assertEquals(CriticalElements.ThresholdDefinition.FROM_OPERATIONAL_LIMITS, parameters.getThresholdDefinition());
        parameters.setThresholdDefinition(CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER);
        assertEquals(CriticalElements.ThresholdDefinition.PERM_LIMIT_MULTIPLIER, parameters.getThresholdDefinition());
    }

    @Test
    void testCMinMaxV() {
        // Optimized min and max V
        assertTrue(parameters.getOptimizedMinV().isEmpty());
        assertTrue(parameters.getOptimizedMaxV().isEmpty());

        parameters.setOptimizedMinMaxV(-1., 2.);
        assertEquals(Optional.of(-1.), parameters.getOptimizedMinV());
        assertEquals(Optional.of(2.), parameters.getOptimizedMaxV());

        parameters.setOptimizedMinMaxV(null, -250.);
        assertTrue(parameters.getOptimizedMinV().isEmpty());
        assertEquals(Optional.of(-250.), parameters.getOptimizedMaxV());

        parameters.setOptimizedMinMaxV(50., null);
        assertEquals(Optional.of(50.), parameters.getOptimizedMinV());
        assertTrue(parameters.getOptimizedMaxV().isEmpty());

        parameters.setOptimizedMinMaxV(null, null);
        assertTrue(parameters.getOptimizedMinV().isEmpty());
        assertTrue(parameters.getOptimizedMaxV().isEmpty());

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.setOptimizedMinMaxV(10., 2.));
        assertEquals("Min should be smaller than max!", exception.getMessage());

        // Monitored min and max V
        assertTrue(parameters.getMonitoredMinMaxV().isEmpty());

        parameters.setMonitoredMinMaxV(new MinAndMax<>(-1., 2.));
        assertEquals(Optional.of(new MinAndMax<>(-1., 2.)), parameters.getMonitoredMinMaxV());

        parameters.setMonitoredMinMaxV(new MinAndMax<>(null, -250.));
        assertEquals(Optional.of(new MinAndMax<>(null, -250.)), parameters.getMonitoredMinMaxV());

        parameters.setMonitoredMinMaxV(new MinAndMax<>(50., null));
        assertEquals(Optional.of(new MinAndMax<>(50., null)), parameters.getMonitoredMinMaxV());

        parameters.setMonitoredMinMaxV(new MinAndMax<>(null, null));
        assertEquals(Optional.of(new MinAndMax<>(null, null)), parameters.getMonitoredMinMaxV());

        parameters.setMonitoredMinMaxV(null);
        assertTrue(parameters.getMonitoredMinMaxV().isEmpty());
    }

    @Test
    void testIsOptimizedOrMonitored() {
        Branch<?> b1 = Mockito.mock(Branch.class);
        Branch<?> b2 = Mockito.mock(Branch.class);
        Contingency co1 = Mockito.mock(Contingency.class);
        Contingency co2 = Mockito.mock(Contingency.class);

        // Default
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b1, null));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b1, co1));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b1, co2));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b2, null));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b2, co1));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b2, co2));

        // Custom function
        parameters.setOptimizedMonitoredProvider((b, c) -> new CriticalElements.OptimizedMonitored(c == null, b == b1 || c == co2));
        assertEquals(new CriticalElements.OptimizedMonitored(true, true), parameters.isOptimizedOrMonitored(b1, null));
        assertEquals(new CriticalElements.OptimizedMonitored(false, true), parameters.isOptimizedOrMonitored(b1, co1));
        assertEquals(new CriticalElements.OptimizedMonitored(false, true), parameters.isOptimizedOrMonitored(b1, co2));
        assertEquals(new CriticalElements.OptimizedMonitored(true, false), parameters.isOptimizedOrMonitored(b2, null));
        assertEquals(new CriticalElements.OptimizedMonitored(false, false), parameters.isOptimizedOrMonitored(b2, co1));
        assertEquals(new CriticalElements.OptimizedMonitored(false, true), parameters.isOptimizedOrMonitored(b2, co2));
    }

    @Test
    void testLimitMultiplierErrors() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.getLimitMultiplierPerInstant(prevInstant, 10.));
        assertEquals("Limit multiplier per instant is null. Please set it using getLimitMultiplierPerInstant.", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> parameters.setLimitMultiplierPerInstant(null));
        assertEquals("You must define the value for every instant.", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> parameters.setLimitMultiplierPerInstantPerNominalV(null));
        assertEquals("You must define the value for every instant.", exception.getMessage());

        Map<String, Double> limits1 = Map.of("preventive", 1.0, "outage", 1.3, "cur1", 1.1); // cur2 missing
        exception = assertThrows(OpenRaoException.class, () -> parameters.setLimitMultiplierPerInstant(limits1));
        assertEquals("You must define the value for every instant.", exception.getMessage());

        Map<String, Map<Double, Double>> limits2 = Map.of("preventive", Map.of(150., 1.0), "outage", Map.of(150., 1.0), "cur1", Map.of(150., 1.0)); // cur2 missing
        exception = assertThrows(OpenRaoException.class, () -> parameters.setLimitMultiplierPerInstantPerNominalV(limits2));
        assertEquals("You must define the value for every instant.", exception.getMessage());
    }

    @Test
    void testLimitMultiplierNominal() {
        parameters.setLimitMultiplierPerInstant(Map.of("preventive", 1.0, "outage", 1.3, "cur1", 1.2, "cur2", 1.1));
        assertEquals(1.0, parameters.getLimitMultiplierPerInstant(prevInstant, 10.));
        assertEquals(1.0, parameters.getLimitMultiplierPerInstant(prevInstant, 20.));
        assertEquals(1.3, parameters.getLimitMultiplierPerInstant(outInstant, 20.));
        assertEquals(1.3, parameters.getLimitMultiplierPerInstant(outInstant, 30.));
        assertEquals(1.2, parameters.getLimitMultiplierPerInstant(cur1Instant, 30.));
        assertEquals(1.2, parameters.getLimitMultiplierPerInstant(cur1Instant, 40.));
        assertEquals(1.1, parameters.getLimitMultiplierPerInstant(cur2Instant, 40.));
        assertEquals(1.1, parameters.getLimitMultiplierPerInstant(cur2Instant, 50.));

        parameters.setLimitMultiplierPerInstantPerNominalV(Map.of("preventive", Map.of(10., 1.05), "outage", Map.of(20., 1.35), "cur1", Map.of(30., 1.25), "cur2", Map.of(40., 1.15)));
        assertEquals(1.05, parameters.getLimitMultiplierPerInstant(prevInstant, 10.));
        assertEquals(1.0, parameters.getLimitMultiplierPerInstant(prevInstant, 20.));
        assertEquals(1.35, parameters.getLimitMultiplierPerInstant(outInstant, 20.));
        assertEquals(1.3, parameters.getLimitMultiplierPerInstant(outInstant, 30.));
        assertEquals(1.25, parameters.getLimitMultiplierPerInstant(cur1Instant, 30.));
        assertEquals(1.2, parameters.getLimitMultiplierPerInstant(cur1Instant, 40.));
        assertEquals(1.15, parameters.getLimitMultiplierPerInstant(cur2Instant, 40.));
        assertEquals(1.1, parameters.getLimitMultiplierPerInstant(cur2Instant, 50.));
    }

    @Test
    void testAcceptableDurationErrors() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> parameters.getApplicableLimitDuration(prevInstant, 10.));
        assertEquals("Acceptable duration per instant is null. Please set it using setApplicableLimitDurationPerInstant.", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> parameters.setApplicableLimitDurationPerInstant(null));
        assertEquals("You must define the value for every instant except preventive.", exception.getMessage());

        exception = assertThrows(OpenRaoException.class, () -> parameters.setApplicableLimitDurationPerInstantPerNominalV(null));
        assertEquals("You must define the value for every instant except preventive.", exception.getMessage());

        Map<String, Double> limits1 = Map.of("preventive", 100., "outage", 130., "cur1", 110.); // cur2 missing
        exception = assertThrows(OpenRaoException.class, () -> parameters.setApplicableLimitDurationPerInstant(limits1));
        assertEquals("You must define the value for every instant except preventive.", exception.getMessage());

        Map<String, Map<Double, Double>> limits2 = Map.of("preventive", Map.of(150., 100.), "outage", Map.of(150., 100.), "cur1", Map.of(150., 100.)); // cur2 missing
        exception = assertThrows(OpenRaoException.class, () -> parameters.setApplicableLimitDurationPerInstantPerNominalV(limits2));
        assertEquals("You must define the value for every instant except preventive.", exception.getMessage());
    }

    @Test
    void testAcceptabledurationNominal() {
        parameters.setApplicableLimitDurationPerInstant(Map.of("outage", 130., "cur1", 120., "cur2", 110.));
        assertEquals(Double.MAX_VALUE, parameters.getApplicableLimitDuration(prevInstant, 10.));
        assertEquals(Double.MAX_VALUE, parameters.getApplicableLimitDuration(prevInstant, 20.));
        assertEquals(130., parameters.getApplicableLimitDuration(outInstant, 20.));
        assertEquals(130., parameters.getApplicableLimitDuration(outInstant, 30.));
        assertEquals(120., parameters.getApplicableLimitDuration(cur1Instant, 30.));
        assertEquals(120., parameters.getApplicableLimitDuration(cur1Instant, 40.));
        assertEquals(110., parameters.getApplicableLimitDuration(cur2Instant, 40.));
        assertEquals(110., parameters.getApplicableLimitDuration(cur2Instant, 50.));

        parameters.setApplicableLimitDurationPerInstantPerNominalV(Map.of("outage", Map.of(20., 135.), "cur1", Map.of(30., 125.), "cur2", Map.of(40., 115.)));
        assertEquals(Double.MAX_VALUE, parameters.getApplicableLimitDuration(prevInstant, 10.));
        assertEquals(Double.MAX_VALUE, parameters.getApplicableLimitDuration(prevInstant, 20.));
        assertEquals(135., parameters.getApplicableLimitDuration(outInstant, 20.));
        assertEquals(130., parameters.getApplicableLimitDuration(outInstant, 30.));
        assertEquals(125., parameters.getApplicableLimitDuration(cur1Instant, 30.));
        assertEquals(120., parameters.getApplicableLimitDuration(cur1Instant, 40.));
        assertEquals(115., parameters.getApplicableLimitDuration(cur2Instant, 40.));
        assertEquals(110., parameters.getApplicableLimitDuration(cur2Instant, 50.));
    }
}
