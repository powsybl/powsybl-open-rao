/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThresholdAdder;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
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
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot{@literal <baptiste.seguinot at rte-france.com>}
 */
class MaxLoopFlowFillerTest extends AbstractFillerTest {
    private LinearProblem linearProblem;
    private CoreProblemFiller coreProblemFiller;
    private MaxLoopFlowFiller maxLoopFlowFiller;
    private LoopFlowParametersExtension loopFlowParameters;
    private FlowCnec cnecOn2sides;

    @BeforeEach
    public void setUp() throws IOException {
        init();

        cnecOn2sides = crac.newFlowCnec()
            .withId("cnec-2-sides")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withSide(TwoSides.ONE).add()
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withSide(TwoSides.TWO).add()
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

        coreProblemFiller = new CoreProblemFiller(
            optimizationPerimeter,
            initialRangeActionSetpointResult,
            (new RaoParameters()).getRangeActionsOptimizationParameters(),
            null,
            Unit.MEGAWATT,
            false, RangeActionsOptimizationParameters.PstModel.CONTINUOUS
        );
        cnec1.newExtension(LoopFlowThresholdAdder.class).withValue(100.).withUnit(Unit.MEGAWATT).add();
    }

    private void createMaxLoopFlowFiller(double initialLoopFlowValue) {
        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getLoopFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(initialLoopFlowValue);
        maxLoopFlowFiller = new MaxLoopFlowFiller(
                Set.of(cnec1),
                initialFlowResult,
                loopFlowParameters
        );
    }

