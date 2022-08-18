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
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private CracImpl crac;
    private String contingency1Id = "condId1";
    private Contingency contingency1;
    private VoltageCnec cnec1;
    private VoltageCnec cnec2;

    @Before
    public void setUp() {
        crac = new CracImpl("test-crac");
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createVoltageCnecs() {
        cnec1 = crac.newVoltageCnec()
                .withId("cnecId1")
                .withName("cnecName1")
                .withInstant(Instant.OUTAGE)
                .withContingency(contingency1Id)
                .withOperator("cnec1Operator")
                .withNetworkElement("neId1", "neName1")
                .newThreshold().withUnit(Unit.KILOVOLT).withMax(1000.0).withMin(-1000.0).add()
                .add();
        cnec2 = crac.newVoltageCnec()
                .withId("cnecId2")
                .withInstant(Instant.PREVENTIVE)
                .withOperator("cnec2Operator")
                .withNetworkElement("neId2")
                .newThreshold().withUnit(Unit.KILOVOLT).withMax(500.0).add()
                .add();
    }

    @Test
    public void testCheckCnecs() {
        createVoltageCnecs();
        assertEquals(2, crac.getVoltageCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getVoltageCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(Instant.OUTAGE, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getVoltageCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(Instant.PREVENTIVE, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(Unit.KILOVOLT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(Unit.KILOVOLT).isPresent());

    }

    @Test
    public void testAdd() {
        createVoltageCnecs();
        // Verify that network elements were created
        crac.newVoltageCnec()
            .withId("cnecId3")
            .withInstant(Instant.PREVENTIVE)
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
        assertNotNull(crac.getState(contingency1Id, Instant.OUTAGE));
    }

    @Test
    public void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.KILOVOLT).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    public void testNotOptimizedMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test(expected = FaraoException.class)
    public void testOptimizedNotMonitored() {
        crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized()
            .add();
    }

    @Test
    public void testNotOptimizedNotMonitored() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    public void testNotOptimizedNotMonitored2() {
        VoltageCnec cnec = crac.newVoltageCnec().withId("Cnec ID")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test(expected = NullPointerException.class)
    public void testNullParentFail() {
        new VoltageCnecAdderImpl(null);
    }

    @Test(expected = FaraoException.class)
    public void testNoIdFail() {
        crac.newVoltageCnec()
            .withName("cnecName")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoStateInstantFail() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoNetworkElementFail() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testNoThresholdFail() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddTwiceError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddPreventiveCnecWithContingencyError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.PREVENTIVE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddOutageCnecWithNoContingencyError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddAutoCnecWithNoContingencyError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.AUTO)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithNoContingencyError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.CURATIVE)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testAddCurativeCnecWithAbsentContingencyError() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.CURATIVE)
            .withContingency("absent-from-crac")
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.KILOVOLT).withMax(100.0).withMin(-100.0).add()
            .add();
    }

    @Test(expected = FaraoException.class)
    public void testThresholdWithUnitAmpere() {
        crac.newVoltageCnec()
            .withId("cnecId")
            .withInstant(Instant.OUTAGE)
            .withContingency(contingency1Id)
            .withNetworkElement("neId")
            .newThreshold().withUnit(Unit.AMPERE).withMax(100.0).withMin(-100.0).add()
            .add();
    }
}
