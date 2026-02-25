/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
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
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FlowCnecAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private CracImpl crac;
    private final String contingency1Id = "condId1";
    private Contingency contingency1;
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

    @Test
    void testAdd() {
        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withOperator("cnec1Operator")
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(1000.0).withMin(-1000.0).add()
            .add();
        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnecId2")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withOperator("cnec2Operator")
            .withNetworkElement("neId2")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(500.0).add()
            .add();
        assertEquals(2, crac.getFlowCnecs().size());

        // Verify 1st cnec content
        assertEquals(cnec1, crac.getFlowCnec("cnecId1"));
        assertEquals("cnecName1", cnec1.getName());
        assertEquals(contingency1, cnec1.getState().getContingency().orElseThrow());
        assertEquals(outageInstant, cnec1.getState().getInstant());
        assertEquals("cnec1Operator", cnec1.getOperator());
        assertEquals("neName1", cnec1.getNetworkElement().getName());
        assertEquals(1000.0, cnec1.getUpperBound(ONE, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertEquals(-1000.0, cnec1.getLowerBound(ONE, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);

        // Verify 2nd cnec content
        assertEquals(cnec2, crac.getFlowCnec("cnecId2"));
        assertEquals("cnecId2", cnec2.getName());
        assertEquals(preventiveInstant, cnec2.getState().getInstant());
        assertEquals("cnec2Operator", cnec2.getOperator());
        assertEquals(Optional.empty(), cnec2.getState().getContingency());
        assertEquals("neId2", cnec2.getNetworkElement().getName());
        assertEquals(500.0, cnec2.getUpperBound(ONE, Unit.MEGAWATT).orElseThrow(), DOUBLE_TOLERANCE);
        assertFalse(cnec2.getLowerBound(ONE, Unit.MEGAWATT).isPresent());

        // Verify that network elements were created
        crac.newFlowCnec()
                .withId("cnecId3")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .withOperator("cnec2Operator")
                .withNetworkElement("neId2") // same as cnec2
                .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(500.0).add()
                .add();
        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neId1"));
        assertNotNull(crac.getNetworkElement("neId2"));

        // Verify states were created
        assertEquals(2, crac.getStates().size());
        assertNotNull(crac.getPreventiveState());
        assertNotNull(crac.getState(contingency1Id, outageInstant));
    }

    @Test
    void testFrmHandling() {
        double maxValueInMw = 100.0;
        double frmInMw = 5.0;
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(maxValueInMw).withMin(-maxValueInMw).add()
            .withReliabilityMargin(frmInMw)
            .add();
        assertEquals(maxValueInMw - frmInMw, cnec.getUpperBound(ONE, Unit.MEGAWATT).orElseThrow(OpenRaoException::new), 0.0);
        assertEquals(frmInMw - maxValueInMw, cnec.getLowerBound(ONE, Unit.MEGAWATT).orElseThrow(OpenRaoException::new), 0.0);
    }

    @Test
    void testNotOptimizedMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withMonitored()
            .add();
        assertFalse(cnec.isOptimized());
        assertTrue(cnec.isMonitored());
    }

    @Test
    void testOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withOptimized()
            .add();
        assertTrue(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(380.)
            .withIMax(1200., ONE)
            .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void testNotOptimizedNotMonitored2() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency(contingency1Id)
                .withNetworkElement("Network Element ID")
                .newThreshold().withUnit(Unit.AMPERE).withSide(ONE).withMax(100.0).withMin(-100.0).add()
                .withOptimized(false)
                .withMonitored(false)
                .withNominalVoltage(380.)
                .add();
        assertFalse(cnec.isOptimized());
        assertFalse(cnec.isMonitored());
    }

    @Test
    void checkThresholdSideInitialisation1() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMax(1.).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.AMPERE).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.AMPERE).withSide(ONE).withMax(200.0).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .withNominalVoltage(380., ONE)
            .withNominalVoltage(220., TWO)
            .withIMax(2000.)
            .add();

        assertEquals(3, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().allMatch(th -> th.getSide().equals(ONE)));
    }

    @Test
    void checkThresholdSideInitialisation2() {
        FlowCnec cnec = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TWO).withMax(100.0).withMin(-200.0).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TWO).withMax(100.0).withMin(-100.0).add()
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(TWO).withMax(1.).withMin(-100.0).add()
            .withOptimized(false)
            .withMonitored(false)
            .withNominalVoltage(380., ONE)
            .withNominalVoltage(220., TWO)
            .withIMax(2000.)
            .add();

        assertEquals(3, cnec.getThresholds().size());
        assertTrue(cnec.getThresholds().stream().allMatch(th -> th.getSide().equals(TWO)));
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new FlowCnecAdderImpl(null));
    }

    @Test
    void testUniqueNetworkElement() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withNetworkElement("neId1", "neName1");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> flowCnecAdder.withNetworkElement("neId2", "neName2"));
        assertEquals("Cannot add multiple network elements for a flow cnec.", exception.getMessage());
    }

    @Test
    void testNoIdFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withName("cnecName1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(1000.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("Cannot add a FlowCnec object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoStateInstantFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withContingency(contingency1Id)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.0).withSide(ONE).add()
            .withNetworkElement("neId1", "neName1")
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("Cannot add Cnec without a instant. Please use withInstant() with a non null value", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .newThreshold().withSide(ONE).withUnit(Unit.MEGAWATT).withMax(1000.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("Cannot add Cnec without a network element. Please use withNetworkElement()", exception.getMessage());
    }

    @Test
    void testNoThresholdFail() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec()
            .withId("cnecId1")
            .withName("cnecName1")
            .withInstant(OUTAGE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("neId1", "neName1")
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("Cannot add a cnec without a threshold. Please use newThreshold", exception.getMessage());
    }

    @Test
    void testAddTwiceError() {
        crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.)
            .add();
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("Cannot add a cnec with an already existing ID - Cnec ID.", exception.getMessage());
    }

    @Test
    void testAddPreventiveCnecWithContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withContingency(contingency1Id)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("You cannot define a contingency for a preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddOutageCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(OUTAGE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddAutoCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(AUTO_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithNoContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(CURATIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals("You must define a contingency for a non-preventive cnec.", exception.getMessage());
    }

    @Test
    void testAddCurativeCnecWithAbsentContingencyError() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("absent-from-crac-contingency")
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(ONE).withMax(100.0).withMin(-100.0).add()
            .withNominalVoltage(220)
            .withIMax(2000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals(
            "Contingency absent-from-crac-contingency of Cnec Cnec ID does not exist in the crac. Use crac.newContingency() first.",
            exception.getMessage()
        );
    }

    @Test
    void testThresholdInPercentImaxButNoIMax1() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMax(1.).withMin(-1.).add()
            .withNominalVoltage(220.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals(
            "iMax on left side of FlowCnec Cnec ID must be defined, as one of its threshold is on PERCENT_IMAX on the left side. Please use withIMax()",
            exception.getMessage()
        );
    }

    @Test
    void testThresholdInPercentImaxButNoIMax2() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMax(1.).withMin(-1.).add()
            .withNominalVoltage(220.)
            .withIMax(1000., TWO); // threshold on left side cannot be interpreted
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals(
            "iMax on left side of FlowCnec Cnec ID must be defined, as one of its threshold is on PERCENT_IMAX on the left side. Please use withIMax()",
            exception.getMessage()
        );
    }

    @Test
    void testThresholdInAmpereButNoNominalVoltage() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.AMPERE).withSide(ONE).withMax(1000.).add()
            .withIMax(1000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals(
            "nominal voltages on both side of FlowCnec Cnec ID must be defined, as one of its threshold is on PERCENT_IMAX or AMPERE. Please use withNominalVoltage()",
            exception.getMessage()
        );
    }

    @Test
    void testThresholdInPercentImaxButNoNominalVoltage() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold().withUnit(Unit.PERCENT_IMAX).withSide(ONE).withMax(1000.).add()
            .withNominalVoltage(220., ONE) // should be defined on both side
            .withIMax(1000.);
        OpenRaoException exception = assertThrows(OpenRaoException.class, flowCnecAdder::add);
        assertEquals(
            "nominal voltages on both side of FlowCnec Cnec ID must be defined, as one of its threshold is on PERCENT_IMAX or AMPERE. Please use withNominalVoltage()",
            exception.getMessage()
        );
    }

    @Test
    void testThresholdWithUnitKiloVolt() {
        BranchThresholdAdder branchThresholdAdder = crac.newFlowCnec().withId("Cnec ID")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .withNetworkElement("Network Element ID")
            .newThreshold();
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> branchThresholdAdder.withUnit(Unit.KILOVOLT));
        assertEquals("kV Unit is not suited to measure a FLOW value.", exception.getMessage());
    }
}
