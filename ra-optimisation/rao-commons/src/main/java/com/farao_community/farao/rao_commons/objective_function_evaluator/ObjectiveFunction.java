/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ObjectiveFunction {
    private final CostEvaluator functionalCostEvaluator;
    private final List<CostEvaluator> virtualCostEvaluators;

    private ObjectiveFunction(CostEvaluator functionalCostEvaluator, List<CostEvaluator> virtualCostEvaluators) {
        this.functionalCostEvaluator = functionalCostEvaluator;
        this.virtualCostEvaluators = virtualCostEvaluators;
    }

    public ObjectiveFunctionResult evaluate(FlowResult flowResult, ComputationStatus sensitivityStatus) {
        return new ObjectiveFunctionResultImpl(this, flowResult, sensitivityStatus);
    }

    public static ObjectiveFunctionBuilder create() {
        return new ObjectiveFunctionBuilder();
    }

    public double getFunctionalCost(FlowResult flowResult, ComputationStatus sensitivityStatus) {
        return functionalCostEvaluator.computeCost(flowResult, sensitivityStatus);
    }

    public List<FlowCnec> getMostLimitingElements(FlowResult flowResult, int number) {
        return functionalCostEvaluator.getCostlyElements(flowResult, number);
    }

    public double getVirtualCost(FlowResult flowResult, ComputationStatus sensitivityStatus) {
        return virtualCostEvaluators.stream()
                .mapToDouble(costEvaluator -> costEvaluator.computeCost(flowResult, sensitivityStatus))
                .sum();
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public double getVirtualCost(FlowResult flowResult, ComputationStatus sensitivityStatus, String virtualCostName) {
        return virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny()
                .map(costEvaluator -> costEvaluator.computeCost(flowResult, sensitivityStatus))
                .orElse(Double.NaN);
    }

    public List<FlowCnec> getCostlyElements(FlowResult flowResult, String virtualCostName, int number) {
        Optional<CostEvaluator> optionalCostEvaluator =  virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny();
        if (optionalCostEvaluator.isPresent()) {
            return optionalCostEvaluator.get().getCostlyElements(flowResult, number);
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
