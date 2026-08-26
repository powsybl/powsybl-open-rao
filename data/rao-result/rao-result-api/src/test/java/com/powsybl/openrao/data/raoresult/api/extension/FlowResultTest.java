/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FlowResult}.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FlowResultTest {

    private FlowCnec flowCnec;
    private Instant preventiveInstant;
    private Instant curativeInstant;
    private FlowResult flowResult;

    @BeforeEach
    void setUp() {
        flowCnec = mock(FlowCnec.class);
        preventiveInstant = mock(Instant.class);
        curativeInstant = mock(Instant.class);

        when(preventiveInstant.getId()).thenReturn("preventive");
        when(curativeInstant.getId()).thenReturn("curative");

        // Set default symmetric bounds [-1000.0, 1000.0] on both sides for MEGAWATT and AMPERE
        when(flowCnec.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-1000.0));
        when(flowCnec.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(1000.0));
        when(flowCnec.getLowerBound(TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Optional.of(-1000.0));
        when(flowCnec.getUpperBound(TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Optional.of(1000.0));

        when(flowCnec.getLowerBound(TwoSides.ONE, Unit.AMPERE)).thenReturn(Optional.of(-2000.0));
        when(flowCnec.getUpperBound(TwoSides.ONE, Unit.AMPERE)).thenReturn(Optional.of(2000.0));
        when(flowCnec.getLowerBound(TwoSides.TWO, Unit.AMPERE)).thenReturn(Optional.of(-2000.0));
        when(flowCnec.getUpperBound(TwoSides.TWO, Unit.AMPERE)).thenReturn(Optional.of(2000.0));

        flowResult = new FlowResult();
    }

    @Test
    @DisplayName("getName() returns 'flow-results'")
    void testGetName() {
        assertEquals("flow-results", flowResult.getName());
    }

    @Nested
    @DisplayName("Uninitialized / Default State Tests")
    class UninitializedTests {

        @ParameterizedTest
        @EnumSource(Unit.class)
        @DisplayName("Returns NaN when no measurements have been added for a FlowCnec")
        void testUninitializedValues(Unit unit) {
            for (Instant instant : new Instant[]{null, preventiveInstant, curativeInstant}) {
                for (TwoSides side : TwoSides.values()) {
                    assertTrue(Double.isNaN(flowResult.getFlow(instant, flowCnec, side, unit)));
                    assertTrue(Double.isNaN(flowResult.getCommercialFlow(instant, flowCnec, side, unit)));
                    assertTrue(Double.isNaN(flowResult.getLoopFlow(instant, flowCnec, side, unit)));
                    assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(instant, flowCnec, side)));
                }
                assertTrue(Double.isNaN(flowResult.getMargin(instant, flowCnec, unit)));
                assertTrue(Double.isNaN(flowResult.getRelativeMargin(instant, flowCnec, unit)));
            }
        }

        @Test
        @DisplayName("Returns NaN for a different FlowCnec that was not modified")
        void testOtherCnecUninitialized() {
            flowResult.addFlowMeasurement(500.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            FlowCnec otherCnec = mock(FlowCnec.class);
            assertTrue(Double.isNaN(flowResult.getFlow(null, otherCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getMargin(null, otherCnec, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getCommercialFlow(null, otherCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getLoopFlow(null, otherCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(null, otherCnec, TwoSides.ONE)));
            assertTrue(Double.isNaN(flowResult.getRelativeMargin(null, otherCnec, Unit.MEGAWATT)));
        }
    }

    @Nested
    @DisplayName("Flow Measurement & Margin Computation Tests")
    class FlowAndMarginTests {

        @Test
        @DisplayName("Add flow measurement on null (initial) instant and verify getters")
        void testFlowMeasurementInitialInstant() {
            flowResult.addFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            assertEquals(200.0, flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            // Bounds are [-1000, 1000]: margin = min(200 - (-1000), 1000 - 200) = min(1200, 800) = 800.0
            assertEquals(800.0, flowResult.getMargin(null, flowCnec, Unit.MEGAWATT));

            // Other side, other unit, and other instant should remain NaN
            assertTrue(Double.isNaN(flowResult.getFlow(null, flowCnec, TwoSides.TWO, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.AMPERE)));
            assertTrue(Double.isNaN(flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT)));
        }

        @Test
        @DisplayName("Add flow measurements for different instants, sides and units")
        void testMultiInstantAndMultiUnitMeasurements() {
            flowResult.addFlowMeasurement(100.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addFlowMeasurement(150.0, null, flowCnec, TwoSides.TWO, Unit.AMPERE);
            flowResult.addFlowMeasurement(300.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addFlowMeasurement(-400.0, curativeInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT);

            assertEquals(100.0, flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(150.0, flowResult.getFlow(null, flowCnec, TwoSides.TWO, Unit.AMPERE));
            assertEquals(300.0, flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(-400.0, flowResult.getFlow(curativeInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT));

            assertEquals(900.0, flowResult.getMargin(null, flowCnec, Unit.MEGAWATT));
            // Ampere bounds [-2000, 2000]: min(150 - (-2000), 2000 - 150) = min(2150, 1850) = 1850.0
            assertEquals(1850.0, flowResult.getMargin(null, flowCnec, Unit.AMPERE));
            assertEquals(700.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
            // Curative: flow = -400, bounds [-1000, 1000]: min(-400 - (-1000), 1000 - (-400)) = min(600, 1400) = 600.0
            assertEquals(600.0, flowResult.getMargin(curativeInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Margin updates correctly when multiple flow measurements are added for the same unit and instant")
        void testMarginUpdateMostConstraining() {
            // First flow on side ONE with flow = 200 MW -> margin = 800 MW
            flowResult.addFlowMeasurement(200.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(800.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));

            // Second flow on side TWO with flow = 850 MW -> margin for 850 is min(1850, 150) = 150 MW
            // Margin should update to min(800, 150) = 150 MW
            flowResult.addFlowMeasurement(850.0, preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT);
            assertEquals(150.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));

            // Overwrite side ONE with a flow giving a less constraining individual margin (e.g. 50 MW -> margin = 950 MW)
            // Stored margin remains min(previous margin = 150, new computation = 950) = 150 MW
            flowResult.addFlowMeasurement(50.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(150.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));

            // Add side ONE with a flow giving even more constraining margin: flow = 980 MW -> margin = 20 MW
            flowResult.addFlowMeasurement(980.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(20.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Margin computation with negative margin (overload in positive flow direction)")
        void testNegativeMarginUpperBoundViolation() {
            // Flow = 1200 MW exceeding upperBound 1000 MW -> margin = min(2200, -200) = -200.0
            flowResult.addFlowMeasurement(1200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(1200.0, flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(-200.0, flowResult.getMargin(null, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Margin computation with negative margin (overload in negative flow direction)")
        void testNegativeMarginLowerBoundViolation() {
            // Flow = -1300 MW below lowerBound -1000 MW -> margin = min(-300, 2300) = -300.0
            flowResult.addFlowMeasurement(-1300.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(-1300.0, flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(-300.0, flowResult.getMargin(null, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Margin computation when bounds are asymmetric between side ONE and side TWO")
        void testAsymmetricBounds() {
            FlowCnec asymmetricCnec = mock(FlowCnec.class);
            when(asymmetricCnec.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-500.0));
            when(asymmetricCnec.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(500.0));
            when(asymmetricCnec.getLowerBound(TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Optional.of(-400.0));
            when(asymmetricCnec.getUpperBound(TwoSides.TWO, Unit.MEGAWATT)).thenReturn(Optional.of(400.0));

            // Flow = 350 MW
            // Side ONE margin: min(350 - (-500), 500 - 350) = min(850, 150) = 150
            // Side TWO margin: min(350 - (-400), 400 - 350) = min(750, 50) = 50
            // Overall margin: min(150, 50) = 50
            flowResult.addFlowMeasurement(350.0, null, asymmetricCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(50.0, flowResult.getMargin(null, asymmetricCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Margin computation when bounds are absent (empty Optionals)")
        void testAbsentBounds() {
            FlowCnec unboundedCnec = mock(FlowCnec.class);
            when(unboundedCnec.getLowerBound(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
            when(unboundedCnec.getUpperBound(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());

            // Flow = 500 MW
            // marginOne = min(500 - (-Double.MAX_VALUE), Double.MAX_VALUE - 500) = Double.MAX_VALUE - 500
            // marginTwo = same
            flowResult.addFlowMeasurement(500.0, null, unboundedCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(Double.MAX_VALUE - 500.0, flowResult.getMargin(null, unboundedCnec, Unit.MEGAWATT));
        }
    }

    @Nested
    @DisplayName("Combinations: Flow without/with Commercial Flow & Loop Flow")
    class FlowAndCommercialFlowCombinations {

        @Test
        @DisplayName("Flow without Commercial Flow: flow & margin present, commercialFlow & loopFlow NaN")
        void testFlowWithoutCommercialFlow() {
            flowResult.addFlowMeasurement(500.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            assertEquals(500.0, flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(500.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
            assertTrue(Double.isNaN(flowResult.getCommercialFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getLoopFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
        }

        @Test
        @DisplayName("Commercial Flow without Flow: commercialFlow present, flow, margin & loopFlow NaN")
        void testCommercialFlowWithoutFlow() {
            flowResult.addCommercialFlowMeasurement(300.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            assertEquals(300.0, flowResult.getCommercialFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertTrue(Double.isNaN(flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getLoopFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
        }

        @Test
        @DisplayName("Flow added first, then Commercial Flow: loopFlow = flow - commercialFlow")
        void testFlowThenCommercialFlowCalculatesLoopFlow() {
            flowResult.addFlowMeasurement(600.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertTrue(Double.isNaN(flowResult.getLoopFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));

            flowResult.addCommercialFlowMeasurement(200.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);

            assertEquals(600.0, flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(200.0, flowResult.getCommercialFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(400.0, flowResult.getLoopFlow(preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(400.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Commercial Flow added first, then Flow: loopFlow = flow - commercialFlow")
        void testCommercialFlowThenFlowCalculatesLoopFlow() {
            flowResult.addCommercialFlowMeasurement(250.0, curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE);
            assertTrue(Double.isNaN(flowResult.getLoopFlow(curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE)));

            flowResult.addFlowMeasurement(800.0, curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE);

            assertEquals(800.0, flowResult.getFlow(curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE));
            assertEquals(250.0, flowResult.getCommercialFlow(curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE));
            assertEquals(550.0, flowResult.getLoopFlow(curativeInstant, flowCnec, TwoSides.TWO, Unit.AMPERE));
            // Margin: bounds [-2000, 2000], flow = 800 -> min(2800, 1200) = 1200.0
            assertEquals(1200.0, flowResult.getMargin(curativeInstant, flowCnec, Unit.AMPERE));
        }

        @Test
        @DisplayName("Updating flow or commercialFlow recalculates loopFlow correctly")
        void testLoopFlowUpdatesWhenFlowOrCommercialFlowUpdated() {
            flowResult.addFlowMeasurement(500.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(300.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(200.0, flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));

            // Update commercial flow
            flowResult.addCommercialFlowMeasurement(450.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(50.0, flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));

            // Update flow
            flowResult.addFlowMeasurement(700.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(250.0, flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Flow on side ONE and commercial flow on side TWO do NOT pair up for loopFlow")
        void testMismatchedSidesDoNotComputeLoopFlow() {
            flowResult.addFlowMeasurement(500.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(200.0, null, flowCnec, TwoSides.TWO, Unit.MEGAWATT);

            assertTrue(Double.isNaN(flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getLoopFlow(null, flowCnec, TwoSides.TWO, Unit.MEGAWATT)));
        }

        @Test
        @DisplayName("Flow in MW and commercial flow in AMPERE do NOT pair up for loopFlow")
        void testMismatchedUnitsDoNotComputeLoopFlow() {
            flowResult.addFlowMeasurement(500.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.AMPERE);

            assertTrue(Double.isNaN(flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT)));
            assertTrue(Double.isNaN(flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.AMPERE)));
        }
    }

    @Nested
    @DisplayName("PTDF Zonal Sum & Relative Margin Tests")
    class PtdfAndRelativeMarginTests {

        @Test
        @DisplayName("Add PTDF zonal sum measurement and get PTDF zonal sum")
        void testAddPtdfZonalSum() {
            flowResult.addPtdfZonalSumMeasurement(0.25, preventiveInstant, flowCnec, TwoSides.ONE);
            flowResult.addPtdfZonalSumMeasurement(0.35, preventiveInstant, flowCnec, TwoSides.TWO);

            assertEquals(0.25, flowResult.getPtdfZonalSum(preventiveInstant, flowCnec, TwoSides.ONE));
            assertEquals(0.35, flowResult.getPtdfZonalSum(preventiveInstant, flowCnec, TwoSides.TWO));
            assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(null, flowCnec, TwoSides.ONE)));
            assertTrue(Double.isNaN(flowResult.getPtdfZonalSum(curativeInstant, flowCnec, TwoSides.ONE)));
        }

        @Test
        @DisplayName("Flow added first (positive margin), then PTDF zonal sum -> relativeMargin = margin / ptdfZonalSum")
        void testFlowThenPtdfZonalSumCalculatesRelativeMargin() {
            // Flow = 200 MW -> margin = 800 MW
            flowResult.addFlowMeasurement(200.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertTrue(Double.isNaN(flowResult.getRelativeMargin(preventiveInstant, flowCnec, Unit.MEGAWATT)));

            // Add PTDF sum = 0.5 on side ONE
            flowResult.addPtdfZonalSumMeasurement(0.5, preventiveInstant, flowCnec, TwoSides.ONE);

            // relativeMargin = 800 / 0.5 = 1600.0
            assertEquals(1600.0, flowResult.getRelativeMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("PTDF zonal sum added first, then Flow (positive margin) -> relativeMargin = margin / ptdfZonalSum")
        void testPtdfZonalSumThenFlowCalculatesRelativeMargin() {
            flowResult.addPtdfZonalSumMeasurement(0.2, curativeInstant, flowCnec, TwoSides.TWO);
            assertTrue(Double.isNaN(flowResult.getRelativeMargin(curativeInstant, flowCnec, Unit.MEGAWATT)));

            // Flow = 500 MW on side TWO -> margin = 500 MW
            flowResult.addFlowMeasurement(500.0, curativeInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT);

            // relativeMargin = 500 / 0.2 = 2500.0
            assertEquals(2500.0, flowResult.getRelativeMargin(curativeInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Relative margin with negative margin (overload) returns raw negative margin")
        void testRelativeMarginWithNegativeMargin() {
            // Flow = 1200 MW -> margin = -200 MW (<= 0)
            flowResult.addFlowMeasurement(1200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.4, null, flowCnec, TwoSides.ONE);

            // When margin <= 0, relative margin is margin directly (-200.0)
            assertEquals(-200.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Relative margin with zero margin returns 0.0")
        void testRelativeMarginWithZeroMargin() {
            // Flow = 1000 MW -> margin = 0.0 (<= 0)
            flowResult.addFlowMeasurement(1000.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.5, null, flowCnec, TwoSides.ONE);

            assertEquals(0.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Relative margin updates when margin gets updated with new more constraining flow")
        void testRelativeMarginUpdatesWhenMarginChanges() {
            flowResult.addPtdfZonalSumMeasurement(0.5, preventiveInstant, flowCnec, TwoSides.ONE);

            // Flow 1: 200 MW -> margin = 800 MW -> relativeMargin = 800 / 0.5 = 1600.0
            flowResult.addFlowMeasurement(200.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(1600.0, flowResult.getRelativeMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));

            // Flow 2: 800 MW -> margin = 200 MW -> relativeMargin = 200 / 0.5 = 400.0
            flowResult.addFlowMeasurement(800.0, preventiveInstant, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            assertEquals(400.0, flowResult.getRelativeMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("Relative margin updates when PTDF zonal sum changes")
        void testRelativeMarginUpdatesWhenPtdfChanges() {
            flowResult.addFlowMeasurement(400.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT); // margin = 600 MW
            flowResult.addPtdfZonalSumMeasurement(0.5, null, flowCnec, TwoSides.ONE);
            assertEquals(1200.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));

            // Update PTDF sum to 0.25 -> relativeMargin = 600 / 0.25 = 2400.0
            flowResult.addPtdfZonalSumMeasurement(0.25, null, flowCnec, TwoSides.ONE);
            assertEquals(2400.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));
        }

        @Test
        @DisplayName("PTDF update applies to both MEGAWATT and AMPERE when margins exist for both")
        void testPtdfUpdateAffectsBothUnits() {
            flowResult.addFlowMeasurement(200.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT); // margin MW = 800
            flowResult.addFlowMeasurement(500.0, null, flowCnec, TwoSides.ONE, Unit.AMPERE);   // margin A = 1500

            flowResult.addPtdfZonalSumMeasurement(0.5, null, flowCnec, TwoSides.ONE);

            assertEquals(1600.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));
            assertEquals(3000.0, flowResult.getRelativeMargin(null, flowCnec, Unit.AMPERE));
        }
    }

    @Nested
    @DisplayName("Comprehensive Combinations & Edge Cases")
    class ComprehensiveCombinationsTests {

        @Test
        @DisplayName("Complete workflow with all measurements per instant and side")
        void testCompleteWorkflow() {
            // Null instant
            flowResult.addFlowMeasurement(100.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(50.0, null, flowCnec, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.2, null, flowCnec, TwoSides.ONE);

            // Preventive instant
            flowResult.addFlowMeasurement(300.0, preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(100.0, preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.4, preventiveInstant, flowCnec, TwoSides.TWO);

            // Curative instant
            flowResult.addCommercialFlowMeasurement(80.0, curativeInstant, flowCnec, TwoSides.ONE, Unit.AMPERE);
            flowResult.addFlowMeasurement(400.0, curativeInstant, flowCnec, TwoSides.ONE, Unit.AMPERE);
            flowResult.addPtdfZonalSumMeasurement(0.8, curativeInstant, flowCnec, TwoSides.ONE);

            // Assert Null instant
            assertEquals(100.0, flowResult.getFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(50.0, flowResult.getCommercialFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(50.0, flowResult.getLoopFlow(null, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(900.0, flowResult.getMargin(null, flowCnec, Unit.MEGAWATT));
            assertEquals(0.2, flowResult.getPtdfZonalSum(null, flowCnec, TwoSides.ONE));
            assertEquals(4500.0, flowResult.getRelativeMargin(null, flowCnec, Unit.MEGAWATT));

            // Assert Preventive instant
            assertEquals(300.0, flowResult.getFlow(preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
            assertEquals(100.0, flowResult.getCommercialFlow(preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
            assertEquals(200.0, flowResult.getLoopFlow(preventiveInstant, flowCnec, TwoSides.TWO, Unit.MEGAWATT));
            assertEquals(700.0, flowResult.getMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));
            assertEquals(0.4, flowResult.getPtdfZonalSum(preventiveInstant, flowCnec, TwoSides.TWO));
            assertEquals(1750.0, flowResult.getRelativeMargin(preventiveInstant, flowCnec, Unit.MEGAWATT));

            // Assert Curative instant
            assertEquals(400.0, flowResult.getFlow(curativeInstant, flowCnec, TwoSides.ONE, Unit.AMPERE));
            assertEquals(80.0, flowResult.getCommercialFlow(curativeInstant, flowCnec, TwoSides.ONE, Unit.AMPERE));
            assertEquals(320.0, flowResult.getLoopFlow(curativeInstant, flowCnec, TwoSides.ONE, Unit.AMPERE));
            assertEquals(1600.0, flowResult.getMargin(curativeInstant, flowCnec, Unit.AMPERE));
            assertEquals(0.8, flowResult.getPtdfZonalSum(curativeInstant, flowCnec, TwoSides.ONE));
            assertEquals(2000.0, flowResult.getRelativeMargin(curativeInstant, flowCnec, Unit.AMPERE));
        }

        @Test
        @DisplayName("Multiple FlowCnecs handled independently without collision")
        void testMultipleFlowCnecsIndependent() {
            FlowCnec cnec1 = mock(FlowCnec.class);
            FlowCnec cnec2 = mock(FlowCnec.class);

            when(cnec1.getLowerBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(-100.0));
            when(cnec1.getUpperBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(100.0));
            when(cnec2.getLowerBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(-500.0));
            when(cnec2.getUpperBound(Mockito.any(), Mockito.any())).thenReturn(Optional.of(500.0));

            flowResult.addFlowMeasurement(80.0, null, cnec1, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(30.0, null, cnec1, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.5, null, cnec1, TwoSides.ONE);

            flowResult.addFlowMeasurement(200.0, null, cnec2, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addCommercialFlowMeasurement(150.0, null, cnec2, TwoSides.ONE, Unit.MEGAWATT);
            flowResult.addPtdfZonalSumMeasurement(0.2, null, cnec2, TwoSides.ONE);

            // Cnec 1
            assertEquals(80.0, flowResult.getFlow(null, cnec1, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(30.0, flowResult.getCommercialFlow(null, cnec1, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(50.0, flowResult.getLoopFlow(null, cnec1, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(20.0, flowResult.getMargin(null, cnec1, Unit.MEGAWATT));
            assertEquals(40.0, flowResult.getRelativeMargin(null, cnec1, Unit.MEGAWATT));

            // Cnec 2
            assertEquals(200.0, flowResult.getFlow(null, cnec2, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(150.0, flowResult.getCommercialFlow(null, cnec2, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(50.0, flowResult.getLoopFlow(null, cnec2, TwoSides.ONE, Unit.MEGAWATT));
            assertEquals(300.0, flowResult.getMargin(null, cnec2, Unit.MEGAWATT));
            assertEquals(1500.0, flowResult.getRelativeMargin(null, cnec2, Unit.MEGAWATT));
        }
    }
}
