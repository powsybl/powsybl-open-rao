/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.loopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ObjectiveFunctionTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private FlowResult flowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;

    @BeforeEach
    public void setUp() {
        flowResult = Mockito.mock(FlowResult.class);
        cnec1 = Mockito.mock(FlowCnec.class);
        cnec2 = Mockito.mock(FlowCnec.class);

        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        when(cnec1.isOptimized()).thenReturn(true);
        when(cnec1.isMonitored()).thenReturn(true);
        when(cnec2.isOptimized()).thenReturn(true);
        when(cnec2.isMonitored()).thenReturn(false);
        when(cnec1.getState()).thenReturn(state);
        when(cnec2.getState()).thenReturn(state);
        when(cnec2.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE, TwoSides.TWO));

        when(flowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(-300.);
        when(flowResult.getMargin(cnec2, Unit.MEGAWATT)).thenReturn(400.);
    }

    @Test
    void testWithFunctionalCostOnly() {
        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters openRaoSearchTreeParameters = new OpenRaoSearchTreeParameters();
        openRaoSearchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(0.0);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, openRaoSearchTreeParameters);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(Set.of(cnec1, cnec2), Set.of(), null, null, Set.of(), raoParameters, Set.of());

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, null);
        assertEquals(300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(300., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertTrue(result.getVirtualCostNames().isEmpty());
    }

    @Test
    void testWithFunctionalAndVirtualCost() {
        RaoParameters raoParameters = new RaoParameters();
        OpenRaoSearchTreeParameters openRaoSearchTreeParameters = new OpenRaoSearchTreeParameters();
        openRaoSearchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(0.0);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, openRaoSearchTreeParameters);
        MnecParameters mnecParameters = new MnecParameters();
        mnecParameters.setAcceptableMarginDecrease(200.0);
        raoParameters.setMnecParameters(mnecParameters);
        openRaoSearchTreeParameters.setMnecParameters(new SearchTreeRaoMnecParameters());
        raoParameters.setLoopFlowParameters(new LoopFlowParameters());
        SearchTreeRaoLoopFlowParameters loopFlowParameters = new SearchTreeRaoLoopFlowParameters();
        loopFlowParameters.setViolationCost(10.);
        openRaoSearchTreeParameters.setLoopFlowParameters(loopFlowParameters);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);

        when(initialFlowResult.getMargin(cnec1, Unit.MEGAWATT)).thenReturn(200.);
        when(flowResult.getLoopFlow(cnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(10.);
        when(flowResult.getLoopFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(10.);

        LoopFlowThreshold loopFlowThreshold = Mockito.mock(LoopFlowThreshold.class);
        when(loopFlowThreshold.getThreshold(Unit.MEGAWATT)).thenReturn(0.0);
        when(cnec2.getExtension(LoopFlowThreshold.class)).thenReturn(loopFlowThreshold);

        when(initialFlowResult.getLoopFlow(cnec2, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(0.0);
        when(initialFlowResult.getLoopFlow(cnec2, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(0.0);

        FlowResult prePerimeterFlowResult = Mockito.mock(FlowResult.class);

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(Set.of(cnec1, cnec2), Set.of(cnec2), initialFlowResult, prePerimeterFlowResult, Set.of(), raoParameters, Set.of());

        assertEquals(3000.0, objectiveFunction.evaluate(flowResult, null).getVirtualCost("mnec-cost"));
        assertEquals(List.of(cnec1), objectiveFunction.evaluate(flowResult, null).getCostlyElements("mnec-cost", 1));

        assertEquals(100., objectiveFunction.evaluate(flowResult, null).getVirtualCost("loop-flow-cost"));
        assertEquals(List.of(cnec2), objectiveFunction.evaluate(flowResult, null).getCostlyElements("loop-flow-cost", 1));

        // ObjectiveFunctionResult
        ObjectiveFunctionResult result = objectiveFunction.evaluate(flowResult, null);
        assertEquals(300., result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(3100., result.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(3400., result.getCost(), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1, cnec2), result.getMostLimitingElements(10));
        assertEquals(2, result.getVirtualCostNames().size());
        assertTrue(result.getVirtualCostNames().containsAll(Set.of("mnec-cost", "loop-flow-cost")));
        assertEquals(3000., result.getVirtualCost("mnec-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec1), result.getCostlyElements("mnec-cost", 10));
        assertEquals(100., result.getVirtualCost("loop-flow-cost"), DOUBLE_TOLERANCE);
        assertEquals(List.of(cnec2), result.getCostlyElements("loop-flow-cost", 10));
    }

    @Test
    void testBuildForInitialSensitivityComputation() {
        RaoParameters raoParameters = new RaoParameters();

        OpenRaoSearchTreeParameters openRaoSearchTreeParameters = new OpenRaoSearchTreeParameters();
        openRaoSearchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(1.0);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, openRaoSearchTreeParameters);
        ObjectiveFunction objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(
            Set.of(cnec1, cnec2), raoParameters, Set.of()
        );
        assertNotNull(objectiveFunction);
        assertEquals(Set.of("sensitivity-failure-cost"), objectiveFunction.evaluate(flowResult, null).getVirtualCostNames());
    }

    @Test
    void testBuildForInitialSensitivityComputationCostlyOptimizationAmpere() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.AMPERE);
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setMinMarginsParameters(new SearchTreeRaoCostlyMinMarginParameters());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(Set.of(), raoParameters, Set.of());
        assertNotNull(objectiveFunction);
        assertTrue(objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(new RangeActionSetpointResultImpl(Map.of()))).getVirtualCostNames().contains("min-margin-violation-evaluator"));
    }

    @Test
    void testBuildForInitialSensitivityComputationCostlyOptimizationMegawatt() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.MEGAWATT);
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setMinMarginsParameters(new SearchTreeRaoCostlyMinMarginParameters());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(Set.of(), raoParameters, Set.of());
        assertNotNull(objectiveFunction);
        assertTrue(objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(new RangeActionSetpointResultImpl(Map.of()))).getVirtualCostNames().contains("min-margin-violation-evaluator"));
    }

    @Test
    void testBuildCostlyOptimizationAmpere() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.AMPERE);
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setMinMarginsParameters(new SearchTreeRaoCostlyMinMarginParameters());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(Set.of(), Set.of(), null, null, Set.of(), raoParameters, Set.of());
        assertNotNull(objectiveFunction);
        assertTrue(objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(new RangeActionSetpointResultImpl(Map.of()))).getVirtualCostNames().contains("min-margin-violation-evaluator"));
    }

    @Test
    void testBuildCostlyOptimizationMegawatt() {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.MEGAWATT);
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        assertTrue(raoParameters.getObjectiveFunctionParameters().getType().costOptimization());
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        raoParameters.getExtension(OpenRaoSearchTreeParameters.class).setMinMarginsParameters(new SearchTreeRaoCostlyMinMarginParameters());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(Set.of(), Set.of(), null, null, Set.of(), raoParameters, Set.of());
        assertNotNull(objectiveFunction);
        assertTrue(objectiveFunction.evaluate(flowResult, RemedialActionActivationResultImpl.empty(new RangeActionSetpointResultImpl(Map.of()))).getVirtualCostNames().contains("min-margin-violation-evaluator"));
    }
}
