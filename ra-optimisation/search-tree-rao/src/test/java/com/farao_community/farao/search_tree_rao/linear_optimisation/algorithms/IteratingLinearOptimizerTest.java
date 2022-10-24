/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.commons.adapter.BranchResultAdapter;
import com.farao_community.farao.search_tree_rao.commons.adapter.SensitivityResultAdapter;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.parameters.RangeActionParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.SolverParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.mocks.MPVariableMock;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.result.impl.IteratingLinearOptimizationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionActivationResultImpl;
import com.farao_community.farao.search_tree_rao.result.impl.RangeActionSetpointResultImpl;
import com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisException;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IteratingLinearOptimizer.class, BestTapFinder.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class IteratingLinearOptimizerTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private RangeAction<?> rangeAction;

    private ObjectiveFunction objectiveFunction;

    private LinearProblem linearProblem;
    private Network network;
    private RangeActionSetpointResult rangeActionSetpointResult;
    private RangeActionActivationResult rangeActionActivationResult;
    private SensitivityComputer sensitivityComputer;
    private IteratingLinearOptimizer optimizer;
    private State optimizedState;
    private IteratingLinearOptimizerInput input;
    private OptimizationPerimeter optimizationPerimeter;

    @Before
    public void setUp() {
        rangeAction = Mockito.mock(RangeAction.class);
        when(rangeAction.getId()).thenReturn("ra");
        when(rangeAction.getNetworkElements()).thenReturn(Set.of(Mockito.mock(NetworkElement.class)));
        optimizedState = Mockito.mock(State.class);
        when(optimizedState.getInstant()).thenReturn(Instant.PREVENTIVE);

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

        IteratingLinearOptimizerParameters parameters = Mockito.mock(IteratingLinearOptimizerParameters.class);
        SolverParameters solverParameters = Mockito.mock(SolverParameters.class);
        when(solverParameters.getSolver()).thenReturn(RaoParameters.Solver.CBC);
        when(parameters.getSolverParameters()).thenReturn(solverParameters);
        when(parameters.getMaxNumberOfIterations()).thenReturn(5);
        RangeActionParameters rangeActionParameters = Mockito.mock(RangeActionParameters.class);
        when(rangeActionParameters.getPstOptimizationApproximation()).thenReturn(RaoParameters.PstOptimizationApproximation.CONTINUOUS);
        when(parameters.getRangeActionParameters()).thenReturn(rangeActionParameters);
        when(parameters.getObjectiveFunction()).thenReturn(RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT);
        optimizer = new IteratingLinearOptimizer(input, parameters);

        linearProblem = Mockito.mock(LinearProblem.class);
        network = Mockito.mock(Network.class);
        when(input.getNetwork()).thenReturn(network);
        rangeActionSetpointResult = new RangeActionSetpointResultImpl(Map.of(rangeAction, 0.));
        when(input.getPrePerimeterSetpoints()).thenReturn(rangeActionSetpointResult);
        rangeActionActivationResult = new RangeActionActivationResultImpl(rangeActionSetpointResult);
        when(input.getRaActivationFromParentLeaf()).thenReturn(rangeActionActivationResult);
        BranchResultAdapter branchResultAdapter = Mockito.mock(BranchResultAdapter.class);
        sensitivityComputer = Mockito.mock(SensitivityComputer.class);

        SystematicSensitivityResult sensi = Mockito.mock(SystematicSensitivityResult.class, "only sensi computation");
        when(systematicSensitivityInterface.run(network)).thenReturn(sensi);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        when(branchResultAdapter.getResult(sensi, network)).thenReturn(flowResult);
        when(sensitivityComputer.getBranchResult(network)).thenReturn(flowResult);
        when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult1);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        when(sensitivityResultAdapter.getResult(sensi)).thenReturn(sensitivityResult);

    }

    private void prepareLinearProblemBuilder() {
        LinearProblemBuilder linearProblemBuilder = Mockito.mock(LinearProblemBuilder.class);
        when(linearProblemBuilder.buildFromInputsAndParameters(Mockito.any(), Mockito.any())).thenReturn(linearProblem);
        try {
            PowerMockito.whenNew(LinearProblemBuilder.class).withNoArguments().thenReturn(linearProblemBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mockFunctionalCost(Double initialFunctionalCost, Double... iterationFunctionalCosts) {
        ObjectiveFunctionResult initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(initialFunctionalCost);
        if (iterationFunctionalCosts.length == 0) {
            when(objectiveFunction.evaluate(any(), any())).thenReturn(initialObjectiveFunctionResult);
        } else {
            ObjectiveFunctionResult[] objectiveFunctionResults = new ObjectiveFunctionResult[iterationFunctionalCosts.length];
            for (int i = 0; i < iterationFunctionalCosts.length; i++) {
                ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
                when(objectiveFunctionResult.getFunctionalCost()).thenReturn(iterationFunctionalCosts[i]);
                objectiveFunctionResults[i] = objectiveFunctionResult;
            }
            when(objectiveFunction.evaluate(any(), any())).thenReturn(
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
                    MPVariableMock absVariationMpVarMock = Mockito.mock(MPVariableMock.class);
                    when(absVariationMpVarMock.solutionValue()).thenReturn(Math.abs(setPoints.get(count - 1)));
                    when(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, optimizedState)).thenReturn(absVariationMpVarMock);
                    MPVariableMock setpointMpVarMock = Mockito.mock(MPVariableMock.class);
                    when(setpointMpVarMock.solutionValue()).thenReturn(setPoints.get(count - 1));
                    when(linearProblem.getRangeActionSetpointVariable(rangeAction, optimizedState)).thenReturn(setpointMpVarMock);
                }
                return statuses.get(count - 1);
            }
        }).when(linearProblem).solve();
    }

    private LinearOptimizationResult optimize() {
        return optimizer.optimize();
    }

    @Test
    public void firstOptimizationFails() {
        mockLinearProblem(List.of(LinearProblemStatus.INFEASIBLE), Collections.emptyList());
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.INFEASIBLE, result.getStatus());
    }

    @Test
    public void firstLinearProblemDoesNotChangeSetPoint() {
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL), List.of(0.));
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(mock(RangeActionActivationResult.class)).when(optimizer, "roundResult", any(), any());
            PowerMockito.doReturn(false).when(optimizer, "hasRemedialActionsChanged", any(), any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(0, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void secondLinearProblemDoesNotChangeSetPoint() {
        mockLinearProblem(Collections.nCopies(2, LinearProblemStatus.OPTIMAL), List.of(1., 1.));
        mockFunctionalCost(100., 50.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(1, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void firstLinearProblemChangesSetPointButWorsenFunctionalCost() {
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL), List.of(1.));
        mockFunctionalCost(100., 140.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.OPTIMAL, result.getStatus());
        assertEquals(0, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void reachMaxIterations() {
        mockLinearProblem(Collections.nCopies(5, LinearProblemStatus.OPTIMAL), List.of(1., 2., 3., 4., 5.));
        mockFunctionalCost(100., 90., 80., 70., 60., 50.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.MAX_ITERATION_REACHED, result.getStatus());
        assertEquals(5, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(5, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void optimizeWithInfeasibility() {
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL, LinearProblemStatus.INFEASIBLE), List.of(1.));
        mockFunctionalCost(100., 50.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.FEASIBLE, result.getStatus());
        assertEquals(1, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(50, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(1, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void optimizeWithSensitivityComputationFailure() {
        Mockito.doAnswer(invocationOnMock -> {
            network = invocationOnMock.getArgument(0);
            throw new SensitivityAnalysisException("Sensi computation failed");
        }).when(sensitivityComputer).compute(network);
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL), List.of(1.));
        mockFunctionalCost(100.);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearOptimizationResult result = optimize();

        assertEquals(LinearProblemStatus.SENSITIVITY_COMPUTATION_FAILED, result.getStatus());
        assertEquals(0, ((IteratingLinearOptimizationResultImpl) result).getNbOfIteration());
        assertEquals(100, result.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, result.getOptimizedSetpoint(rangeAction, optimizedState), DOUBLE_TOLERANCE);
    }

    @Test
    public void testUnapplyRangeAction() {
        network = NetworkImportsUtil.import12NodesNetwork();
        when(input.getNetwork()).thenReturn(network);
        mockLinearProblem(List.of(LinearProblemStatus.OPTIMAL, LinearProblemStatus.OPTIMAL), List.of(1., 2.));
        mockFunctionalCost(100., 90., 100.);
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
        MPVariableMock absVariationMpVarMock = Mockito.mock(MPVariableMock.class);
        when(absVariationMpVarMock.solutionValue()).thenReturn(1.);
        when(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, optimizedState)).thenReturn(absVariationMpVarMock);
        MPVariableMock setpointMpVarMock = Mockito.mock(MPVariableMock.class);
        when(setpointMpVarMock.solutionValue()).thenReturn(1.);
        when(linearProblem.getRangeActionSetpointVariable(rangeAction, optimizedState)).thenReturn(setpointMpVarMock);
        network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().setTapPosition(5);
        prepareLinearProblemBuilder();
        optimizer = PowerMockito.spy(optimizer);
        try {
            PowerMockito.doReturn(sensitivityComputer).when(optimizer, "createSensitivityComputer", any());
        } catch (Exception e) {
            e.printStackTrace();
        }
        optimizer.optimize();
        assertEquals(1, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }
}
