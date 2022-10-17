/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_loopflow_extension.LoopFlowThresholdAdder;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.LoopFlowParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class MaxLoopFlowFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private MaxLoopFlowFiller maxLoopFlowFiller;
    private LoopFlowParameters loopFlowParameters;
    private FlowCnec cnecOn2sides;

    @Before
    public void setUp() {
        init();

        cnecOn2sides = crac.newFlowCnec()
            .withId("cnec-2-sides")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withSide(Side.LEFT).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withSide(Side.RIGHT).add()
            .add();
        cnecOn2sides.newExtension(LoopFlowThresholdAdder.class).withValue(100.).withUnit(Unit.MEGAWATT).add();

        State state = crac.getPreventiveState();
        double initialAlpha = network.getTwoWindingsTransformer(RANGE_ACTION_ELEMENT_ID).getPhaseTapChanger().getCurrentStep().getAlpha();
        RangeActionSetpointResult initialRangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(pstRangeAction, initialAlpha));
        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getFlowCnecs()).thenReturn(Set.of(cnec1, cnecOn2sides));

        Map<State, Set<RangeAction<?>>> rangeActions = new HashMap<>();
        rangeActions.put(state, Set.of(pstRangeAction));
        Mockito.when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(rangeActions);

        RangeActionParameters rangeActionParameters = RangeActionParameters.buildFromRaoParameters(new RaoParameters());
        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            new RangeActionActivationResultImpl(initialRangeActionSetpointResult),
            rangeActionParameters,
            Unit.MEGAWATT
        );
        cnec1.newExtension(LoopFlowThresholdAdder.class).withValue(100.).withUnit(Unit.MEGAWATT).add();
    }

    private void createMaxLoopFlowFiller(double initialLoopFlowValue) {
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getLoopFlow(cnec1, Side.LEFT, Unit.MEGAWATT)).thenReturn(initialLoopFlowValue);
        maxLoopFlowFiller = new MaxLoopFlowFiller(
                Set.of(cnec1),
                initialFlowResult,
                loopFlowParameters
        );
    }

    private void setCommercialFlowValue(double commercialFlowValue) {
        when(flowResult.getCommercialFlow(cnec1, Side.LEFT, Unit.MEGAWATT)).thenReturn(commercialFlowValue);
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxLoopFlowFiller)
            .withSolver(mpSolver)
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void updateLinearProblem() {
        linearProblem.updateBetweenSensiIteration(flowResult, sensitivityResult, Mockito.mock(RangeActionActivationResultImpl.class));
    }

    @Test
    public void testFill1() {
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                13,
                10,
                5);
        createMaxLoopFlowFiller(0);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // violation cost
        assertEquals(10., linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnec1, Side.LEFT)), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFill2() {
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
                30,
                10,
                5);
        createMaxLoopFlowFiller(80);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(110 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((110 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShouldUpdate() {
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST,
                0,
                10,
                5);
        createMaxLoopFlowFiller(0);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // update loop-flow value
        setCommercialFlowValue(67);
        updateLinearProblem();

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 67.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 67.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnec1, Side.LEFT);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void testShouldNotUpdate() {
        loopFlowParameters = new LoopFlowParameters(
                RaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO,
                0,
                10,
                5);
        createMaxLoopFlowFiller(0);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // update loop-flow value
        setCommercialFlowValue(67);
        updateLinearProblem();

        // check flow constraint for cnec1
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, Side.LEFT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testFill2Sides() {
        loopFlowParameters = new LoopFlowParameters(
            RaoParameters.LoopFlowApproximationLevel.FIXED_PTDF,
            13,
            10,
            5);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getLoopFlow(cnecOn2sides, Side.LEFT, Unit.MEGAWATT)).thenReturn(0.);
        when(initialFlowResult.getLoopFlow(cnecOn2sides, Side.RIGHT, Unit.MEGAWATT)).thenReturn(10.);
        maxLoopFlowFiller = new MaxLoopFlowFiller(
            Set.of(cnecOn2sides),
            initialFlowResult,
            loopFlowParameters
        );

        when(flowResult.getCommercialFlow(cnecOn2sides, Side.LEFT, Unit.MEGAWATT)).thenReturn(49.);
        when(flowResult.getCommercialFlow(cnecOn2sides, Side.RIGHT, Unit.MEGAWATT)).thenReturn(69.);

        buildLinearProblem();

        // Check left side
        MPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, Side.LEFT, LinearProblem.BoundExtension.UPPER_BOUND);
        MPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, Side.LEFT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        MPVariable flowVariable = linearProblem.getFlowVariable(cnecOn2sides, Side.LEFT);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // Check right side
        loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, Side.RIGHT, LinearProblem.BoundExtension.UPPER_BOUND);
        loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, Side.RIGHT, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 69.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 69.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        flowVariable = linearProblem.getFlowVariable(cnecOn2sides, Side.RIGHT);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // violation cost
        assertEquals(10. / 2, linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnecOn2sides, Side.LEFT)), DOUBLE_TOLERANCE);
        assertEquals(10. / 2, linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnecOn2sides, Side.RIGHT)), DOUBLE_TOLERANCE);
    }
}
