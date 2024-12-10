/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.commons.costevaluatorresult.CostEvaluatorResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final CostEvaluatorResult functionalCostResult;
    private final Map<String, CostEvaluatorResult> virtualCostResults;
    private final List<FlowCnec> flowCnecsByMargin;
    private Set<String> excludedContingencies;

    public ObjectiveFunctionResultImpl(CostEvaluatorResult functionalCostResult, Map<String, CostEvaluatorResult> virtualCostResults, List<FlowCnec> flowCnecsByMargin) {
        this.functionalCostResult = functionalCostResult;
        this.virtualCostResults = virtualCostResults;
        this.flowCnecsByMargin = flowCnecsByMargin;
        this.excludedContingencies = new HashSet<>();
    }

    @Override
    public double getFunctionalCost() {
        return functionalCostResult.getCost(excludedContingencies);
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        List<FlowCnec> filteredFlowCnecs = flowCnecsByMargin.stream().filter(flowCnec -> flowCnec.getState().getContingency().isEmpty() || flowCnec.getState().getContingency().isPresent() && !excludedContingencies.contains(flowCnec.getState().getContingency().get().getId())).toList();
        return filteredFlowCnecs.subList(0, Math.min(filteredFlowCnecs.size(), number));
    }

    @Override
    public double getVirtualCost() {
        return virtualCostResults.values().stream().mapToDouble(result -> result.getCost(excludedContingencies)).sum();
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return virtualCostResults.keySet();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        return virtualCostResults.containsKey(virtualCostName) ? virtualCostResults.get(virtualCostName).getCost(excludedContingencies) : Double.NaN;
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        List<FlowCnec> costlyElements = virtualCostResults.get(virtualCostName).getCostlyElements(excludedContingencies);
        return costlyElements.subList(0, Math.min(costlyElements.size(), number));
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        this.excludedContingencies = new HashSet<>(contingenciesToExclude);
    }
}
