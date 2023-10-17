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
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.AngleThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class AngleCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final Instant instantPrev = new InstantImpl("preventive", InstantKind.PREVENTIVE, null);
    private static final Instant instantOutage = new InstantImpl("outage", InstantKind.OUTAGE, instantPrev);
    private static final Instant instantAuto = new InstantImpl("auto", InstantKind.AUTO, instantOutage);
    private static final Instant instantCurative = new InstantImpl("curative", InstantKind.CURATIVE, instantAuto);
    private CracImpl crac;
    private String contingency1Id = "condId1";
    private Contingency contingency1;
    private AngleCnec cnec1;
    private AngleCnec cnec2;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac");
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createAngleCnecs() {
        cnec1 = crac.newAngleCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withExportingNetworkElement("eneId1", "eneName1")
            .withImportingNetworkElement("ineId1", "ineName1")
            .newThreshold().withUnit(Unit.DEGREE).withMax(1000.0).withMin(-1000.0).add()
            .add();
        cnec2 = crac.newAngleCnec()
            .withId("cnecId2")
            .withInstant(instantPrev)
            .withOperator("cnec2Operator")
            .withExportingNetworkElement("eneId2")
            .withImportingNetworkElement("ineId2")
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.0).add()
            .add();
    }

    @Test
    void testCheckCnecs() {
        createAngleCnecs();
        assertEquals(2, crac.getAngleCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getAngleCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(instantOutage, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("eneName1", cnec1.getExportingNetworkElement().getName());
        assertEquals("ineName1", cnec1.getImportingNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getAngleCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(instantPrev, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("eneId2", cnec2.getExportingNetworkElement().getName());
        assertEquals("ineId2", cnec2.getImportingNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(Unit.DEGREE).isPresent());

    }

    @Test
    void testAdd() {
        createAngleCnecs();
        // Verify that network elements were created
        crac.newAngleCnec()
            .withId("cnecId3")
            .withInstant(instantPrev)
            .withOperator("cnec2Operator")
            .withExportingNetworkElement("eneId2") // same as cnec2
            .withImportingNetworkElement("ineId2") // same as cnec2
            .newThreshold().withUnit(Unit.DEGREE).withMax(500.0).add()
            .add();
        assertEquals(4, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("eneId1"));
        assertNotNull(crac.getNetworkElement("ineId1"));
        assertNotNull(crac.getNetworkElement("eneId2"));
        assertNotNull(crac.getNetworkElement("ineId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, instantOutage));
    }

    @Test
    void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.DEGREE).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.DEGREE).orElseThrow(FaraoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    void testNotOptimizedMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    void testOptimizedNotMonitored() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withOptimized();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testNotOptimizedNotMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored2() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new AngleCnecAdderImpl(null));
    }

    @Test
    void testNetworkElementNotImportingNotExporting() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec();
        assertThrows(FaraoException.class, () -> angleCnecAdder.withNetworkElement("neId1", "neName1"));
    }

    @Test
    void testNoIdFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withName("cnecName")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testNoStateInstantFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testNoExportingNetworkElementFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testNoImportingNetworkElementFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testNoThresholdFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId");
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddTwiceError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantPrev)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantAuto)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantCurative)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantCurative)
            .withContingency("absent-from-crac")
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        assertThrows(FaraoException.class, angleCnecAdder::add);
    }

    @Test
    void testThresholdWithUnitKiloVolt() {
        AngleThresholdAdder angleThresholdAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(instantOutage)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold();
        assertThrows(FaraoException.class, () -> angleThresholdAdder.withUnit(Unit.KILOVOLT));
    }
}
