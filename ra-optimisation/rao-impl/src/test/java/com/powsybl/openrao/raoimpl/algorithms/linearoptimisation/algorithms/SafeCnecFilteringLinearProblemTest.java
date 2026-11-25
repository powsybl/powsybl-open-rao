/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters.ObjectiveFunctionType;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.PtdfApproximation;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.PstModel;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem.LinearProblemBuilder;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;
import com.powsybl.openrao.raoimpl.result.api.LinearProblemStatus;
import com.powsybl.openrao.raoimpl.result.api.RangeActionActivationResult;
import com.powsybl.openrao.raoimpl.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.raoimpl.result.impl.RangeActionSetpointResultImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Builds and solves the real linear problem (real fillers, real solver) with and without the safe CNEC filtering,
 * and checks that the optimal objective value is the same although CNECs were actually removed.
 *
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
class SafeCnecFilteringLinearProblemTest {
    private static final double THRESHOLD = 1000.;
    private static final double RELATIVE_TOLERANCE = 1e-6;
    private static final String PST_ID = "PRA_PST_BE";

    private Crac crac;
    private PstRangeAction pst;
    private OptimizationPerimeter perimeter;
    private RangeActionSetpointResult prePerimeterSetpoints;
    private FlowResult flowResult;
    private SensitivityResult sensitivityResult;
    private Set<FlowCnec> cnecs;

    /**
     * @param overloaded true to make the two most limiting CNECs overloaded (negative minimum margin)
     */
    private void setUp(boolean overloaded) throws IOException {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac = Crac.read("small-crac.json", getClass().getResourceAsStream("/crac/small-crac.json"), network);
        pst = crac.getPstRangeAction(PST_ID);
        prePerimeterSetpoints = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, Set.of(pst));

