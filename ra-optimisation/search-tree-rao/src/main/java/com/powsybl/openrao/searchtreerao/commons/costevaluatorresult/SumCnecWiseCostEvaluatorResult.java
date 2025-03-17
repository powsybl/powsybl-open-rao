/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.costevaluatorresult;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.DoubleStream;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class SumCnecWiseCostEvaluatorResult implements CostEvaluatorResult {
    private final Map<FlowCnec, Double> costPerCnec;
    private final List<FlowCnec> costlyElements;

    public SumCnecWiseCostEvaluatorResult(Map<FlowCnec, Double> costPerCnec, List<FlowCnec> costlyElements) {
        this.costPerCnec = costPerCnec;
        this.costlyElements = costlyElements;
    }

    @Override
    public double getCost(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {

        DoubleStream filteredCosts = costPerCnec.entrySet().stream()
            .filter(entry -> cnecMustBeKept(entry.getKey(), contingenciesToExclude, cnecsToExclude))
            .mapToDouble(Map.Entry::getValue);
        return filteredCosts.sum();
    }

    @Override
    public List<FlowCnec> getCostlyElements(Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        return costlyElements.stream().filter(flowCnec -> cnecMustBeKept(flowCnec, contingenciesToExclude, cnecsToExclude)).toList();
    }

    private static boolean cnecMustBeKept(FlowCnec flowCnec, Set<String> contingenciesToExclude, Set<String> cnecsToExclude) {
        Optional<Contingency> contingency = flowCnec.getState().getContingency();
        return (contingency.isEmpty() || !contingenciesToExclude.contains(contingency.get().getId())) && !cnecsToExclude.contains(flowCnec.getId());
    }
}
