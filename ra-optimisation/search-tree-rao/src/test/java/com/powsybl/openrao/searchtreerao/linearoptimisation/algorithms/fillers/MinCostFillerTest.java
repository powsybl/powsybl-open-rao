/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jeremy Wang{@literal <jeremy.wang at rte-france.com>}
 */
class MinCostFillerTest extends AbstractFillerTest {
    private static final double PST_ACTIVATION_COST = 1.5;
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private MinCostFiller minCostFiller;

    @BeforeEach
    public void setUp() throws IOException {
        init();
        network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().setTapPosition(TAP_INITIAL);
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec1));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(0.01);
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(0.01);
        RangeActionsOptimizationParameters rangeActionParameters = RangeActionsOptimizationParameters.buildFromRaoParameters(raoParameters);

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT,
            false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
    }

    private void createMinCostFiller() {
        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(cnec1.getState(), Set.of(pstRangeAction));
        minCostFiller = new MinCostFiller(Set.of(cnec1), rangeActions);
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(minCostFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    @Test
    void fillWithMinCostMarginConstraint() {
        createMinCostFiller();
        buildLinearProblem();

        OpenRaoMPVariable flowCnec1 = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        OpenRaoMPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, cnec1.getState());

        // check minimum margin variable
        OpenRaoMPVariable minimumMargin = linearProblem.getMinimumMarginVariable();
        assertNotNull(minimumMargin);

        // check minimum margin constraints
        OpenRaoMPConstraint cnec1AboveThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        OpenRaoMPConstraint cnec1BelowThreshold = linearProblem.getMinimumMarginConstraint(cnec1, TwoSides.ONE, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(cnec1AboveThreshold);
        assertNotNull(cnec1BelowThreshold);
        assertEquals(-linearProblem.infinity(), cnec1BelowThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(-MIN_FLOW_1, cnec1BelowThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-linearProblem.infinity(), cnec1AboveThreshold.lb(), linearProblem.infinity() * 1e-3);
        assertEquals(MAX_FLOW_1, cnec1AboveThreshold.ub(), DOUBLE_TOLERANCE);
        assertEquals(-1, cnec1BelowThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(flowCnec1), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1BelowThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);
        assertEquals(1, cnec1AboveThreshold.getCoefficient(minimumMargin), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(0.01, linearProblem.getObjective().getCoefficient(absoluteVariation), DOUBLE_TOLERANCE); // penalty cost
        // 1000 is for the arbitrary penalty coefficient
        assertEquals(-1000, linearProblem.getObjective().getCoefficient(minimumMargin), DOUBLE_TOLERANCE); // penalty cost if unsecure
        assertTrue(linearProblem.minimization());
    }

    @Test
    void fillWithMinCostTotalCostConstraints() {
        createMinCostFiller();
        buildLinearProblem();

        OpenRaoMPVariable absoluteVariation = linearProblem.getAbsoluteRangeActionVariationVariable(pstRangeAction, cnec1.getState());

        // check rangeAction cost variable
        OpenRaoMPVariable rangeActionCostVariable = linearProblem.getRangeActionCostVariable(pstRangeAction, cnec1.getState());
        assertNotNull(rangeActionCostVariable);
        // check total cost variable
        OpenRaoMPVariable totalCostVariable = linearProblem.getTotalCostVariable();
        assertNotNull(totalCostVariable);

        // check rangeAction cost constraint
        OpenRaoMPConstraint rangeActionCostConstraint = linearProblem.getRangeActionCostConstraint(pstRangeAction, cnec1.getState());
        assertNotNull(rangeActionCostConstraint);
        assertEquals(1, rangeActionCostConstraint.getCoefficient(rangeActionCostVariable), DOUBLE_TOLERANCE);
        assertEquals(-PST_ACTIVATION_COST, rangeActionCostConstraint.getCoefficient(absoluteVariation), DOUBLE_TOLERANCE);
        assertEquals(0.0, rangeActionCostConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(0.0, rangeActionCostConstraint.ub(), DOUBLE_TOLERANCE);

        // check total cost constraint
        OpenRaoMPConstraint totalCostConstraint = linearProblem.getTotalCostConstraint();
        assertNotNull(totalCostConstraint);
        assertEquals(1, totalCostConstraint.getCoefficient(totalCostVariable), DOUBLE_TOLERANCE);
        assertEquals(-1, totalCostConstraint.getCoefficient(rangeActionCostVariable), DOUBLE_TOLERANCE);
        assertEquals(0.0, totalCostConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(0.0, totalCostConstraint.ub(), DOUBLE_TOLERANCE);

        // check objective
        assertEquals(1, linearProblem.getObjective().getCoefficient(totalCostVariable), DOUBLE_TOLERANCE);

        // check the number of variables and constraints
        // total number of variables 6 :
        //      - 3 due to CoreFiller
        //      - minimum margin variable
        //      - total cost variable
        //      - 1 range action cost variable
        // total number of constraints 7 :
        //      - 3 due to CoreFiller
        //      - 2 per CNEC (min margin constraints)
        //      - 1 per range action (range action cost constraint)
        //      - total cost constraint
        assertEquals(6, linearProblem.numVariables());
        assertEquals(7, linearProblem.numConstraints());
    }

    @Test
    void fillWithMissingFlowVariables() {
        createMinCostFiller();
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(minCostFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();

        // AbsoluteRangeActionVariables present, but no the FlowVariables
        linearProblem.addAbsoluteRangeActionVariationVariable(0.0, 0.0, pstRangeAction, cnec1.getState());
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.fill(flowResult, sensitivityResult));
        assertEquals("Variable Tieline BE FR - N - preventive_one_flow_variable has not been created yet", e.getMessage());
    }

    @Test
    void fillWithMissingRangeActionVariables() {
        createMinCostFiller();
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(minCostFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .build();

        // FlowVariables present , but not the absoluteRangeActionVariables present,
        // This should work since range actions can be filtered out by the CoreProblemFiller if their number
        // exceeds the max-pst-per-tso parameter
        linearProblem.addFlowVariable(0.0, 0.0, cnec1, TwoSides.ONE);
        linearProblem.addFlowVariable(0.0, 0.0, cnec2, TwoSides.TWO);
        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.fill(flowResult, sensitivityResult));
        assertEquals("Variable PRA_PST_BE_preventive_absolutevariation_variable has not been created yet", e.getMessage());
    }
}

