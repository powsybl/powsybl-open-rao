/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracloopflowextension.LoopFlowThreshold;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.SumCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class LoopFlowViolationCostEvaluator implements CnecViolationCostEvaluator {
    private final Set<FlowCnec> loopflowCnecs;
    private final FlowResult initialLoopFlowResult;
    private final double loopFlowViolationCost;
    private final double loopFlowAcceptableAugmentation;

    public LoopFlowViolationCostEvaluator(Set<FlowCnec> loopflowCnecs,
                                          FlowResult initialLoopFlowResult,
                                          LoopFlowParametersExtension loopFlowParameters) {
        this.loopflowCnecs = loopflowCnecs;
        this.initialLoopFlowResult = initialLoopFlowResult;
        this.loopFlowViolationCost = loopFlowParameters.getViolationCost();
        this.loopFlowAcceptableAugmentation = loopFlowParameters.getAcceptableIncrease();
    }

    @Override
    public String getName() {
        return "loop-flow-cost";
    }

    @Override
    public List<FlowCnec> getElementsInViolation(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = loopflowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .collect(Collectors.toMap(
                Function.identity(),
                cnec -> getLoopFlowExcess(flowResult, cnec)
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() != 0)
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        Collections.reverse(costlyElements);
        return new ArrayList<>(costlyElements);
    }

    double getLoopFlowExcess(FlowResult flowResult, FlowCnec cnec) {
        return cnec.getMonitoredSides()
            .stream().map(side -> Math.max(0, Math.abs(flowResult.getLoopFlow(cnec, side, Unit.MEGAWATT)) - getLoopFlowUpperBound(cnec, side)))
            .max(Double::compareTo).orElse(0.0);
    }

    private double getLoopFlowUpperBound(FlowCnec cnec, TwoSides side) {
        double loopFlowThreshold = cnec.getExtension(LoopFlowThreshold.class).getThresholdWithReliabilityMargin(Unit.MEGAWATT);
        double initialLoopFlow = initialLoopFlowResult.getLoopFlow(cnec, side, Unit.MEGAWATT);
        return Math.max(0.0, Math.max(loopFlowThreshold, Math.abs(initialLoopFlow) + loopFlowAcceptableAugmentation));
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> loopFlowCnecsAndCost = loopflowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toMap(Function.identity(), loopFlowCnec -> getLoopFlowExcess(flowResult, loopFlowCnec)));
        Set<State> states = loopflowCnecs.stream().map(FlowCnec::getState).collect(Collectors.toSet());
        Map<State, Double> costPerState = states.stream().collect(Collectors.toMap(Function.identity(), state -> computeCostForState(flowResult, state)));
        if (costPerState.values().stream().anyMatch(loopFlowCost -> loopFlowCost > 0)) {
            // TODO: will be logged even if the contingency is filtered out at some point
            OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Some loopflow constraints are not respected.");
        }

        List<FlowCnec> sortedLoopFlowCnecs = loopFlowCnecsAndCost.entrySet().stream().filter(entry -> entry.getValue() != 0).sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).collect(Collectors.toList());
        Collections.reverse(sortedLoopFlowCnecs);
        return new SumCostEvaluatorResult(costPerState, new ArrayList<>(sortedLoopFlowCnecs));
    }

    protected double computeCostForState(FlowResult flowResult, State state) {
        return loopFlowViolationCost * loopflowCnecs.stream().filter(loopFlowCnec -> state.equals(loopFlowCnec.getState())).mapToDouble(loopFlowCnec -> getLoopFlowExcess(flowResult, loopFlowCnec)).filter(loopFlowExcess -> loopFlowExcess != 0).sum();
    }
}
