/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final FlowResult flowResult;
    private boolean areCostsComputed;
    private Double functionalCost;
    private Map<String, Double> virtualCosts;
    private List<FlowCnec> orderedLimitingElements;
    private Map<String, List<FlowCnec>> orderedCostlyElements;

    private Set<String> excludedContingencies;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       FlowResult flowResult) {
        this.objectiveFunction = objectiveFunction;
        this.flowResult = flowResult;
        this.areCostsComputed = false;
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    @Override
    public double getFunctionalCost() {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return functionalCost;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return orderedLimitingElements.subList(0, Math.min(orderedLimitingElements.size(), number));
    }

    @Override
    public double getVirtualCost() {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        if (!virtualCosts.isEmpty()) {
            return virtualCosts.values().stream().mapToDouble(v -> v).sum();
        }
        return 0;
    }

    @Override
    public Set<String> getVirtualCostNames() {
        return objectiveFunction.getVirtualCostNames();
    }

    @Override
    public double getVirtualCost(String virtualCostName) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return virtualCosts.getOrDefault(virtualCostName, Double.NaN);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return orderedCostlyElements.get(virtualCostName).subList(0, Math.min(orderedCostlyElements.get(virtualCostName).size(), number));
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        if (!contingenciesToExclude.equals(excludedContingencies)) {
            computeCosts(contingenciesToExclude);
        }
    }

    private void computeCosts(Set<String> contingenciesToExclude) {
        Pair<Double, List<FlowCnec>> functionalCostAndLimitingElements = objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, contingenciesToExclude);
        functionalCost = functionalCostAndLimitingElements.getLeft();
        orderedLimitingElements = functionalCostAndLimitingElements.getRight();
        virtualCosts = new HashMap<>();
        orderedCostlyElements = new HashMap<>();
        getVirtualCostNames().forEach(vcn -> {
            Pair<Double, List<FlowCnec>> virtualCostAndCostlyElements = objectiveFunction.getVirtualCostAndCostlyElements(flowResult, vcn, contingenciesToExclude);
            virtualCosts.put(vcn, virtualCostAndCostlyElements.getLeft());
            orderedCostlyElements.put(vcn, virtualCostAndCostlyElements.getRight());
        });
        areCostsComputed = true;
        excludedContingencies = contingenciesToExclude;
    }
}
