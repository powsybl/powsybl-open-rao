/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CnecViolationCostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final FlowResult initialFlowResult;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecViolationCost;

    public MnecViolationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, FlowResult initialFlowResult, MnecParametersExtension mnecParametersExtension) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.initialFlowResult = initialFlowResult;
        this.mnecAcceptableMarginDecrease = mnecParametersExtension.getAcceptableMarginDecrease();
        this.mnecViolationCost = mnecParametersExtension.getViolationCost();
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    @Override
    public double evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return Math.abs(mnecViolationCost) < 1e-10 ? 0.0 : mnecViolationCost * getElementsInViolation(flowResult, contingenciesToExclude).stream().mapToDouble(mnec -> computeMnecCost(flowResult, mnec)).sum();
    }

    @Override
    public List<FlowCnec> getElementsInViolation(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> sortedElements = flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isMonitored)
            .collect(Collectors.toMap(
                Function.identity(),
                cnec -> computeMnecCost(flowResult, cnec)
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() != 0)
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        Collections.reverse(sortedElements);
        return new ArrayList<>(sortedElements);
    }

    private double computeMnecCost(FlowResult flowResult, FlowCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, unit);
        double currentMargin = flowResult.getMargin(mnec, unit);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDecrease) - currentMargin);
    }

    @Override
    public CostEvaluatorResult eval(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Map<FlowCnec, Double> mnecsAndCost = flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toMap(Function.identity(), mnec -> computeMnecCost(flowResult, mnec)));
        Set<State> states = mnecsAndCost.keySet().stream().map(FlowCnec::getState).collect(Collectors.toSet());
        // TODO: optimize
        Map<State, Double> costPerState = states.stream().collect(Collectors.toMap(Function.identity(), state -> computeCostForState(flowResult, state)));

        List<FlowCnec> sortedMnecs = mnecsAndCost.entrySet().stream().filter(entry -> entry.getValue() != 0).sorted(Comparator.comparingDouble(Map.Entry::getValue)).map(Map.Entry::getKey).collect(Collectors.toList());
        Collections.reverse(sortedMnecs);
        return new SumCostEvaluatorResult(costPerState, new ArrayList<>(sortedMnecs));
    }

    protected double computeCostForState(FlowResult flowResult, State state) {
        return Math.abs(mnecViolationCost) < 1e-10 ? 0.0 : mnecViolationCost * flowCnecs.stream().filter(FlowCnec::isMonitored).filter(flowCnec -> state.equals(flowCnec.getState())).mapToDouble(mnec -> computeMnecCost(flowResult, mnec)).filter(mnecCost -> mnecCost != 0).sum();
    }
}
