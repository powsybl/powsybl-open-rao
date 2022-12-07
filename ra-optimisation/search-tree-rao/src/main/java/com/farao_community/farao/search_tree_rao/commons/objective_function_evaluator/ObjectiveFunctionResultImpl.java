/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunctionResultImpl implements ObjectiveFunctionResult {
    private final ObjectiveFunction objectiveFunction;
    private final FlowResult flowResult;
    private final RangeActionActivationResult rangeActionActivationResult;
    private final SensitivityResult sensitivityResult;
    private final ComputationStatus sensitivityStatus;
    private boolean areCostComputed;
    private Double functionalCost;
    private Map<String, Double> virtualCosts;

    public ObjectiveFunctionResultImpl(ObjectiveFunction objectiveFunction,
                                       FlowResult flowResult,
                                       RangeActionActivationResult rangeActionActivationResult,
                                       SensitivityResult sensitivityResult,
                                       ComputationStatus sensitivityStatus) {
        this.objectiveFunction = objectiveFunction;
        this.flowResult = flowResult;
        this.rangeActionActivationResult = rangeActionActivationResult;
        this.sensitivityResult = sensitivityResult;
        this.sensitivityStatus = sensitivityStatus;
        this.areCostComputed = false;
    }

    @Override
    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    @Override
    public double getFunctionalCost() {
        if (!areCostComputed) {
            computeCosts();
        }
        return functionalCost;
    }

    @Override
    public List<FlowCnec> getMostLimitingElements(int number) {
        return objectiveFunction.getMostLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, number);
    }

    @Override
    public double getVirtualCost() {
        if (!areCostComputed) {
            computeCosts();
        }
        if (virtualCosts.size() > 0) {
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
        if (!areCostComputed) {
            computeCosts();
        }
        return virtualCosts.getOrDefault(virtualCostName, Double.NaN);
    }

    @Override
    public List<FlowCnec> getCostlyElements(String virtualCostName, int number) {
        return objectiveFunction.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, virtualCostName, number);
    }

    private void computeCosts() {
        functionalCost = objectiveFunction.getFunctionalCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
        virtualCosts = new HashMap<>();
        getVirtualCostNames().forEach(vcn -> virtualCosts.put(vcn, objectiveFunction.getVirtualCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, vcn)));
        areCostComputed = true;
    }
}
