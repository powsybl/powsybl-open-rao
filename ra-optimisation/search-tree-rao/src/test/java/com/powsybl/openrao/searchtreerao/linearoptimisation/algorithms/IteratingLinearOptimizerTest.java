/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.searchtreerao.commons.adapter.BranchResultAdapter;
import com.powsybl.openrao.searchtreerao.commons.adapter.SensitivityResultAdapter;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.IteratingLinearOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityInterface;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class IteratingLinearOptimizerTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private RangeAction<?> rangeAction;

    private ObjectiveFunction objectiveFunction;

    private LinearProblem linearProblem;
    private Network network;
    private RangeActionSetpointResult rangeActionSetpointResult;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityComputer sensitivityComputer;
    private State optimizedState;
    private IteratingLinearOptimizerInput input;
    private IteratingLinearOptimizerParameters parameters;
    private OptimizationPerimeter optimizationPerimeter;

    private MockedStatic<LinearProblem> linearProblemMockedStatic;
    private MockedStatic<SensitivityComputer> sensitivityComputerMockedStatic;

    @BeforeEach
    public void setUp() {
        rangeAction = Mockito.mock(RangeAction.class);
        when(rangeAction.getId()).thenReturn("ra");
        when(rangeAction.getNetworkElements()).thenReturn(Set.of(Mockito.mock(NetworkElement.class)));
        optimizedState = Mockito.mock(State.class);
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(optimizedState.getInstant()).thenReturn(preventiveInstant);

        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        SystematicSensitivityInterface systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        SensitivityResultAdapter sensitivityResultAdapter = Mockito.mock(SensitivityResultAdapter.class);

        input = Mockito.mock(IteratingLinearOptimizerInput.class);
        when(input.getObjectiveFunction()).thenReturn(objectiveFunction);
        SensitivityResult sensitivityResult1 = Mockito.mock(SensitivityResult.class);
        when(input.getPreOptimizationSensitivityResult()).thenReturn(sensitivityResult1);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(rangeAction)
        ));
        when(optimizationPerimeter.getMainOptimizationState()).thenReturn(optimizedState);
        when(input.getOptimizationPerimeter()).thenReturn(optimizationPerimeter);

        parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);
        RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters = Mockito.mock(RangeActionsOptimizationParameters.LinearOptimizationSolver.class);
        when(solverParameters.getSolver()).thenReturn(RangeActionsOptimizationParameters.Solver.CBC);
        when(parameters.getSolverParameters()).thenReturn(solverParameters);
        when(parameters.getMaxNumberOfIterations()).thenReturn(5);
        RangeActionsOptimizationParameters rangeActionParameters = Mockito.mock(RangeActionsOptimizationParameters.class);
        when(rangeActionParameters.getPstModel()).thenReturn(RangeActionsOptimizationParameters.PstModel.CONTINUOUS);
        when(parameters.getRangeActionParametersExtension()).thenReturn(rangeActionParameters);
        when(parameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(parameters.getObjectiveFunctionUnit()).thenReturn(Unit.MEGAWATT);
        when(parameters.getRaRangeShrinking()).thenReturn(false);

        linearProblem = Mockito.mock(LinearProblem.class);
        network = Mockito.mock(Network.class);
        when(input.getNetwork()).thenReturn(network);
        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(rangeAction, 0.));
        when(input.getPrePerimeterSetpoints()).thenReturn(rangeActionSetpointResult);
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        when(input.getRaActivationFromParentLeaf()).thenReturn(rangeActionActivationResult);
        BranchResultAdapter branchResultAdapter = Mockito.mock(BranchResultAdapter.class);
        sensitivityComputer = Mockito.mock(SensitivityComputer.class);

        Instant outageInstant = Mockito.mock(Instant.class);
        when(outageInstant.isOutage()).thenReturn(true);
        SystematicSensitivityResult sensi = Mockito.mock(SystematicSensitivityResult.class, "only sensi computation");
        when(systematicSensitivityInterface.run(network)).thenReturn(sensi);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(branchResultAdapter.getResult(sensi, network)).thenReturn(flowResult);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(flowResult);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult1);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityResultAdapter.getResult(sensi)).thenReturn(sensitivityResult);

        linearProblemMockedStatic = Mockito.mockStatic(LinearProblem.class);
        sensitivityComputerMockedStatic = Mockito.mockStatic(SensitivityComputer.class);
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = Mockito.spy(SensitivityComputer.SensitivityComputerBuilder.class);
        doReturn(sensitivityComputer).when(sensitivityComputerBuilder).build();
        sensitivityComputerMockedStatic.when(SensitivityComputer::create).thenReturn(sensitivityComputerBuilder);

        when(input.getOutageInstant()).thenReturn(outageInstant);
    }

    @AfterEach
    public void tearDown() {
        linearProblemMockedStatic.close();
        sensitivityComputerMockedStatic.close();
    }

    private void prepareLinearProblemBuilder() {
        LinearProblemBuilder linearProblemBuilder = Mockito.mock(LinearProblemBuilder.class);
        when(linearProblemBuilder.buildFromInputsAndParameters(Mockito.any(), Mockito.any())).thenReturn(linearProblem);
        linearProblemMockedStatic.when(LinearProblem::create).thenReturn(linearProblemBuilder);
    }

    private void mockFunctionalCost(Double initialFunctionalCost, Double... iterationFunctionalCosts) {
        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(initialFunctionalCost);
        if (iterationFunctionalCosts.length == 0) {
            when(objectiveFunction.evaluate(any())).thenReturn(initialObjectiveFunctionResult);
        } else {
            ObjectiveFunctionResult[] objectiveFunctionResults = new ObjectiveFunctionResult[iterationFunctionalCosts.length];
            for (int i = 0; i < iterationFunctionalCosts.length; i++) {
                ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
                when(objectiveFunctionResult.getFunctionalCost()).thenReturn(iterationFunctionalCosts[i]);
                objectiveFunctionResults[i] = objectiveFunctionResult;
            }
            when(objectiveFunction.evaluate(any())).thenReturn(
                    initialObjectiveFunctionResult,
                    objectiveFunctionResults
            );
        }
    }

    private void mockLinearProblem(List<LinearProblemStatus> statuses, List<Double> setPoints) {
        doAnswer(new Answer() {
            private int count = 0;
            public Object answer(InvocationOnMock invocation) {
                count += 1;
                if (statuses.get(count - 1) == LinearProblemStatus.OPTIMAL) {
                    OpenRaoMPVariable absVariationMpVarMock = Mockito.mock(OpenRaoMPVariable.class);
                    when(absVariationMpVarMock.solutionValue()).thenReturn(Math.abs(setPoints.get(count - 1)));
                    when(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, optimizedState)).thenReturn(absVariationMpVarMock);
                    OpenRaoMPVariable setpointMpVarMock = Mockito.mock(OpenRaoMPVariable.class);
                    when(setpointMpVarMock.solutionValue()).thenReturn(setPoints.get(count - 1));
                    when(linearProblem.getRangeActionSetpointVariable(rangeAction, optimizedState)).thenReturn(setpointMpVarMock);
                }
                return statuses.get(count - 1);
            }
        }).when(linearProblem).solve();
    }

    @Test
    void firstOptimizationFails() {
        mockLinearProblem(List.of(LinearProblemStatus.INFEASIBLE), Collections.emptyList());
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();
        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.INFEASIBLE, result.getStatus());
    }

    @Test
    void firstLinearProblemDoesNotChangeSetPoint() {
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL), List.of(0.));
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(1, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void secondLinearProblemDoesNotChangeSetPoint() {
        mockLinearProblem(Collections.nCopies(2, LinearProblemStatus.OPTIMAL), List.of(1., 1.));
        mockFunctionalCost(100., 50.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(2, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void linearProblemDegradesTheSolutionButKeepsBestIteration() {
        when(parameters.getRaRangeShrinking()).thenReturn(true);
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 150., 140., 130., 120., 110.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.MAX_ITERATION_REACHED, result.getStatus());
        assertEquals(5, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void linearProblemDegradesTheSolution() {
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 150., 140., 130., 120., 110.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(1, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void linearProblemFluctuatesButKeepsBestIteration() {
        when(parameters.getRaRangeShrinking()).thenReturn(true);
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 120., 105., 90., 100., 95.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.MAX_ITERATION_REACHED, result.getStatus());
        assertEquals(5, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(90, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(3, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void reachMaxIterations() {
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 90., 80., 70., 60., 50.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.MAX_ITERATION_REACHED, result.getStatus());
        assertEquals(5, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(5, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void optimizeWithInfeasibility() {
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL, LinearProblemStatus.INFEASIBLE), List.of(1.));
        mockFunctionalCost(100., 50.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.FEASIBLE, result.getStatus());
        assertEquals(2, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void optimizeWithSensitivityComputationFailure() {
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FAILURE);
        Mockito.doNothing().when(sensitivityComputer).compute(network);
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL), List.of(1.));
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = IteratingLinearOptimizer.optimize(input, parameters);

        assertEquals(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED, result.getStatus());
        assertEquals(1, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    void testUnapplyRangeAction() {
        when(parameters.getRaRangeShrinking()).thenReturn(true);
        network = NetworkImportsUtil.import12NodesNetwork();
        when(input.getNetwork()).thenReturn(network);
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 120., 105., 90., 100., 95.);
        Crac crac = CracFactory.findDefault().create("test-crac");
        rangeAction = crac.newPstRangeAction().withId("test-pst").withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(0, 0., 1, 1., 2, 2., 3, 3., 4, 4., 5, 5.)).add();
        when(optimizationPerimeter.getRangeActionsPerState()).thenReturn(Map.of(
            optimizedState, Set.of(rangeAction)
        ));
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(rangeAction, 5.));
        when(input.getPrePerimeterSetpoints()).thenReturn(rangeActionSetpointResult);
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        when(input.getRaActivationFromParentLeaf()).thenReturn(rangeActionActivationResult);
        prepareLinearProblemBuilder();

        IteratingLinearOptimizer.optimize(input, parameters);
        assertEquals(3, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }
}
