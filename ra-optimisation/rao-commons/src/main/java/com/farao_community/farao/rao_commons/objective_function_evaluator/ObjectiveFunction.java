/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.ObjectiveFunctionResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ObjectiveFunction {
    private final CostEvaluator functionalCostEvaluator;
    private final List<CostEvaluator> virtualCostEvaluators;

    private ObjectiveFunction(CostEvaluator functionalCostEvaluator, List<CostEvaluator> virtualCostEvaluators) {
        this.functionalCostEvaluator = functionalCostEvaluator;
        this.virtualCostEvaluators = virtualCostEvaluators;
    }

    public ObjectiveFunctionResult evaluate(BranchResult branchResult, SensitivityStatus sensitivityStatus) {
        return new ObjectiveFunctionResultImpl(this, branchResult, sensitivityStatus);
    }

    public static ObjectiveFunctionBuilder create() {
        return new ObjectiveFunctionBuilder();
    }

    public double getFunctionalCost(BranchResult branchResult, SensitivityStatus sensitivityStatus) {
        return functionalCostEvaluator.computeCost(branchResult, sensitivityStatus);
    }

    public List<BranchCnec> getMostLimitingElements(BranchResult branchResult, int number) {
        return functionalCostEvaluator.getCostlyElements(branchResult, number);
    }

    public double getVirtualCost(BranchResult branchResult, SensitivityStatus sensitivityStatus) {
        return virtualCostEvaluators.stream()
                .mapToDouble(costEvaluator -> costEvaluator.computeCost(branchResult, sensitivityStatus))
                .sum();
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public double getVirtualCost(BranchResult branchResult, SensitivityStatus sensitivityStatus, String virtualCostName) {
        return virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny()
                .map(costEvaluator -> costEvaluator.computeCost(branchResult, sensitivityStatus))
                .orElse(Double.NaN);
    }

    public List<BranchCnec> getCostlyElements(BranchResult branchResult, String virtualCostName, int number) {
        Optional<CostEvaluator> optionalCostEvaluator =  virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny();
        if (optionalCostEvaluator.isPresent()) {
            return optionalCostEvaluator.get().getCostlyElements(branchResult, number);
        } else {
            return Collections.emptyList();
        }
    }

    public static class ObjectiveFunctionBuilder {
        private CostEvaluator functionalCostEvaluator;
        private final List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();

        public ObjectiveFunctionBuilder withFunctionalCostEvaluator(CostEvaluator costEvaluator) {
            this.functionalCostEvaluator = costEvaluator;
            return this;
        }

        public ObjectiveFunctionBuilder withVirtualCostEvaluator(CostEvaluator costEvaluator) {
            virtualCostEvaluators.add(costEvaluator);
            return this;
        }

        public ObjectiveFunction build() {
            Objects.requireNonNull(functionalCostEvaluator);
            return new ObjectiveFunction(functionalCostEvaluator, virtualCostEvaluators);
        }
    }
}
