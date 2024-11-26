/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.commons.FlowCnecSorting;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.MaxCostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    protected Set<FlowCnec> flowCnecs;
    protected Unit unit;
    protected MarginEvaluator marginEvaluator;

    public MinMarginEvaluator(Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
    }

    @Override
    public String getName() {
        return "min-margin-evaluator";
    }

    @Override
    public CostEvaluatorResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        Set<State> states = flowCnecs.stream().map(FlowCnec::getState).collect(Collectors.toSet());
        Map<State, Double> costPerState = states.stream().collect(Collectors.toMap(Function.identity(), state -> computeCostForState(flowResult, getFlowCnecsOfState(state))));
        return new MaxCostEvaluatorResult(costPerState, FlowCnecSorting.sortByMargin(flowCnecs, unit, marginEvaluator, flowResult));
    }

    private double getHighestThresholdAmongFlowCnecs() {
        return flowCnecs.stream().map(this::getHighestThreshold).max(Double::compareTo).orElse(0.0);
    }

    private double getHighestThreshold(FlowCnec flowCnec) {
        return Math.max(
            Math.max(
                flowCnec.getUpperBound(TwoSides.ONE, unit).orElse(0.0),
                flowCnec.getUpperBound(TwoSides.TWO, unit).orElse(0.0)),
            Math.max(
                -flowCnec.getLowerBound(TwoSides.ONE, unit).orElse(0.0),
                -flowCnec.getLowerBound(TwoSides.TWO, unit).orElse(0.0)));
    }

    private Set<FlowCnec> getFlowCnecsOfState(State state) {
        return flowCnecs.stream().filter(flowCnec -> state.equals(flowCnec.getState())).filter(FlowCnec::isOptimized).collect(Collectors.toSet());
    }

    protected double computeCostForState(FlowResult flowResult, Set<FlowCnec> flowCnecsOfState) {
        List<FlowCnec> flowCnecsByMargin = flowCnecsOfState.stream().collect(Collectors.toMap(Function.identity(), flowCnec -> marginEvaluator.getMargin(flowResult, flowCnec, unit))).entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        FlowCnec limitingElement;
        if (flowCnecsByMargin.isEmpty()) {
            limitingElement = null;
        } else {
            limitingElement = flowCnecsByMargin.get(0);
        }
        if (limitingElement == null) {
            // In case there is no limiting element (may happen in perimeters where only MNECs exist),
            // return a finite value, so that the virtual cost is not hidden by the functional cost
            // This finite value should only be equal to the highest possible margin, i.e. the highest cnec threshold
            return -getHighestThresholdAmongFlowCnecs();
        }
        double margin = marginEvaluator.getMargin(flowResult, limitingElement, unit);
        if (margin >= Double.MAX_VALUE / 2) {
            // In case margin is infinite (may happen in perimeters where only unoptimized CNECs exist, none of which has seen its margin degraded),
            // return a finite value, like MNEC case above
            return -getHighestThresholdAmongFlowCnecs();
        }
        return -margin;
    }
}
