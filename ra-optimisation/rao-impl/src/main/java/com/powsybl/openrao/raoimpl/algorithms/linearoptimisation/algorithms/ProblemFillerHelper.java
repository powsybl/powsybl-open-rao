/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.LinearProblemCnecFiltering;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.FlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.MarginCriterion;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.cnecfiltering.SafeFlowCnecFilter;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.ContinuousRangeActionGroupFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.CostCoreProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.DiscretePstGroupFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MarginCoreProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxLoopFlowFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MaxMinRelativeMarginFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.MnecFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RaUsageLimitsFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.RangeActionImpacts;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers.UnoptimizedCnecFiller;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.FlowResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getLinearProblemCnecFiltering;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class ProblemFillerHelper {
    private ProblemFillerHelper() {
    }

    public static List<ProblemFiller> getProblemFillers(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters, OffsetDateTime timestamp) {
        List<ProblemFiller> problemFillers = new ArrayList<>();
        // shared by the core filler and the margin filler so that both model the same flow CNECs
        FlowCnecFilter flowCnecFilter = buildFlowCnecFilter(input, parameters);

        // Core problem filler
        if (parameters.getObjectiveFunction().costOptimization()) {
            // TODO : mutualize arguments using only SearchTreeRaoRangeActionsOptimizationParameters extension
            CostCoreProblemFiller costCoreProblemFiller = new CostCoreProblemFiller(
                input.optimizationPerimeter(),
                input.prePerimeterSetpoints(),
                parameters.getRangeActionParameters(),
                parameters.getRangeActionParametersExtension(),
                parameters.getFlowUnit(),
                parameters.getRaRangeShrinking(),
                getPstModel(parameters.getRangeActionParametersExtension()),
                timestamp,
                flowCnecFilter
            );
            problemFillers.add(costCoreProblemFiller);
        } else {
            MarginCoreProblemFiller marginCoreProblemFiller = new MarginCoreProblemFiller(
                input.optimizationPerimeter(),
                input.prePerimeterSetpoints(),
                parameters.getRangeActionParameters(),
                parameters.getRangeActionParametersExtension(),
                parameters.getFlowUnit(),
                parameters.getRaRangeShrinking(),
                getPstModel(parameters.getRangeActionParametersExtension()),
                timestamp,
                flowCnecFilter
            );
            problemFillers.add(marginCoreProblemFiller);
        }

        // max.min margin, or max.min relative margin
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                input.optimizationPerimeter().getOptimizedFlowCnecs(),
                input.preOptimizationFlowResult(),
                parameters.getFlowUnit(),
                parameters.getMinMarginParameters(),
                parameters.getMaxMinRelativeMarginParameters(),
                timestamp,
                flowCnecFilter
            );
            problemFillers.add(maxMinRelativeMarginFiller);
        } else {
            MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(
                input.optimizationPerimeter().getOptimizedFlowCnecs(),
                parameters.getFlowUnit(),
                parameters.getObjectiveFunction().costOptimization(),
                parameters.getMinMarginParameters(),
                timestamp,
                flowCnecFilter
            );
            problemFillers.add(maxMinMarginFiller);
        }

        // MNEC
        if (parameters.isRaoWithMnecLimitation()) {
            MnecFiller mnecFiller = new MnecFiller(
                input.initialFlowResult(),
                input.optimizationPerimeter().getMonitoredFlowCnecs(),
                parameters.getFlowUnit(),
                parameters.getMnecParametersExtension().getViolationCost(),
                parameters.getMnecParameters().getAcceptableMarginDecrease(),
                parameters.getMnecParametersExtension().getConstraintAdjustmentCoefficient(),
                timestamp
            );
            problemFillers.add(mnecFiller);
        }

        // loop-flow limitation
        if (parameters.isRaoWithLoopFlowLimitation()) {
            MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                input.optimizationPerimeter().getLoopFlowCnecs(),
                input.initialFlowResult(),
                parameters.getLoopFlowParameters(),
                parameters.getLoopFlowParametersExtension(),
                timestamp,
                parameters.getFlowUnit()
            );
            problemFillers.add(maxLoopFlowFiller);
        }

        // unoptimized CNECs for TSOs without curative RA
        if (isUnoptimizedCnecFillerActive(input, parameters)) {
            UnoptimizedCnecFiller unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                input.optimizationPerimeter().getFlowCnecs(),
                input.prePerimeterFlowResult(),
                parameters.getUnoptimizedCnecParameters(),
                timestamp,
                parameters.getFlowUnit()
            );
            problemFillers.add(unoptimizedCnecFiller);
        }

        // MIP optimization vs. CONTINUOUS optimization
        SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel = getPstModel(parameters.getRangeActionParametersExtension());
        if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS.equals(pstModel)) {
            Map<State, Set<PstRangeAction>> pstRangeActions = copyOnlyPstRangeActions(input.optimizationPerimeter().getRangeActionsPerState());
            Map<State, Set<RangeAction<?>>> otherRa = copyWithoutPstRangeActions(input.optimizationPerimeter().getRangeActionsPerState());
            DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
                input.optimizationPerimeter(),
                pstRangeActions,
                input.prePerimeterSetpoints(),
                parameters.getRangeActionParameters(),
                parameters.getObjectiveFunction().costOptimization(),
                parameters.getRaLimitationParameters() != null
                    && input.optimizationPerimeter().getRangeActionOptimizationStates().stream()
                    .anyMatch(state -> !parameters.getRaLimitationParameters().getMaxElementaryActionsPerTso(state).isEmpty())
            );
            problemFillers.add(discretePstTapFiller);
            DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
                input.optimizationPerimeter().getMainOptimizationState(),
                pstRangeActions
            );
            problemFillers.add(discretePstGroupFiller);
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(otherRa);
            problemFillers.add(continuousRangeActionGroupFiller);
        } else if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS.equals(pstModel)) {
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(input.optimizationPerimeter().getRangeActionsPerState());
            problemFillers.add(continuousRangeActionGroupFiller);
        }

        // RA limitation
        if (parameters.getRaLimitationParameters() != null
            && input.optimizationPerimeter().getRangeActionOptimizationStates().stream()
            .anyMatch(state -> parameters.getRaLimitationParameters().areRangeActionLimitedForState(state))) {
            RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
                input.optimizationPerimeter().getRangeActionsPerState(),
                input.prePerimeterSetpoints(),
                parameters.getRaLimitationParameters(),
                getPstModel(parameters.getRangeActionParametersExtension()) == SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS,
                parameters.getObjectiveFunction().costOptimization()
            );
            problemFillers.add(raUsageLimitsFiller);
        }
        return problemFillers;
    }

    private static boolean isUnoptimizedCnecFillerActive(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        return !Objects.isNull(parameters.getUnoptimizedCnecParameters())
            && !Objects.isNull(parameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize())
            && input.optimizationPerimeter() instanceof CurativeOptimizationPerimeter;
    }

    /**
     * Builds the filter of the flow CNECs modeled by the core filler and the margin filler of one linear problem.
     * CNECs whose flow variable is read by another filler are protected; CNECs of operators not to optimize are
     * also excluded from the witness role, since their minimum margin constraint can be relaxed.
     */
    static FlowCnecFilter buildFlowCnecFilter(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        if (getLinearProblemCnecFiltering(parameters.getRangeActionParametersExtension()) != LinearProblemCnecFiltering.SAFE_BOUNDS) {
            return FlowCnecFilter.NONE;
        }
        OptimizationPerimeter perimeter = input.optimizationPerimeter();

        Set<FlowCnec> notWitnessCnecs = new HashSet<>();
        if (isUnoptimizedCnecFillerActive(input, parameters)) {
            Set<String> operatorsNotToOptimize = parameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize();
            perimeter.getFlowCnecs().stream()
                .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()))
                .forEach(notWitnessCnecs::add);
        }

        Set<FlowCnec> protectedCnecs = new HashSet<>(notWitnessCnecs);
        if (parameters.isRaoWithMnecLimitation()) {
            protectedCnecs.addAll(perimeter.getMonitoredFlowCnecs());
        }
        if (parameters.isRaoWithLoopFlowLimitation()) {
            protectedCnecs.addAll(perimeter.getLoopFlowCnecs());
        }

        return new SafeFlowCnecFilter(
            perimeter,
            input.prePerimeterSetpoints(),
            new RangeActionImpacts(perimeter, parameters.getRangeActionParametersExtension(), parameters.getFlowUnit()),
            getMarginCriterion(input, parameters),
            protectedCnecs,
            notWitnessCnecs,
            parameters.getFlowUnit()
        );
    }

    private static MarginCriterion getMarginCriterion(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters) {
        return switch (parameters.getObjectiveFunction()) {
            case SECURE_FLOW, MAX_MIN_MARGIN -> new MarginCriterion.Absolute(OptionalDouble.empty());
            case MIN_COST -> new MarginCriterion.Absolute(OptionalDouble.of(parameters.getMinMarginParameters().getShiftedViolationThreshold()));
            case MAX_MIN_RELATIVE_MARGIN -> {
                SearchTreeRaoRelativeMarginsParameters relativeMarginParameters = parameters.getMaxMinRelativeMarginParameters();
                // same PTDF source as MaxMinRelativeMarginFiller: null means the flow result of the linearization point
                FlowResult ptdfSource = relativeMarginParameters.getPtdfApproximation().shouldUpdatePtdfWithPstChange() ? null : input.preOptimizationFlowResult();
                yield new MarginCriterion.Relative(ptdfSource, relativeMarginParameters.getPtdfSumLowerBound());
            }
        };
    }

    private static Map<State, Set<RangeAction<?>>> copyWithoutPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
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

    private static Map<State, Set<PstRangeAction>> copyOnlyPstRangeActions(Map<State, Set<RangeAction<?>>> inRangeActions) {
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
