/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.*;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblemBuilder {
    private final List<ProblemFiller> problemFillers = new ArrayList<>();
    private RangeActionsOptimizationParameters.Solver solver;
    private double relativeMipGap = RangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_RELATIVE_MIP_GAP;
    private String solverSpecificParameters = RangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

    public LinearProblem buildFromInputsAndParameters(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(parameters);

        this.withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters())
            .withProblemFiller(buildCoreProblemFiller(inputs, parameters));

        // max.min margin, or max.min relative margin
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            this.withProblemFiller(buildMaxMinRelativeMarginFiller(inputs, parameters));
        } else {
            this.withProblemFiller(buildMaxMinMarginFiller(inputs, parameters));
        }

        // MNEC
        if (parameters.isRaoWithMnecLimitation()) {
            this.withProblemFiller(buildMnecFiller(inputs, parameters));
        }

        // loop-flow limitation
        if (parameters.isRaoWithLoopFlowLimitation()) {
            this.withProblemFiller(buildLoopFlowFiller(inputs, parameters));
        }

        // unoptimized CNECs for TSOs without curative RA
        if (!Objects.isNull(parameters.getUnoptimizedCnecParameters())) {
            if (!Objects.isNull(parameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize()) && inputs.getOptimizationPerimeter() instanceof CurativeOptimizationPerimeter
                || !Objects.isNull(parameters.getUnoptimizedCnecParameters().getDoNotOptimizeCnecsSecuredByTheirPst())) {
                this.withProblemFiller(buildUnoptimizedCnecFiller(inputs, parameters));
            }
        }

        // MIP optimization vs. CONTINUOUS optimization
        if (parameters.getRangeActionParameters().getPstModel().equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {
            Map<State, Set<PstRangeAction>> pstRangeActions = copyOnlyPstRangeActions(inputs.getOptimizationPerimeter().getRangeActionsPerState());
            Map<State, Set<RangeAction<?>>> otherRa = copyWithoutPstRangeActions(inputs.getOptimizationPerimeter().getRangeActionsPerState());
            this.withProblemFiller(buildIntegerPstTapFiller(inputs, pstRangeActions));
            this.withProblemFiller(buildDiscretePstGroupFiller(inputs, pstRangeActions));
            this.withProblemFiller(buildContinuousRangeActionGroupFiller(otherRa));
        } else {
            this.withProblemFiller(buildContinuousRangeActionGroupFiller(inputs.getOptimizationPerimeter().getRangeActionsPerState()));
        }

        // RA limitation
        if (parameters.getRaLimitationParameters() != null
            && inputs.getOptimizationPerimeter().getRangeActionOptimizationStates().stream()
            .anyMatch(state -> parameters.getRaLimitationParameters().areRangeActionLimitedForState(state))) {
            this.withProblemFiller(buildRaUageLimitsFiller(inputs, parameters));
        }

        return new LinearProblem(problemFillers, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblem build() {
        return new LinearProblem(problemFillers, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblemBuilder withProblemFiller(ProblemFiller problemFiller) {
        problemFillers.add(problemFiller);
        return this;
    }

    public LinearProblemBuilder withSolver(RangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        return this;
    }

    public LinearProblemBuilder withRelativeMipGap(double relativeMipGap) {
        this.relativeMipGap = relativeMipGap;
        return this;
    }

    public LinearProblemBuilder withSolverSpecificParameters(String solverSpecificParameters) {
        this.solverSpecificParameters = solverSpecificParameters;
        return this;
    }

    private ProblemFiller buildCoreProblemFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new CoreProblemFiller(
            inputs.getOptimizationPerimeter(),
            inputs.getPrePerimeterSetpoints(),
            inputs.getRaActivationFromParentLeaf(),
            parameters.getRangeActionParameters(),
            parameters.getObjectiveFunctionUnit(),
            parameters.getRaRangeShrinking()
        );
    }

    private ProblemFiller buildMaxMinRelativeMarginFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new MaxMinRelativeMarginFiller(
            inputs.getOptimizationPerimeter().getOptimizedFlowCnecs(),
            inputs.getPreOptimizationFlowResult(),
            parameters.getObjectiveFunction().getUnit(),
            parameters.getMaxMinRelativeMarginParameters()
        );
    }

    private ProblemFiller buildMaxMinMarginFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new MaxMinMarginFiller(
            inputs.getOptimizationPerimeter().getOptimizedFlowCnecs(),
            parameters.getObjectiveFunctionUnit()
        );
    }

    private ProblemFiller buildMnecFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new MnecFiller(
            inputs.getInitialFlowResult(),
            inputs.getOptimizationPerimeter().getMonitoredFlowCnecs(),
            parameters.getObjectiveFunctionUnit(),
            parameters.getMnecParameters()
        );
    }

    private ProblemFiller buildLoopFlowFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new MaxLoopFlowFiller(
            inputs.getOptimizationPerimeter().getLoopFlowCnecs(),
            inputs.getInitialFlowResult(),
            parameters.getLoopFlowParameters()
        );
    }

    private ProblemFiller buildUnoptimizedCnecFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new UnoptimizedCnecFiller(
            inputs.getOptimizationPerimeter(),
            inputs.getOptimizationPerimeter().getFlowCnecs(),
            inputs.getPrePerimeterFlowResult(),
            parameters.getUnoptimizedCnecParameters(),
            parameters.getRangeActionParameters()
        );
    }

    private ProblemFiller buildIntegerPstTapFiller(IteratingLinearOptimizerInput inputs, Map<State, Set<PstRangeAction>> pstRangeActions) {
        return new DiscretePstTapFiller(
            inputs.getNetwork(),
            inputs.getOptimizationPerimeter(),
            pstRangeActions,
            inputs.getPrePerimeterSetpoints()
        );
    }

    private ProblemFiller buildDiscretePstGroupFiller(IteratingLinearOptimizerInput inputs, Map<State, Set<PstRangeAction>> pstRangeActions) {
        return new DiscretePstGroupFiller(
            inputs.getNetwork(),
            inputs.getOptimizationPerimeter().getMainOptimizationState(),
            pstRangeActions
        );
    }

    private ProblemFiller buildContinuousRangeActionGroupFiller(Map<State, Set<RangeAction<?>>> rangeActionsPerState) {
        return new ContinuousRangeActionGroupFiller(rangeActionsPerState);
    }

    private ProblemFiller buildRaUageLimitsFiller(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new RaUsageLimitsFiller(
            inputs.getOptimizationPerimeter().getRangeActionsPerState(),
            inputs.getPrePerimeterSetpoints(),
            parameters.getRaLimitationParameters(),
            parameters.getRangeActionParameters().getPstModel() == RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS);
    }

    private Map<State, Set<RangeAction<?>>> copyWithoutPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
        Map<State, Set<RangeAction<?>>> outRangeActions = new HashMap<>();
        inRangeActions.forEach((state, rangeActions) -> {
            if (rangeActions.stream().anyMatch(ra -> !(ra instanceof PstRangeAction))) {
                outRangeActions.put(state, rangeActions.stream().filter(ra -> !(ra instanceof PstRangeAction)).collect(Collectors.toCollection(
                    () -> new TreeSet<>(Comparator.comparing(RangeAction::getId))
                )));
            }
        });
        return outRangeActions;
    }

    private Map<State, Set<PstRangeAction>> copyOnlyPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
        Map<State, Set<PstRangeAction>> outRangeActions = new TreeMap<>(Comparator.comparing(State::getId));
        inRangeActions.forEach((state, rangeActions) -> {
            if (rangeActions.stream().anyMatch(PstRangeAction.class::isInstance)) {
                outRangeActions.put(state, rangeActions.stream().filter(PstRangeAction.class::isInstance).map(PstRangeAction.class::cast).collect(Collectors.toCollection(
                    () -> new TreeSet<>(Comparator.comparing(PstRangeAction::getId))
                )));
            }
        });
        return outRangeActions;
    }
}
