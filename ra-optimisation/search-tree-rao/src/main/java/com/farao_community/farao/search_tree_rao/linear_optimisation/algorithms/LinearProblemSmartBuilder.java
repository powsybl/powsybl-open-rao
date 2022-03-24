/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.*;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPSolver;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.linear_optimisation.inputs.IteratingLinearOptimizerInput;
import com.farao_community.farao.search_tree_rao.linear_optimisation.parameters.IteratingLinearOptimizerParameters;
import com.google.ortools.linearsolver.MPSolver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblemSmartBuilder {

    private static final String OPT_PROBLEM_NAME = "RangeActionOptProblem";

    private IteratingLinearOptimizerInput inputs;
    private IteratingLinearOptimizerParameters parameters;

    public LinearProblemSmartBuilder withInputs(IteratingLinearOptimizerInput inputs) {
        this.inputs = inputs;
        return this;
    }

    public LinearProblemSmartBuilder withParameters(IteratingLinearOptimizerParameters parameters) {
        this.parameters = parameters;
        return this;
    }

    public LinearProblem build() {

        Objects.requireNonNull(inputs);
        Objects.requireNonNull(parameters);

        LinearProblemBuilder builder = LinearProblem.create();

        builder.withSolver(buildSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters())
            .withProblemFiller(buildCoreProblemFiller());

        // max.min margin, or max.min relative margin
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            builder.withProblemFiller(buildMaxMinRelativeMarginFiller());
        } else {
            builder.withProblemFiller(buildMaxMinMarginFiller());
        }

        // MNEC
        if (parameters.isRaoWithMnecLimitation()) {
            builder.withProblemFiller(buildMnecFiller());
        }

        // loop-flow limitation
        if (parameters.isRaoWithLoopFlowLimitation()) {
            builder.withProblemFiller(buildLoopFlowFiller());
        }

        // unoptimized CNECs for TSOs without curative RA
        if (!Objects.isNull(parameters.getUnoptimizedCnecParameters())) {
            builder.withProblemFiller(buildUnoptimizedCnecFiller());
        }

        // MIP optimization vs. CONTINUOUS optimization
        if (parameters.getRangeActionParameters().getPstOptimizationApproximation().equals(RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {
            Map<State, Set<PstRangeAction>> pstRangeActions = copyOnlyPstRangeActions(inputs.getOptimizationPerimeter().getRangeActionsPerState());
            Map<State, Set<RangeAction<?>>> otherRa = copyWithoutPstRangeActions(inputs.getOptimizationPerimeter().getRangeActionsPerState());
            builder.withProblemFiller(buildIntegerPstTapFiller(pstRangeActions));
            builder.withProblemFiller(buildDiscretePstGroupFiller(pstRangeActions));
            builder.withProblemFiller(buildContinuousRangeActionGroupFiller(otherRa));
        } else {
            builder.withProblemFiller(buildContinuousRangeActionGroupFiller(inputs.getOptimizationPerimeter().getRangeActionsPerState()));
        }

        // RA limitation

        if (parameters.getRaLimitationParameters() != null
            && inputs.getOptimizationPerimeter().getRangeActionOptimizationStates().stream()
                .anyMatch(state -> parameters.getRaLimitationParameters().areRangeActionLimitedForState(state))) {
            builder.withProblemFiller(buildRaUageLimitsFiller());
        }

        return builder.build();
    }

    private FaraoMPSolver buildSolver() {
        switch (parameters.getSolverParameters().getSolver()) {
            case CBC:
                return new FaraoMPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);

            case SCIP:
                return new FaraoMPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING);

            case XPRESS:
                return new FaraoMPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.XPRESS_MIXED_INTEGER_PROGRAMMING);

            default:
                throw new FaraoException(String.format("unknown solver %s in RAO parameters", parameters.getSolverParameters().getSolver()));
        }
    }

    private ProblemFiller buildCoreProblemFiller() {
        return new CoreProblemFiller(
            inputs.getNetwork(),
            inputs.getOptimizationPerimeter(),
            inputs.getPrePerimeterSetpoints(),
            inputs.getRaActivationFromParentLeaf(),
            parameters.getRangeActionParameters()
        );
    }

    private ProblemFiller buildMaxMinRelativeMarginFiller() {
        return new MaxMinRelativeMarginFiller(
            inputs.getOptimizationPerimeter().getOptimizedFlowCnecs(),
            inputs.getPreOptimizationFlowResult(),
            parameters.getObjectiveFunction().getUnit(),
            parameters.getMaxMinRelativeMarginParameters()
        );
    }

    private ProblemFiller buildMaxMinMarginFiller() {
        return new MaxMinMarginFiller(
            inputs.getOptimizationPerimeter().getOptimizedFlowCnecs(),
            parameters.getObjectiveFunctionUnit()
        );
    }

    private ProblemFiller buildMnecFiller() {
        return new MnecFiller(
            inputs.getInitialFlowResult(),
            inputs.getOptimizationPerimeter().getMonitoredFlowCnecs(),
            parameters.getObjectiveFunctionUnit(),
            parameters.getMnecParameters()
        );
    }

    private ProblemFiller buildLoopFlowFiller() {
        return new MaxLoopFlowFiller(
            inputs.getOptimizationPerimeter().getLoopFlowCnecs(),
            inputs.getInitialFlowResult(),
            parameters.getLoopFlowParameters()
        );
    }

    private ProblemFiller buildUnoptimizedCnecFiller() {
        return new UnoptimizedCnecFiller(
            inputs.getOptimizationPerimeter().getFlowCnecs(),
            inputs.getPrePerimeterFlowResult(),
            parameters.getUnoptimizedCnecParameters()
        );
    }

    private ProblemFiller buildIntegerPstTapFiller(Map<State, Set<PstRangeAction>> pstRangeActions) {
        return new DiscretePstTapFiller(
            inputs.getNetwork(),
            inputs.getOptimizationPerimeter().getMainOptimizationState(),
            pstRangeActions,
            inputs.getPrePerimeterSetpoints()
        );
    }

    private ProblemFiller buildDiscretePstGroupFiller(Map<State, Set<PstRangeAction>> pstRangeActions) {
        return new DiscretePstGroupFiller(
            inputs.getNetwork(),
            inputs.getOptimizationPerimeter().getMainOptimizationState(),
            pstRangeActions
        );
    }

    private ProblemFiller buildContinuousRangeActionGroupFiller(Map<State, Set<RangeAction<?>>> rangeActionsPerState) {
        return new ContinuousRangeActionGroupFiller(rangeActionsPerState);
    }

    private ProblemFiller buildRaUageLimitsFiller() {
        return new RaUsageLimitsFiller(
            inputs.getOptimizationPerimeter().getRangeActionsPerState(),
            inputs.getPrePerimeterSetpoints(),
            parameters.getRaLimitationParameters(),
            parameters.getRangeActionParameters().getPstOptimizationApproximation() == RaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS);
    }

    private Map<State, Set<RangeAction<?>>> copyWithoutPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
        Map<State, Set<RangeAction<?>>> outRangeActions = new HashMap<>();
        inRangeActions.forEach((state, rangeActions) -> {
            if (rangeActions.stream().anyMatch(ra -> !(ra instanceof PstRangeAction))) {
                outRangeActions.put(state, rangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).collect(Collectors.toSet()));
            }
        });
        return outRangeActions;
    }

    private Map<State, Set<PstRangeAction>> copyOnlyPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
        Map<State, Set<PstRangeAction>> outRangeActions = new TreeMap<>(Comparator.comparing(State::getId));
        inRangeActions.forEach((state, rangeActions) -> {
            if (rangeActions.stream().anyMatch(PstRangeAction.class::isInstance)) {
                outRangeActions.put(state, rangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).collect(Collectors.toSet()));
            }
        });
        return outRangeActions;
    }
}