        flowResult = Mockito.mock(FlowResult.class);
        sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus(any())).thenReturn(ComputationStatus.DEFAULT);
        cnecs = new HashSet<>();

        // two limiting CNECs pulled in opposite directions by the PST: the optimum balances them
        double limitingOffset = overloaded ? 1050. : 900.;
        addCnec("limiting-1", limitingOffset, 20., 0.8);
        addCnec("limiting-2", -limitingOffset + 30., 15., 0.6);
        // CNECs with large margins that the PST cannot make limiting
        addCnec("far-1", 100., 5., 0.3);
        addCnec("far-2", -50., -3., 0.5);
        addCnec("far-3", 0., 2., 0.9);
        addCnec("far-4", 300., 8., 0.4);
        addCnec("far-5", -200., -6., 0.7);
        // medium margin, may or may not be removed depending on the criterion
        addCnec("medium", 600., -10., 0.2);

        perimeter = new PreventiveOptimizationPerimeter(crac.getPreventiveState(), cnecs, Set.of(), Set.of(), Set.<RangeAction<?>>of(pst));
    }

    private void addCnec(String id, double flow, double sensitivity, double ptdfZonalSum) {
        FlowCnec cnec = crac.newFlowCnec()
            .withId(id)
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .withInstant(crac.getPreventiveInstant().getId())
            .withOptimized(true)
            .newThreshold().withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).withMin(-THRESHOLD).withMax(THRESHOLD).add()
            .add();
        when(flowResult.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        when(flowResult.getPtdfZonalSum(cnec, TwoSides.ONE)).thenReturn(ptdfZonalSum);
        when(sensitivityResult.getSensitivityValue(cnec, TwoSides.ONE, pst, Unit.MEGAWATT)).thenReturn(sensitivity);
        cnecs.add(cnec);
    }

    private IteratingLinearOptimizerParameters parameters(ObjectiveFunctionType objectiveFunction, LinearProblemCnecFiltering filtering) {
        SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension = new SearchTreeRaoRangeActionsOptimizationParameters();
        // costly optimization requires the APPROXIMATED_INTEGERS PST model; also exercise it with MAX_MIN_MARGIN
        rangeActionParametersExtension.setPstModel(objectiveFunction == ObjectiveFunctionType.MIN_COST || objectiveFunction == ObjectiveFunctionType.MAX_MIN_MARGIN
            ? PstModel.APPROXIMATED_INTEGERS : PstModel.CONTINUOUS);
        rangeActionParametersExtension.setLinearProblemCnecFiltering(filtering);
        // no MIP gap: the two problems must reach the exact same optimum
        rangeActionParametersExtension.getLinearOptimizationSolver().setRelativeMipGap(0.);

        SearchTreeRaoRelativeMarginsParameters relativeMarginsParameters = new SearchTreeRaoRelativeMarginsParameters();
        relativeMarginsParameters.setPtdfApproximation(PtdfApproximation.FIXED_PTDF);
        relativeMarginsParameters.setPtdfSumLowerBound(0.01);

        return IteratingLinearOptimizerParameters.create()
            .withObjectiveFunction(objectiveFunction)
            .withFlowUnit(Unit.MEGAWATT)
            .withRangeActionParameters(new RangeActionsOptimizationParameters())
            .withRangeActionParametersExtension(rangeActionParametersExtension)
            .withMaxMinRelativeMarginParameters(relativeMarginsParameters)
            .withMinMarginParameters(new SearchTreeRaoCostlyMinMarginParameters())
            .withSolverParameters(rangeActionParametersExtension.getLinearOptimizationSolver())
            .withMaxNumberOfIterations(1)
            .build();
    }

    private IteratingLinearOptimizerInput input() {
        RangeActionActivationResult linearizationPoint = new RangeActionActivationResultImpl(prePerimeterSetpoints);
        return IteratingLinearOptimizerInput.create()
            .withOptimizationPerimeter(perimeter)
            .withPrePerimeterSetpoints(prePerimeterSetpoints)
            .withInitialFlowResult(flowResult)
            .withPrePerimeterFlowResult(flowResult)
            .withPreOptimizationFlowResult(flowResult)
            .withPreOptimizationSensitivityResult(sensitivityResult)
            .withRaActivationFromParentLeaf(linearizationPoint)
            .build();
    }

    private record Solved(double objective, int numberOfConstraints) {
    }

    private Solved solve(ObjectiveFunctionType objectiveFunction, LinearProblemCnecFiltering filtering) {
        LinearProblem linearProblem = new LinearProblemBuilder().buildFromInputsAndParameters(input(), parameters(objectiveFunction, filtering));
        linearProblem.fill(flowResult, sensitivityResult);
        assertEquals(LinearProblemStatus.OPTIMAL, linearProblem.solve());
        return new Solved(linearProblem.getObjective().value(), linearProblem.numConstraints());
    }

    static Stream<Arguments> objectivesAndScenarios() {
        return Stream.of(ObjectiveFunctionType.values())
            .flatMap(objectiveFunction -> Stream.of(Arguments.of(objectiveFunction, false), Arguments.of(objectiveFunction, true)));
    }

    @ParameterizedTest(name = "{0}, overloaded = {1}")
    @MethodSource("objectivesAndScenarios")
    void filteringKeepsTheOptimalObjectiveValue(ObjectiveFunctionType objectiveFunction, boolean overloaded) throws IOException {
        setUp(overloaded);

        // the filter does remove CNECs from this problem
        Set<FlowCnec> retained = ProblemFillerHelper.buildFlowCnecFilter(input(), parameters(objectiveFunction, LinearProblemCnecFiltering.SAFE_BOUNDS))
            .retain(perimeter.getOptimizedFlowCnecs(), flowResult, sensitivityResult, new RangeActionActivationResultImpl(prePerimeterSetpoints));
        assertTrue(retained.size() < cnecs.size(), () -> "no CNEC removed: " + retained.size() + " of " + cnecs.size() + " kept");

        Solved full = solve(objectiveFunction, LinearProblemCnecFiltering.DISABLED);
        Solved filtered = solve(objectiveFunction, LinearProblemCnecFiltering.SAFE_BOUNDS);

        // fewer constraints in the filtered problem: removed CNECs are really absent from it
        assertTrue(filtered.numberOfConstraints() < full.numberOfConstraints(),
            () -> filtered.numberOfConstraints() + " constraints with filtering, " + full.numberOfConstraints() + " without");
        assertEquals(full.objective(), filtered.objective(), RELATIVE_TOLERANCE * Math.max(1., Math.abs(full.objective())));
    }
}
