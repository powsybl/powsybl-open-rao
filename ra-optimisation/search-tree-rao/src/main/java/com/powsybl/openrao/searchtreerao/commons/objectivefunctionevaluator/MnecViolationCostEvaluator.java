/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An evaluator that computes the virtual cost resulting from the violation of
 * the MNEC minimum margin soft constraint
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MnecViolationCostEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final FlowResult initialFlowResult;
    private final double mnecAcceptableMarginDecrease;
    private final double mnecViolationCost;

    public MnecViolationCostEvaluator(Set<FlowCnec> flowCnecs, Unit unit, FlowResult initialFlowResult, double mnecAcceptableMarginDecrease, double mnecViolationCost) {
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.initialFlowResult = initialFlowResult;
        this.mnecAcceptableMarginDecrease = mnecAcceptableMarginDecrease;
        this.mnecViolationCost = mnecViolationCost;
    }

    @Override
    public String getName() {
        return "mnec-cost";
    }

    private double computeCost(FlowResult flowResult, FlowCnec mnec) {
        double initialMargin = initialFlowResult.getMargin(mnec, unit);
        double currentMargin = flowResult.getMargin(mnec, unit);
        return Math.max(0, Math.min(0, initialMargin - mnecAcceptableMarginDecrease) - currentMargin);
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        if (Math.abs(mnecViolationCost) < 1e-10) {
            return Pair.of(0., new ArrayList<>());
        }
        double totalMnecMarginViolation = 0;
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, contingenciesToExclude);
        for (FlowCnec mnec : costlyElements) {
            Optional<Contingency> contingency = mnec.getState().getContingency();
            if (mnec.isMonitored() && (mnec.getState().getContingency().isEmpty() || contingency.isPresent() && !contingenciesToExclude.contains(contingency.get().getId()))) {
                totalMnecMarginViolation += computeCost(flowResult, mnec);
            }
        }
        return Pair.of(mnecViolationCost * totalMnecMarginViolation, costlyElements);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> sortedElements = flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isMonitored)
            .collect(Collectors.toMap(
                Function.identity(),
                cnec -> computeCost(flowResult, cnec)
            ))
            .entrySet().stream()
            .filter(entry -> entry.getValue() != 0)
            .sorted(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        Collections.reverse(sortedElements);

        return sortedElements;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
