/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final FlowResult flowResult;
    private final SensitivityResult sensitivityResult;
    private boolean areCostsComputed;
    private Double functionalCost;
    private Double instantFunctionalCost;
    private Map<String, Double> virtualCosts;
    private Map<String, Double> instantVirtualCosts;
    private Map<FlowCnec, Double> orderedLimitingElementsAndCost;
    private Map<String, Map<FlowCnec, Double>> orderedCostlyElements;

    private Set<String> excludedContingencies;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       FlowResult flowResult,
                                       SensitivityResult sensitivityResult) {
        this.objectiveFunction = objectiveFunction;
        this.flowResult = flowResult;
        this.sensitivityResult = sensitivityResult;
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
    public double getInstantFunctionalCost() {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return instantFunctionalCost;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return orderedLimitingElementsAndCost.keySet().stream().limit(number).collect(Collectors.toList());
    }

    @Override
    public double getVirtualCost() {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        if (virtualCosts.size() > 0) {
            return virtualCosts.values().stream().mapToDouble(v -> v).sum();
        }
        return 0;
    }

    @Override
    public double getInstantVirtualCost() {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        if (instantVirtualCosts.size() > 0) {
            return instantVirtualCosts.values().stream().mapToDouble(v -> v).sum();
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
    public double getInstantVirtualCost(String virtualCostName) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return instantVirtualCosts.getOrDefault(virtualCostName, Double.NaN);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        if (!areCostsComputed) {
            computeCosts(new HashSet<>());
        }
        return orderedCostlyElements.get(virtualCostName).keySet().stream().limit(number).collect(Collectors.toList());
    }

    @Override
    public void excludeContingencies(Set<String> contingenciesToExclude) {
        if (!contingenciesToExclude.equals(excludedContingencies)) {
            computeCosts(contingenciesToExclude);
        }
    }

    private void computeCosts(Set<String> contingenciesToExclude) {
        Pair<Double, Map<FlowCnec, Double>> functionalCostAndLimitingElements = objectiveFunction.getFunctionalCostAndLimitingElements(flowResult, sensitivityResult, contingenciesToExclude);
        Instant firstInstant = functionalCostAndLimitingElements.getRight().keySet().stream().map(flowCnec -> flowCnec.getState().getInstant()).min(Comparator.comparingInt(Instant::getOrder)).orElseThrow();
        functionalCost = functionalCostAndLimitingElements.getLeft();
        if (firstInstant.isPreventive()) {
            instantFunctionalCost = functionalCostAndLimitingElements.getRight().entrySet().stream()
                .filter(e -> e.getKey().getState().getInstant().isPreventive() || e.getKey().getState().getInstant().isOutage())
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(-Double.MAX_VALUE);
        } else {
            instantFunctionalCost = functionalCostAndLimitingElements.getRight().entrySet().stream()
                .filter(e -> e.getKey().getState().getInstant().equals(firstInstant))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(-Double.MAX_VALUE);
        }
        orderedLimitingElementsAndCost = functionalCostAndLimitingElements.getRight();
        virtualCosts = new HashMap<>();
        orderedCostlyElements = new HashMap<>();
        getVirtualCostNames().forEach(vcn -> {
            Pair<Double, Map<FlowCnec, Double>> virtualCostAndCostlyElements = objectiveFunction.getVirtualCostAndCostlyElements(flowResult, sensitivityResult, vcn, contingenciesToExclude);
            virtualCosts.put(vcn, virtualCostAndCostlyElements.getLeft());
            orderedCostlyElements.put(vcn, virtualCostAndCostlyElements.getRight());
        });
        instantVirtualCosts = new HashMap<>();
        getVirtualCostNames().forEach(vcn -> {
            if (firstInstant.isPreventive()) {
                instantVirtualCosts.put(vcn, orderedCostlyElements.get(vcn).entrySet().stream()
                    .filter(e -> e.getKey().getState().getInstant().isPreventive() || e.getKey().getState().getInstant().isOutage())
                    .mapToDouble(Map.Entry::getValue)
                    .sum());
            } else {
                instantVirtualCosts.put(vcn, orderedCostlyElements.get(vcn).entrySet().stream()
                    .filter(e -> e.getKey().getState().getInstant().equals(firstInstant))
                    .mapToDouble(Map.Entry::getValue)
                    .sum());
            }
        });
        areCostsComputed = true;
        excludedContingencies = contingenciesToExclude;
    }
}
