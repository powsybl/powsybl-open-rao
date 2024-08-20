/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MinMarginEvaluator implements CostEvaluator {
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;

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
    public Unit getUnit() {
        return unit;
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        Map<FlowCnec, Double> margins = new HashMap<>();

        flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isOptimized)
            .forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));

        return margins.keySet().stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .toList();

    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        List<FlowCnec> costlyElements = getCostlyElements(flowResult, contingenciesToExclude);
        FlowCnec limitingElement;
        if (costlyElements.isEmpty()) {
            limitingElement = null;
        } else {
            limitingElement = costlyElements.get(0);
        }
        if (limitingElement == null) {
            // In case there is no limiting element (may happen in perimeters where only MNECs exist),
            // return a finite value, so that the virtual cost is not hidden by the functional cost
            // This finite value should only be equal to the highest possible margin, i.e. the highest cnec threshold
            return Pair.of(-getHighestThresholdAmongFlowCnecs(), costlyElements);
        }
        double margin = marginEvaluator.getMargin(flowResult, limitingElement, unit);
        if (margin >= Double.MAX_VALUE / 2) {
            // In case margin is infinite (may happen in perimeters where only unoptimized CNECs exist, none of which has seen its margin degraded),
            // return a finite value, like MNEC case above
            return Pair.of(-getHighestThresholdAmongFlowCnecs(), costlyElements);
        }
        return Pair.of(-margin, costlyElements);
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
}
