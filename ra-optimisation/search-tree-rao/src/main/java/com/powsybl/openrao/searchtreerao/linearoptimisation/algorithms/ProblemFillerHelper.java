/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.CurativeOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.RangeActionLimitationParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
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
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class ProblemFillerHelper {
    private ProblemFillerHelper() {
    }

    public static List<ProblemFiller> getProblemFillers(Crac crac, Network network, OptimizationPerimeter optimizationPerimeter, PrePerimeterResult prePerimeterResult, OffsetDateTime timestamp, RaoParameters raoParameters) {
        List<ProblemFiller> problemFillers = new ArrayList<>();
        SearchTreeRaoRangeActionsOptimizationParameters.PstModel pstModel = raoParameters.hasExtension(SearchTreeRaoRangeActionsOptimizationParameters.class) ? raoParameters.getExtension(SearchTreeRaoRangeActionsOptimizationParameters.class).getPstModel() : null;
        SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking raRangeShrinking = raoParameters.hasExtension(SearchTreeRaoRangeActionsOptimizationParameters.class) ? raoParameters.getExtension(SearchTreeRaoRangeActionsOptimizationParameters.class).getRaRangeShrinking() : SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.DISABLED;

        // Core problem filler
        if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
            // TODO : mutualize arguments using only SearchTreeRaoRangeActionsOptimizationParameters extension
            CostCoreProblemFiller costCoreProblemFiller = new CostCoreProblemFiller(
                optimizationPerimeter,
                prePerimeterResult,
                raoParameters.getRangeActionsOptimizationParameters(),
                raoParameters.getExtension(SearchTreeRaoRangeActionsOptimizationParameters.class),
                raoParameters.getObjectiveFunctionParameters().getUnit(),
                !raRangeShrinking.equals(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.DISABLED),
                pstModel,
                timestamp
            );
            problemFillers.add(costCoreProblemFiller);
        } else {
            MarginCoreProblemFiller marginCoreProblemFiller = new MarginCoreProblemFiller(
                optimizationPerimeter,
                prePerimeterResult,
                raoParameters.getRangeActionsOptimizationParameters(),
                raoParameters.getExtension(SearchTreeRaoRangeActionsOptimizationParameters.class),
                raoParameters.getObjectiveFunctionParameters().getUnit(),
                !raRangeShrinking.equals(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.DISABLED),
                pstModel,
                timestamp
            );
            problemFillers.add(marginCoreProblemFiller);
        }

        // max.min margin, or max.min relative margin
        if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
            MaxMinRelativeMarginFiller maxMinRelativeMarginFiller = new MaxMinRelativeMarginFiller(
                optimizationPerimeter.getOptimizedFlowCnecs(),
                prePerimeterResult,
                raoParameters.getObjectiveFunctionParameters().getUnit(),
                raoParameters.getExtension(SearchTreeRaoRelativeMarginsParameters.class),
                timestamp
            );
            problemFillers.add(maxMinRelativeMarginFiller);
        } else {
            MaxMinMarginFiller maxMinMarginFiller = new MaxMinMarginFiller(
                optimizationPerimeter.getOptimizedFlowCnecs(),
                raoParameters.getObjectiveFunctionParameters().getUnit(),
                raoParameters.getObjectiveFunctionParameters().getType().costOptimization(),
                timestamp
            );
            problemFillers.add(maxMinMarginFiller);
        }

        // MNEC
        Optional<MnecParameters> mnecParameters = raoParameters.getMnecParameters();
        if (mnecParameters.isPresent() && raoParameters.hasExtension(SearchTreeRaoMnecParameters.class)) {
            MnecFiller mnecFiller = new MnecFiller(
                prePerimeterResult,
                optimizationPerimeter.getMonitoredFlowCnecs(),
                raoParameters.getObjectiveFunctionParameters().getUnit(),
                raoParameters.getExtension(SearchTreeRaoMnecParameters.class).getViolationCost(),
                mnecParameters.get().getAcceptableMarginDecrease(),
                raoParameters.getExtension(SearchTreeRaoMnecParameters.class).getConstraintAdjustmentCoefficient(),
                timestamp
            );
            problemFillers.add(mnecFiller);
        }

        // loop-flow limitation
        Optional<LoopFlowParameters> loopFlowParameters = raoParameters.getLoopFlowParameters();
        if (loopFlowParameters.isPresent() && raoParameters.hasExtension(SearchTreeRaoLoopFlowParameters.class)) {
            MaxLoopFlowFiller maxLoopFlowFiller = new MaxLoopFlowFiller(
                optimizationPerimeter.getLoopFlowCnecs(),
                prePerimeterResult,
                loopFlowParameters.get(),
                raoParameters.getExtension(SearchTreeRaoLoopFlowParameters.class),
                timestamp
            );
            problemFillers.add(maxLoopFlowFiller);
        }

        // unoptimized CNECs for TSOs without curative RA
        UnoptimizedCnecParameters unoptimizedCnecParameters = UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), findOperatorsNotSharingCras(crac));
        if (!Objects.isNull(unoptimizedCnecParameters)
            && !Objects.isNull(unoptimizedCnecParameters.getOperatorsNotToOptimize())
            && optimizationPerimeter instanceof CurativeOptimizationPerimeter) {
            UnoptimizedCnecFiller unoptimizedCnecFiller = new UnoptimizedCnecFiller(
                optimizationPerimeter.getFlowCnecs(),
                prePerimeterResult,
                unoptimizedCnecParameters,
                null
            );
            problemFillers.add(unoptimizedCnecFiller);
        }

        // MIP optimization vs. CONTINUOUS optimization
        if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS.equals(pstModel)) {
            Map<State, Set<PstRangeAction>> pstRangeActions = copyOnlyPstRangeActions(optimizationPerimeter.getRangeActionsPerState());
            Map<State, Set<RangeAction<?>>> otherRa = copyWithoutPstRangeActions(optimizationPerimeter.getRangeActionsPerState());
            DiscretePstTapFiller discretePstTapFiller = new DiscretePstTapFiller(
                optimizationPerimeter,
                pstRangeActions,
                prePerimeterResult,
                raoParameters.getRangeActionsOptimizationParameters(),
                raoParameters.getObjectiveFunctionParameters().getType().costOptimization(),
                timestamp
            );
            problemFillers.add(discretePstTapFiller);
            DiscretePstGroupFiller discretePstGroupFiller = new DiscretePstGroupFiller(
                optimizationPerimeter.getMainOptimizationState(),
                pstRangeActions,
                timestamp
            );
            problemFillers.add(discretePstGroupFiller);
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(otherRa, timestamp);
            problemFillers.add(continuousRangeActionGroupFiller);
        } else if (SearchTreeRaoRangeActionsOptimizationParameters.PstModel.CONTINUOUS.equals(pstModel)) {
            ContinuousRangeActionGroupFiller continuousRangeActionGroupFiller = new ContinuousRangeActionGroupFiller(optimizationPerimeter.getRangeActionsPerState(), timestamp);
            problemFillers.add(continuousRangeActionGroupFiller);
        }

        // RA limitation
        if (optimizationPerimeter.getRangeActionOptimizationStates().stream().anyMatch(state -> areRangeActionLimitedForState(crac.getRaUsageLimits(state.getInstant())))) {
            RangeActionLimitationParameters rangeActionLimitationParameters = new RangeActionLimitationParameters();
            optimizationPerimeter.getRangeActionOptimizationStates().forEach(optimizationState -> {
                RaUsageLimits raUsageLimits = crac.getRaUsageLimits(optimizationState.getInstant());
                if (raUsageLimits != null) {
                    rangeActionLimitationParameters.setMaxTso(optimizationState, raUsageLimits.getMaxTso());
                    rangeActionLimitationParameters.setMaxRangeAction(optimizationState, raUsageLimits.getMaxRa());
                    rangeActionLimitationParameters.setMaxTsoExclusion(optimizationState, raUsageLimits.getMaxTsoExclusion());
                    rangeActionLimitationParameters.setMaxPstPerTso(optimizationState, raUsageLimits.getMaxPstPerTso());
                    rangeActionLimitationParameters.setMaxElementaryActionsPerTso(optimizationState, raUsageLimits.getMaxElementaryActionsPerTso());
                    rangeActionLimitationParameters.setMaxRangeActionPerTso(optimizationState, raUsageLimits.getMaxRaPerTso());
                }
            });
            RaUsageLimitsFiller raUsageLimitsFiller = new RaUsageLimitsFiller(
                optimizationPerimeter.getRangeActionsPerState(),
                prePerimeterResult,
                rangeActionLimitationParameters,
                SearchTreeRaoRangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS.equals(pstModel),
                network,
                raoParameters.getObjectiveFunctionParameters().getType().costOptimization(),
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

    private static boolean areRangeActionLimitedForState(RaUsageLimits raUsageLimits) {
        return raUsageLimits != null && (
            raUsageLimits.getMaxRa() < Integer.MAX_VALUE
                || raUsageLimits.getMaxTso() < Integer.MAX_VALUE
                || !raUsageLimits.getMaxPstPerTso().isEmpty()
                || !raUsageLimits.getMaxRaPerTso().isEmpty()
                || !raUsageLimits.getMaxElementaryActionsPerTso().isEmpty());
    }

    private static Set<String> findOperatorsNotSharingCras(Crac crac) {
        Set<String> tsos = crac.getFlowCnecs().stream().map(Cnec::getOperator).collect(Collectors.toSet());
        tsos.addAll(crac.getRemedialActions().stream().map(RemedialAction::getOperator).collect(Collectors.toSet()));
        // <!> If a CNEC's operator is not null, filter it out of the list of operators not sharing CRAs
        return tsos.stream().filter(tso -> Objects.nonNull(tso) && !tsoHasCra(tso, crac)).collect(Collectors.toSet());
    }

    static boolean tsoHasCra(String tso, Crac crac) {
        Set<State> optimizedCurativeStates = crac.getCurativeStates();
        return optimizedCurativeStates.stream().anyMatch(state ->
            crac.getPotentiallyAvailableNetworkActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals) ||
                crac.getPotentiallyAvailableRangeActions(state).stream().map(RemedialAction::getOperator).anyMatch(tso::equals)
        );
    }
}