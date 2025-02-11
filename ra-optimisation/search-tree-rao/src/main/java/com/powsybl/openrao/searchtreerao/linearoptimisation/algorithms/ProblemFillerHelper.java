/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ContinuousRangeActionGroupFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.CostCoreProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.DiscretePstGroupFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.DiscretePstTapFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MarginCoreProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxLoopFlowFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxMinMarginFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MaxMinRelativeMarginFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.MnecFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.RaUsageLimitsFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.UnoptimizedCnecFiller;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstModel;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class ProblemFillerHelper {
    private ProblemFillerHelper() {
    }

    public static List<ProblemFiller> getProblemFillers(IteratingLinearOptimizerInput input, IteratingLinearOptimizerParameters parameters, OffsetDateTime timestamp) {
        List<ProblemFiller> problemFillers = new ArrayList<>();

        // Core problem filler
        if (parameters.getObjectiveFunction().costOptimization()) {
            // TODO : mutualize arguments using only SearchTreeRaoRangeActionsOptimizationParameters extension
            CostCoreProblemFiller costCoreProblemFiller = new CostCoreProblemFiller(
                input.optimizationPerimeter(),
                input.prePerimeterSetpoints(),
                parameters.getRangeActionParameters(),
                parameters.getRangeActionParametersExtension(),
                parameters.getObjectiveFunctionUnit(),
                parameters.getRaRangeShrinking(),
                getPstModel(parameters.getRangeActionParametersExtension()),
                timestamp
            );
            problemFillers.add(costCoreProblemFiller);
        } else {
            MarginCoreProblemFiller marginCoreProblemFiller = new MarginCoreProblemFiller(
                input.optimizationPerimeter(),
                input.prePerimeterSetpoints(),
                parameters.getRangeActionParameters(),
                parameters.getRangeActionParametersExtension(),
                parameters.getObjectiveFunctionUnit(),
                parameters.getRaRangeShrinking(),
                getPstModel(parameters.getRangeActionParametersExtension()),
                timestamp
            );
            problemFillers.add(marginCoreProblemFiller);
        }

        // max.min margin, or max.min relative margin
        if (parameters.getObjectiveFunction().relativePositiveMargins()) {
            MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                input.optimizationPerimeter().getOptimizedFlowCnecs(),
                input.preOptimizationFlowResult(),
                parameters.getObjectiveFunctionUnit(),
                parameters.getMaxMinRelativeMarginParameters(),
                timestamp
            );
            problemFillers.add(maxMinRelativeMarginFiller);
        } else {
            MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(
                input.optimizationPerimeter().getOptimizedFlowCnecs(),
                parameters.getObjectiveFunctionUnit(),
                parameters.getObjectiveFunction().costOptimization(),
                timestamp
            );
            problemFillers.add(maxMinMarginFiller);
        }

        // MNEC
        if (parameters.isRaoWithMnecLimitation()) {
            MnecFiller mnecFiller = new MnecFiller(
                input.initialFlowResult(),
                input.optimizationPerimeter().getMonitoredFlowCnecs(),
                parameters.getObjectiveFunctionUnit(),
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
                timestamp
            );
            problemFillers.add(maxLoopFlowFiller);
        }

        // unoptimized CNECs for TSOs without curative RA
        if (!Objects.isNull(parameters.getUnoptimizedCnecParameters())
            && !Objects.isNull(parameters.getUnoptimizedCnecParameters().getOperatorsNotToOptimize())
            && input.optimizationPerimeter() instanceof CurativeOptimizationPerimeter) {
            UnoptimizedCnecFiller unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                input.optimizationPerimeter().getFlowCnecs(),
                input.prePerimeterFlowResult(),
                parameters.getUnoptimizedCnecParameters(),
                timestamp
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
                timestamp
            );
            problemFillers.add(discretePstTapFiller);
            DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
                input.optimizationPerimeter().getMainOptimizationState(),
                pstRangeActions,
                timestamp
            );
            problemFillers.add(discretePstGroupFiller);
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(otherRa, timestamp);
            problemFillers.add(continuousRangeActionGroupFiller);
        } else if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS.equals(pstModel)) {
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(input.optimizationPerimeter().getRangeActionsPerState(), timestamp);
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
                input.network(),
                parameters.getObjectiveFunction().costOptimization(),
                timestamp
            );
            problemFillers.add(raUsageLimitsFiller);
        }
        return problemFillers;
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

    static boolean tsoHasCra(String tso, Crac crac) {
        Set<State> optimizedCurativeStates = crac.getCurativeStates();
        return optimizedCurativeStates.stream().anyMatch(state ->
            crac.getPotentiallyAvailableNetworkActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals) ||
                crac.getPotentiallyAvailableRangeActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals)
        );
    }
}
