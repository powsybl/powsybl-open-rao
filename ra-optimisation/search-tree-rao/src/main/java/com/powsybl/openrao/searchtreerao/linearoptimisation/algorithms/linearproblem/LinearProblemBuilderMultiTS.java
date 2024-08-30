/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.*;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerMultiTSInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jeremy Wang {@literal <jeremy.wang at rte-france.com>}
 */
public class LinearProblemBuilderMultiTS {

    private static final String OPT_PROBLEM_NAME = "RangeActionOptProblem";

    private final List<ProblemFiller> problemFillers = new ArrayList<>();
    private RangeActionsOptimizationParameters.Solver solver;
    private double relativeMipGap = RangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_RELATIVE_MIP_GAP;
    private String solverSpecificParameters = RangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

    public LinearProblem buildFromInputsAndParameters(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters) {
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(parameters);

        this.withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters());

        for (int timeStepIndex = 0; timeStepIndex < inputs.getOptimizationPerimeters().size(); timeStepIndex++) {
            this.withProblemFiller(buildCoreProblemFiller(inputs, parameters, timeStepIndex));
        }

        // max.min margin, or max.min relative margin
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            this.withProblemFiller(buildMaxMinRelativeMarginFiller(inputs, parameters));
        } else {
            this.withProblemFiller(buildMaxMinMarginFiller(inputs, parameters));
        }

        // MNEC
        if (parameters.isRaoWithMnecLimitation()) {
            for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
                this.withProblemFiller(buildMnecFiller(inputs, parameters, optimizationPerimeter));
            }
        }

        // loop-flow limitation
        if (parameters.isRaoWithLoopFlowLimitation()) {
            for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
                this.withProblemFiller(buildLoopFlowFiller(inputs, parameters, optimizationPerimeter));
            }
        }

        // unoptimized CNECs for TSOs without curative RA
        if (!Objects.isNull(parameters.getUnoptimizedCnecParameters())) {
            for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
                if (!Objects.isNull(parameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize()) && optimizationPerimeter instanceof CurativeOptimizationPerimeter
                    || !Objects.isNull(parameters.getUnoptimizedCnecParameters().getDoNotOptimizeCnecsSecuredByTheirPst())) {
                    this.withProblemFiller(buildUnoptimizedCnecFiller(inputs, parameters, optimizationPerimeter));
                }
            }
        }

        // MIP optimization vs. CONTINUOUS optimization
        if (parameters.getRangeActionParameters().getPstModel().equals(RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS)) {
            List<Map<State, Set<PstRangeAction>>> pstRangeActionsPerTS = new ArrayList<>();
            List<Map<State, Set<RangeAction<?>>>> otherRaPerTS = new ArrayList<>();

            for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
                pstRangeActionsPerTS.add(copyOnlyPstRangeActions(optimizationPerimeter.getRangeActionsPerState()));
                otherRaPerTS.add(copyWithoutPstRangeActions(optimizationPerimeter.getRangeActionsPerState()));
            }

            for (int i = 0; i < inputs.getNetworks().size(); i++) {
                this.withProblemFiller(buildIntegerPstTapFiller(inputs, pstRangeActionsPerTS.get(i), i));
                this.withProblemFiller(buildDiscretePstGroupFiller(inputs, pstRangeActionsPerTS.get(i), i));
                this.withProblemFiller(buildContinuousRangeActionGroupFiller(otherRaPerTS.get(i)));
            }

        } else {
            for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
                this.withProblemFiller(buildContinuousRangeActionGroupFiller(optimizationPerimeter.getRangeActionsPerState()));
            }
        }

        // Add Multi time steps constraints if multiple time steps
        if (inputs.getNetworks().size() > 1) {
            this.withProblemFiller(buildMultiTSFiller(inputs, parameters));
        }

        // RA limitation
        for (OptimizationPerimeter optimizationPerimeter : inputs.getOptimizationPerimeters()) {
            if (parameters.getRaLimitationParameters() != null
                && optimizationPerimeter.getRangeActionOptimizationStates().stream()
                .anyMatch(state -> parameters.getRaLimitationParameters().areRangeActionLimitedForState(state))) {
                this.withProblemFiller(buildRaUageLimitsFiller(inputs, parameters, optimizationPerimeter));
            }
        }
        return new LinearProblem(problemFillers, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblem build() {
        return new LinearProblem(problemFillers, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblemBuilderMultiTS withProblemFiller(ProblemFiller problemFiller) {
        problemFillers.add(problemFiller);
        return this;
    }

    public LinearProblemBuilderMultiTS withSolver(RangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        return this;
    }

    public LinearProblemBuilderMultiTS withRelativeMipGap(double relativeMipGap) {
        this.relativeMipGap = relativeMipGap;
        return this;
    }

    public LinearProblemBuilderMultiTS withSolverSpecificParameters(String solverSpecificParameters) {
        this.solverSpecificParameters = solverSpecificParameters;
        return this;
    }

    public OpenRaoMPSolver buildSolver(IteratingLinearOptimizerParameters parameters) {
        return new OpenRaoMPSolver(OPT_PROBLEM_NAME, parameters.getSolverParameters().getSolver());
    }

    private ProblemFiller buildCoreProblemFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters, int timeStepIndex) {
        return new CoreProblemFiller(
            inputs.getOptimizationPerimeters().get(timeStepIndex),
            inputs.getPrePerimeterSetpoints(),
            inputs.getRaActivationFromParentLeaf(),
            parameters.getRangeActionParameters(),
            parameters.getObjectiveFunctionUnit(),
            parameters.getRaRangeShrinking(),
            timeStepIndex
        );
    }

    private ProblemFiller buildMaxMinRelativeMarginFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters) {
        Set<FlowCnec> optimizedCnecs = new HashSet<>();
        for (OptimizationPerimeter perimeter : inputs.getOptimizationPerimeters()) {
            optimizedCnecs.addAll(perimeter.getOptimizedFlowCnecs());
        }
        return new MaxMinRelativeMarginFiller(
            optimizedCnecs,
            inputs.getPreOptimizationFlowResult(),
            parameters.getObjectiveFunction().getUnit(),
            parameters.getMaxMinRelativeMarginParameters()
        );
    }

    private ProblemFiller buildMaxMinMarginFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters) {
        Set<FlowCnec> optimizedCnecs = new HashSet<>();
        for (OptimizationPerimeter perimeter : inputs.getOptimizationPerimeters()) {
            optimizedCnecs.addAll(perimeter.getOptimizedFlowCnecs());
        }
        return new MaxMinMarginFiller(
            optimizedCnecs,
            parameters.getObjectiveFunctionUnit()
        );
    }

    private ProblemFiller buildMnecFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters, OptimizationPerimeter optimizationPerimeter) {
        return new MnecFiller(
            inputs.getInitialFlowResult(),
            optimizationPerimeter.getMonitoredFlowCnecs(),
            parameters.getObjectiveFunctionUnit(),
            parameters.getMnecParameters()
        );
    }

    private ProblemFiller buildLoopFlowFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters, OptimizationPerimeter optimizationPerimeter) {
        return new MaxLoopFlowFiller(
            optimizationPerimeter.getLoopFlowCnecs(),
            inputs.getInitialFlowResult(),
            parameters.getLoopFlowParameters()
        );
    }

    private ProblemFiller buildUnoptimizedCnecFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters, OptimizationPerimeter optimizationPerimeter) {
        return new UnoptimizedCnecFiller(
            optimizationPerimeter,
            optimizationPerimeter.getFlowCnecs(),
            inputs.getPrePerimeterFlowResult(),
            parameters.getUnoptimizedCnecParameters(),
            parameters.getRangeActionParameters()
        );
    }

    private ProblemFiller buildIntegerPstTapFiller(IteratingLinearOptimizerMultiTSInput inputs, Map<State, Set<PstRangeAction>> pstRangeActions, int i) {
        return new DiscretePstTapFiller(
            inputs.getNetwork(i),
            inputs.getOptimizationPerimeter(i),
            pstRangeActions,
            inputs.getPrePerimeterSetpoints()
        );
    }

    private ProblemFiller buildDiscretePstGroupFiller(IteratingLinearOptimizerMultiTSInput inputs, Map<State, Set<PstRangeAction>> pstRangeActions, int i) {
        return new DiscretePstGroupFiller(
            inputs.getNetwork(i),
            inputs.getOptimizationPerimeter(i).getMainOptimizationState(),
            pstRangeActions
        );
    }

    private ProblemFiller buildMultiTSFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters) {
        return new MultiTSFiller(
            inputs.getOptimizationPerimeters(),
            inputs.getNetworks(),
            parameters.getRangeActionParameters(),
            inputs.getRaActivationFromParentLeaf()
        );
    }

    private ProblemFiller buildContinuousRangeActionGroupFiller(Map<State, Set<RangeAction<?>>> rangeActionsPerState) {
        return new ContinuousRangeActionGroupFiller(rangeActionsPerState);
    }

    private ProblemFiller buildRaUageLimitsFiller(IteratingLinearOptimizerMultiTSInput inputs, IteratingLinearOptimizerParameters parameters, OptimizationPerimeter optimizationPerimeter) {
        return new RaUsageLimitsFiller(
            optimizationPerimeter.getRangeActionsPerState(),
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