    private void setCommercialFlowValue(double commercialFlowValue) {
        when(flowResult.getCommercialFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(commercialFlowValue);
    }

    private void buildLinearProblem() {
        linearProblem = new LinearProblemBuilder()
            .withProblemFiller(coreProblemFiller)
            .withProblemFiller(maxLoopFlowFiller)
            .withSolver(RangeActionsOptimizationParameters.Solver.SCIP)
            .withInitialRangeActionActivationResult(getInitialRangeActionActivationResult())
            .build();
        linearProblem.fill(flowResult, sensitivityResult);
    }

    private void updateLinearProblem() {
        linearProblem.updateBetweenSensiIteration(flowResult, sensitivityResult, Mockito.mock(RangeActionActivationResultImpl.class));
    }

    @Test
    void testFill1() {
        loopFlowParameters = new LoopFlowParametersExtension();
        loopFlowParameters.setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        loopFlowParameters.setAcceptableIncrease(13);
        loopFlowParameters.setViolationCost(10);
        loopFlowParameters.setConstraintAdjustmentCoefficient(5);

        createMaxLoopFlowFiller(0);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // check flow constraint for cnec1
        OpenRaoMPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND);
        OpenRaoMPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // violation cost
        assertEquals(10., linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnec1, TwoSides.ONE)), DOUBLE_TOLERANCE);
    }

    @Test
    void testFill2() {
        loopFlowParameters = new LoopFlowParametersExtension();
        loopFlowParameters.setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        loopFlowParameters.setAcceptableIncrease(30);
        loopFlowParameters.setViolationCost(10);
        loopFlowParameters.setConstraintAdjustmentCoefficient(5);

        createMaxLoopFlowFiller(80);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // check flow constraint for cnec1
        OpenRaoMPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND);
        OpenRaoMPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(110 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((110 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testShouldUpdate() {
        loopFlowParameters = new LoopFlowParametersExtension();
        loopFlowParameters.setPtdfApproximation(PtdfApproximation.UPDATE_PTDF_WITH_TOPO_AND_PST);
        loopFlowParameters.setAcceptableIncrease(0);
        loopFlowParameters.setViolationCost(10);
        loopFlowParameters.setConstraintAdjustmentCoefficient(5);

        createMaxLoopFlowFiller(0);
        setCommercialFlowValue(49);
        buildLinearProblem();

        // update loop-flow value
        setCommercialFlowValue(67);
        updateLinearProblem();

        // check flow constraint for cnec1
        OpenRaoMPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND);
        OpenRaoMPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(-(100 - 5.) + 67.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 67.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnec1, TwoSides.ONE);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), DOUBLE_TOLERANCE);
    }

    @Test
    void testFill2Sides() {
        loopFlowParameters = new LoopFlowParametersExtension();
        loopFlowParameters.setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        loopFlowParameters.setAcceptableIncrease(13);
        loopFlowParameters.setViolationCost(10);
        loopFlowParameters.setConstraintAdjustmentCoefficient(5);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getLoopFlow(cnecOn2sides, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(0.);
        when(initialFlowResult.getLoopFlow(cnecOn2sides, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(10.);
        maxLoopFlowFiller = new MaxLoopFlowFiller(
            Set.of(cnecOn2sides),
            initialFlowResult,
            loopFlowParameters
        );

        when(flowResult.getCommercialFlow(cnecOn2sides, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(49.);
        when(flowResult.getCommercialFlow(cnecOn2sides, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(69.);

        buildLinearProblem();

        // Check left side
        OpenRaoMPConstraint loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND);
        OpenRaoMPConstraint loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 49.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 49.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        OpenRaoMPVariable flowVariable = linearProblem.getFlowVariable(cnecOn2sides, TwoSides.ONE);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // Check right side
        loopFlowConstraintUb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, TwoSides.TWO, LinearProblem.BoundExtension.UPPER_BOUND);
        loopFlowConstraintLb = linearProblem.getMaxLoopFlowConstraint(cnecOn2sides, TwoSides.TWO, LinearProblem.BoundExtension.LOWER_BOUND);

        assertNotNull(loopFlowConstraintUb);
        assertNotNull(loopFlowConstraintLb);

        assertEquals(-(100 - 5.) + 69.0 - 0.01, loopFlowConstraintLb.lb(), DOUBLE_TOLERANCE);
        assertEquals((100 - 5.) + 69.0 + 0.01, loopFlowConstraintUb.ub(), DOUBLE_TOLERANCE);

        flowVariable = linearProblem.getFlowVariable(cnecOn2sides, TwoSides.TWO);
        assertEquals(1, loopFlowConstraintUb.getCoefficient(flowVariable), 0.1);
        assertEquals(1, loopFlowConstraintLb.getCoefficient(flowVariable), 0.1);

        // violation cost
        assertEquals(10. / 2, linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnecOn2sides, TwoSides.ONE)), DOUBLE_TOLERANCE);
        assertEquals(10. / 2, linearProblem.getObjective().getCoefficient(linearProblem.getLoopflowViolationVariable(cnecOn2sides, TwoSides.TWO)), DOUBLE_TOLERANCE);
    }

    @Test
    void testFilterCnecWithNoInitialFlow() {
        loopFlowParameters = new LoopFlowParametersExtension();
        loopFlowParameters.setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        loopFlowParameters.setAcceptableIncrease(13);
        loopFlowParameters.setViolationCost(10);
        loopFlowParameters.setConstraintAdjustmentCoefficient(5);

        FlowResult initialFlowResult = Mockito.mock(FlowResult.class);
        when(initialFlowResult.getLoopFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(90.);
        when(initialFlowResult.getFlow(cnec1, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Double.NaN);
        maxLoopFlowFiller = new MaxLoopFlowFiller(
            Set.of(cnec1),
            initialFlowResult,
            loopFlowParameters
        );
        setCommercialFlowValue(49);
        buildLinearProblem();

        Exception e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.UPPER_BOUND));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_maxloopflow_upper_bound_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getMaxLoopFlowConstraint(cnec1, TwoSides.ONE, LinearProblem.BoundExtension.LOWER_BOUND));
        assertEquals("Constraint Tieline BE FR - N - preventive_one_maxloopflow_lower_bound_constraint has not been created yet", e.getMessage());
        e = assertThrows(OpenRaoException.class, () -> linearProblem.getLoopflowViolationVariable(cnec1, TwoSides.ONE));
        assertEquals("Variable Tieline BE FR - N - preventive_one_loopflowviolation_variable has not been created yet", e.getMessage());
    }
}
