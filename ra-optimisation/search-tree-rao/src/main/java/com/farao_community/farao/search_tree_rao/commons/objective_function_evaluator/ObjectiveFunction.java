/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.farao_community.farao.search_tree_rao.commons.parameters.LoopFlowParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.UnoptimizedCnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.*;

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

    public ObjectiveFunctionResult evaluate(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return new ObjectiveFunctionResultImpl(this, flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
    }

    public static ObjectiveFunctionBuilder create() {
        return new ObjectiveFunctionBuilder();
    }

    public Set<FlowCnec> getFlowCnecs() {
        Set<FlowCnec> allFlowCnecs = new HashSet<>(functionalCostEvaluator.getFlowCnecs());
        virtualCostEvaluators.forEach(virtualCostEvaluator -> allFlowCnecs.addAll(virtualCostEvaluator.getFlowCnecs()));
        return allFlowCnecs;
    }

    public double getFunctionalCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return functionalCostEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
    }

    public double getFunctionalCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        return functionalCostEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, contingenciesToExclude);
    }

    public List<FlowCnec> getMostLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, int number) {
        return functionalCostEvaluator.getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, number);
    }

    public double getVirtualCost(FlowResult flowResult,  RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return virtualCostEvaluators.stream()
                .mapToDouble(costEvaluator -> costEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus))
                .sum();
    }

    public double getVirtualCost(FlowResult flowResult,  RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .mapToDouble(costEvaluator -> costEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, contingenciesToExclude))
            .sum();
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public double getVirtualCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, String virtualCostName) {
        return virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny()
                .map(costEvaluator -> costEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus))
                .orElse(Double.NaN);
    }

    public double getVirtualCost(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, String virtualCostName, Set<String> contingenciesToExlude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> costEvaluator.computeCost(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, contingenciesToExlude))
            .orElse(Double.NaN);
    }

    public List<FlowCnec> getCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, String virtualCostName, int number) {
        Optional<CostEvaluator> optionalCostEvaluator =  virtualCostEvaluators.stream()
                .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
                .findAny();
        if (optionalCostEvaluator.isPresent()) {
            return optionalCostEvaluator.get().getCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, number);
        } else {
            return Collections.emptyList();
        }
    }

    public static class ObjectiveFunctionBuilder {
        private CostEvaluator functionalCostEvaluator;
        private final List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();

        public ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs,
                                                                       RaoParameters raoParameters,
                                                                       Crac crac,
                                                                       RangeActionSetpointResult prePerimeterRangeActionSetpointResult) {
            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            // Unoptimized cnecs in series with psts
            if (!raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(),
                        new MarginEvaluatorWithPstLimitationUnoptimizedCnecs(marginEvaluator, UnoptimizedCnecParameters.getUnoptimizedCnecsInSeriesWithPsts(raoParameters.getNotOptimizedCnecsParameters(), crac), prePerimeterRangeActionSetpointResult)));
            } else  {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(), marginEvaluator));
            }
            return this.build();
        }

        public ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                       Set<FlowCnec> loopFlowCnecs,
                                       FlowResult initialFlowResult,
                                       FlowResult prePerimeterFlowResult,
                                       RangeActionSetpointResult prePerimeterRangeActionSetpointResult,
                                       Crac crac,
                                       Set<String> operatorsNotToOptimizeInCurative,
                                       RaoParameters raoParameters) {

            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            // Unoptimized cnecs in operatorsNotToOptimizeInCurative countries
            if (raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras()
                && !operatorsNotToOptimizeInCurative.isEmpty()) {

                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(),
                    new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));
                // Unoptimized cnecs in series with psts
            } else if (!raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(),
                        new MarginEvaluatorWithPstLimitationUnoptimizedCnecs(marginEvaluator, UnoptimizedCnecParameters.getUnoptimizedCnecsInSeriesWithPsts(raoParameters.getNotOptimizedCnecsParameters(), crac), prePerimeterRangeActionSetpointResult)));
            } else  {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(), marginEvaluator));
            }

            // mnec virtual cost evaluator
            MnecParametersExtension mnecParameters = raoParameters.getExtension(MnecParametersExtension.class);
            if (Objects.nonNull(mnecParameters)) {
                this.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                    flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                    raoParameters.getObjectiveFunctionParameters().getObjectiveFunctionType().getUnit(),
                    initialFlowResult,
                    MnecParameters.buildFromRaoParameters(raoParameters)
                ));
            }

            // loop-flow virtual cost evaluator
            LoopFlowParametersExtension loopFlowParameters = raoParameters.getExtension(LoopFlowParametersExtension.class);
            if (Objects.nonNull(loopFlowParameters)) {
                this.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                    loopFlowCnecs,
                    initialFlowResult,
                    LoopFlowParameters.buildFromRaoParameters(raoParameters)
                ));
            }

            // If sensi failed, create a high virtual cost via SensitivityFailureOvercostEvaluator
            // to ensure that corresponding leaf is not selected
            this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));

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
