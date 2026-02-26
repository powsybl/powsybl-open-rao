/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.AngleThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class AngleCnecAdderImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private CracImpl crac;
    private String contingency1Id = "condId1";
    private Contingency contingency1;
    private AngleCnec cnec1;
    private AngleCnec cnec2;
    private Instant preventiveInstant;
    private Instant outageInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        contingency1 = crac.newContingency().withId(contingency1Id).add();
    }

    private void createAngleCnecs() {
        cnec1 = crac.newAngleCnec()
                .withId("cnecId1")
                .withName("cnecName1")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency(contingency1Id)
                .withOperator("cnec1Operator")
                .withExportingNetworkElement("eneId1", "eneName1")
                .withImportingNetworkElement("ineId1", "ineName1")
                .newThreshold().withUnit(Unit.DEGREE).withMax(1000.0).withMin(-1000.0).add()
                .add();
        cnec2 = crac.newAngleCnec()
                .withId("cnecId2")
                .withInstant(PREVENTIVE_INSTANT_ID)
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
        assertEquals(outageInstant, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("eneName1", cnec1.getExportingNetworkElement().getName());
        assertEquals("ineName1", cnec1.getImportingNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(Unit.DEGREE).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getAngleCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(preventiveInstant, cnec2.getState().getInstant());
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
            .withInstant(PREVENTIVE_INSTANT_ID)
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
        assertNotNull(crac.getState(contingency1Id, outageInstant));
    }

    @Test
    void testReliabilityMarginHandling() {
        double maxValue = 100.0;
        double reliabilityMargin = 5.0;
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(maxValue).withMin(-maxValue).add()
            .withReliabilityMargin(reliabilityMargin)
            .add();
        assertEquals(maxValue - reliabilityMargin, cnec.getUpperBound(Unit.DEGREE).orElseThrow(OpenRaoException::new), DOUBLE_TOLERANCE);
        assertEquals(reliabilityMargin - maxValue, cnec.getLowerBound(Unit.DEGREE).orElseThrow(OpenRaoException::new), DOUBLE_TOLERANCE);
    }

    @Test
    void testNotOptimizedMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
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
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .withOptimized();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Error while adding cnec Cnec ID : Open RAO does not allow the optimization of AngleCnecs.", exception.getMessage());
    }

    @Test
    void testNotOptimizedNotMonitored() {
        AngleCnec cnec = crac.newAngleCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
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
            .withInstant(OUTAGE_INSTANT_ID)
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
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleCnecAdder.withNetworkElement("neId1", "neName1"));
        assertEquals("For an angle cnec, use withExportingNetworkElement() and withImportingNetworkElement().", exception.getMessage());
    }

    @Test
    void testNoIdFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withName("cnecName")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add a AngleCnec object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoStateInstantFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add Cnec without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoExportingNetworkElementFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add AngleCnec without a exporting network element. Please use withExportingNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testNoImportingNetworkElementFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add AngleCnec without a importing network element. Please use withImportingNetworkElement() with a non null value", exception.getMessage());
    }

    @Test
    void testNoThresholdFail() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId");
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add an AngleCnec without a threshold. Please use newThreshold", exception.getMessage());
    }

    @Test
    void testAddTwiceError() {
        crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add()
            .add();
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Cannot add a cnec with an already existing ID - cnecId.", exception.getMessage());
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("You cannot define a contingency for a preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(AUTO_INSTANT_ID)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(CURATIVE_INSTANT_ID)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        AngleCnecAdder angleCnecAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("absent-from-crac")
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold().withUnit(Unit.DEGREE).withMax(100.0).withMin(-100.0).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, angleCnecAdder::add);
        assertEquals("Contingency absent-from-crac of Cnec cnecId does not exist in the crac. Use crac.newContingency() first.", exception.getMessage());
    }

    @Test
    void testThresholdWithUnitKiloVolt() {
        AngleThresholdAdder angleThresholdAdder = crac.newAngleCnec()
            .withId("cnecId")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withExportingNetworkElement("eneId")
            .withImportingNetworkElement("ineId")
            .newThreshold();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> angleThresholdAdder.withUnit(Unit.KILOVOLT));
        assertEquals("kV Unit is not suited to measure a ANGLE value.", exception.getMessage());
    }
}
