/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.LoopFlowParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;

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

        public ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs,
                                                                              RaoParameters raoParameters) {

            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunction().relativePositiveMargins()) {
                marginEvaluator = FlowResult::getRelativeMargin;
            } else {
                marginEvaluator = FlowResult::getMargin;
            }
            this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(), marginEvaluator));

            return this.build();
        }

        public ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                              Set<FlowCnec> loopFlowCnecs,
                                              FlowResult initialFlowResult,
                                              FlowResult prePerimeterFlowResult,
                                              Set<String> operatorsNotToOptimizeInCurative,
                                              RaoParameters raoParameters) {

            SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
            if (searchTreeRaoParameters == null) {
                throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
            }

            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunction().relativePositiveMargins()) {
                marginEvaluator = FlowResult::getRelativeMargin;
            } else {
                marginEvaluator = FlowResult::getMargin;
            }

            if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()
                && !operatorsNotToOptimizeInCurative.isEmpty()) {

                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(),
                    new MarginEvaluatorWithUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));

            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(), marginEvaluator));
            }

            // mnec virtual cost evaluator
            if (raoParameters.isRaoWithMnecLimitation()) {

                this.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                    flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                    initialFlowResult,
                    MnecParameters.buildFromRaoParameters(raoParameters)
                ));
            }

            // loop-flow virtual cost evaluator
            if (raoParameters.isRaoWithLoopFlowLimitation()) {
                this.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                    loopFlowCnecs,
                    initialFlowResult,
                    LoopFlowParameters.buildFromRaoParameters(raoParameters)
                ));
            }

            // sensi fall-back overcost
            this.withVirtualCostEvaluator(new SensitivityFallbackOvercostEvaluator(raoParameters.getFallbackOverCost()));

            return this.build();
        }

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
