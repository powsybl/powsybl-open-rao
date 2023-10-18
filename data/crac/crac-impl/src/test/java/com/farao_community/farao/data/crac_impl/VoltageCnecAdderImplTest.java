/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.ThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.VoltageThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class VoltageCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final Instant INSTANT_PREV = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant INSTANT_OUTAGE = new InstantImpl("outage", InstantKind.OUTAGE, INSTANT_PREV);
    private static final Instant INSTANT_AUTO = new InstantImpl("auto", InstantKind.AUTO, INSTANT_OUTAGE);
    private static final Instant INSTANT_CURATIVE = new InstantImpl("curative", InstantKind.CURATIVE, INSTANT_AUTO);
    private final String contingency1Id = "condId1";
    private CracImpl crac;
    private Contingency contingency1;
    private VoltageCnec cnec1;
    private VoltageCnec cnec2;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        crac.addInstant(INSTANT_PREV);
        crac.addInstant(INSTANT_OUTAGE);
        crac.addInstant(INSTANT_AUTO);
        crac.addInstant(INSTANT_CURATIVE);
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createVoltageCnecs() {
        cnec1 = crac.newVoltageCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.0).withMin(-1000.0).add()
            .add();
        cnec2 = crac.newVoltageCnec()
            .withId("cnecId2")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.0).add()
            .add();
    }

    @Test
    void testCheckCnecs() {
        createVoltageCnecs();
        assertEquals(2, crac.getVoltageCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getVoltageCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(INSTANT_OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getVoltageCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(INSTANT_PREV, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(Unit.KILOVOLT).isPresent());

    }

    @Test
    void testAdd() {
        createVoltageCnecs();
        // Verify that network elements were created
        crac.newVoltageCnec()
            .withId("cnecId3")
            .withInstantId(INSTANT_PREV.getId())
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2") // same as cnec2
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neId1"));
        assertNotNull(crac.getNetworkElement("neId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, INSTANT_OUTAGE));
    }

    @Test
    void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    void testNotOptimizedMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    void testOptimizedNotMonitored() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testNotOptimizedNotMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored2() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new VoltageCnecAdderImpl(null));
    }

    @Test
    void testNoIdFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withName("cnecName")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testNoStateInstantFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testNoNetworkElementFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testNoThresholdFail() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId");
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddTwiceError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_PREV.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_AUTO.getId())
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_CURATIVE.getId())
            .withContingency("absent-from-crac")
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, voltageCnecAdder::add);
    }

    @Test
    void testThresholdWithUnitAmpere() {
        ThresholdAdder<VoltageThresholdAdder> thresholdAdder = crac.newVoltageCnec()
            .withId("cnecId")
            .withInstantId(INSTANT_OUTAGE.getId())
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold();
        assertThrows(FaraoException.class, () -> thresholdAdder.withUnit(Unit.AMPERE));
    }
}
