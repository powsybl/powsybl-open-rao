/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RaoResultTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private Instant instant;
    private State state;
    private FlowCnec flowCnec;
    private AngleCnec angleCnec;
    private VoltageCnec voltageCnec;
    private MockRaoResult raoResult;

    @BeforeEach
    void setUp() {
        instant = Mockito.mock(Instant.class);
        state = Mockito.mock(State.class);

        flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnec.getLowerBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(-1000.));
        Mockito.when(flowCnec.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));

        angleCnec = Mockito.mock(AngleCnec.class);
        Mockito.when(angleCnec.getLowerBound(Unit.DEGREE)).thenReturn(Optional.of(-10.));
        Mockito.when(angleCnec.getUpperBound(Unit.DEGREE)).thenReturn(Optional.of(10.));

        voltageCnec = Mockito.mock(VoltageCnec.class);
        Mockito.when(voltageCnec.getLowerBound(Unit.KILOVOLT)).thenReturn(Optional.of(380.));
        Mockito.when(voltageCnec.getUpperBound(Unit.KILOVOLT)).thenReturn(Optional.of(420.));

        raoResult = new MockRaoResult();
    }

    @Test
    void testMetadataMethods() {
        raoResult.addExtension(Metadata.class, Metadata.builder()
            .addComputationStatus(null, ComputationStatus.DEFAULT)
            .addComputationStatus(state, ComputationStatus.FAILURE)
            .withExecutionDetails("details")
            .build());
        assertEquals(ComputationStatus.PARTIAL_FAILURE, raoResult.getComputationStatus());
        assertEquals(ComputationStatus.FAILURE, raoResult.getComputationStatus(state));
        assertEquals("details", raoResult.getExecutionDetails());
        raoResult.setExecutionDetails("new details");
        assertEquals("new details", raoResult.getExecutionDetails());
        assertEquals(ComputationStatus.FAILURE, raoResult.getComputationStatus(state));
    }

    @Test
    void testFlowResultMethods() {
        raoResult.addExtension(FlowResult.class, FlowResult.builder()
            .addFlowMeasurement(800., instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)
            .addCommercialFlowMeasurement(500., instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT)
            .addPtdfZonalSumMeasurement(0.5, instant, flowCnec, TwoSides.ONE)
            .build());
        assertEquals(800., raoResult.getFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(200., raoResult.getMargin(instant, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(400., raoResult.getRelativeMargin(instant, flowCnec, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(500., raoResult.getCommercialFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(300., raoResult.getLoopFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0.5, raoResult.getPtdfZonalSum(instant, flowCnec, TwoSides.ONE), DOUBLE_TOLERANCE);
    }

    @Test
    void testAngleResultMethods() {
        raoResult.addExtension(AngleResult.class, AngleResult.builder()
            .addMeasurement(7., instant, angleCnec, Unit.DEGREE)
            .build());
        assertEquals(7., raoResult.getAngle(instant, angleCnec, Unit.DEGREE), DOUBLE_TOLERANCE);
        assertEquals(3., raoResult.getMargin(instant, angleCnec, Unit.DEGREE), DOUBLE_TOLERANCE);
    }

    @Test
    void testVoltageResultMethods() {
        raoResult.addExtension(VoltageResult.class, VoltageResult.builder()
            .addMeasurement(390., 415., instant, voltageCnec, Unit.KILOVOLT)
            .build());
        assertEquals(390., raoResult.getMinVoltage(instant, voltageCnec, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(415., raoResult.getMaxVoltage(instant, voltageCnec, Unit.KILOVOLT), DOUBLE_TOLERANCE);
        assertEquals(5., raoResult.getMargin(instant, voltageCnec, Unit.KILOVOLT), DOUBLE_TOLERANCE);
    }

    @Test
    void testCostResultMethods() {
        raoResult.addExtension(CostResult.class, CostResult.builder()
            .addFunctionalCostResult(instant, 100.)
            .addVirtualCostResult(instant, "cost-1", 10.)
            .addVirtualCostResult(instant, "cost-2", 20.)
            .build());
        assertEquals(130., raoResult.getCost(instant), DOUBLE_TOLERANCE);
        assertEquals(100., raoResult.getFunctionalCost(instant), DOUBLE_TOLERANCE);
        assertEquals(30., raoResult.getVirtualCost(instant), DOUBLE_TOLERANCE);
        assertEquals(Set.of("cost-1", "cost-2"), raoResult.getVirtualCostNames());
        assertEquals(10., raoResult.getVirtualCost(instant, "cost-1"), DOUBLE_TOLERANCE);
        assertEquals(20., raoResult.getVirtualCost(instant, "cost-2"), DOUBLE_TOLERANCE);
    }

    @Test
    void testMissingExtensions() {
        assertMissingExtension("a Metadata", () -> raoResult.getComputationStatus());
        assertMissingExtension("a Metadata", () -> raoResult.getComputationStatus(state));
        assertMissingExtension("a Metadata", () -> raoResult.getExecutionDetails());
        assertMissingExtension("a Metadata", () -> raoResult.setExecutionDetails("details"));
        assertMissingExtension("a FlowResult", () -> raoResult.getFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
        assertMissingExtension("a FlowResult", () -> raoResult.getMargin(instant, flowCnec, Unit.MEGAWATT));
        assertMissingExtension("a FlowResult", () -> raoResult.getRelativeMargin(instant, flowCnec, Unit.MEGAWATT));
        assertMissingExtension("a FlowResult", () -> raoResult.getCommercialFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
        assertMissingExtension("a FlowResult", () -> raoResult.getLoopFlow(instant, flowCnec, TwoSides.ONE, Unit.MEGAWATT));
        assertMissingExtension("a FlowResult", () -> raoResult.getPtdfZonalSum(instant, flowCnec, TwoSides.ONE));
        assertMissingExtension("an AngleResult", () -> raoResult.getAngle(instant, angleCnec, Unit.DEGREE));
        assertMissingExtension("an AngleResult", () -> raoResult.getMargin(instant, angleCnec, Unit.DEGREE));
        assertMissingExtension("a VoltageResult", () -> raoResult.getMinVoltage(instant, voltageCnec, Unit.KILOVOLT));
        assertMissingExtension("a VoltageResult", () -> raoResult.getMaxVoltage(instant, voltageCnec, Unit.KILOVOLT));
        assertMissingExtension("a VoltageResult", () -> raoResult.getMargin(instant, voltageCnec, Unit.KILOVOLT));
        assertMissingExtension("a CostResult", () -> raoResult.getCost(instant));
        assertMissingExtension("a CostResult", () -> raoResult.getFunctionalCost(instant));
        assertMissingExtension("a CostResult", () -> raoResult.getVirtualCost(instant));
        assertMissingExtension("a CostResult", () -> raoResult.getVirtualCostNames());
        assertMissingExtension("a CostResult", () -> raoResult.getVirtualCost(instant, "cost-1"));
    }

    private static void assertMissingExtension(String articleAndExtensionName, org.junit.jupiter.api.function.Executable executable) {
        OpenRaoException exception = assertThrows(OpenRaoException.class, executable);
        assertEquals("RaoResult does not contain " + articleAndExtensionName + " extension.", exception.getMessage());
    }
}
