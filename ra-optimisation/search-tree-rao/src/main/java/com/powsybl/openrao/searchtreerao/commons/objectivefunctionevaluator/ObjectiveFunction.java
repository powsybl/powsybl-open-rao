/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;

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

    public ObjectiveFunctionResult evaluate(FlowResult flowResult) {
        return new ObjectiveFunctionResultImpl(this, flowResult);
    }

    public static ObjectiveFunctionBuilder create() {
        return new ObjectiveFunctionBuilder();
    }

    public Set<FlowCnec> getFlowCnecs() {
        Set<FlowCnec> allFlowCnecs = new HashSet<>(functionalCostEvaluator.getFlowCnecs());
        virtualCostEvaluators.forEach(virtualCostEvaluator -> allFlowCnecs.addAll(virtualCostEvaluator.getFlowCnecs()));
        return allFlowCnecs;
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult) {
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult);
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult, contingenciesToExclude);
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, String virtualCostName, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> costEvaluator.computeCostAndLimitingElements(flowResult, contingenciesToExclude))
            .orElse(Pair.of(Double.NaN, new ArrayList<>()));
    }

    public static class ObjectiveFunctionBuilder {
        private CostEvaluator functionalCostEvaluator;
        private final List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();

        public ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs,
                                                                       RaoParameters raoParameters) {
            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getUnit(), marginEvaluator));

            // sensitivity failure over-cost should be computed on initial sensitivity result too
            // (this allows the RAO to prefer RAs that can remove sensitivity failures)
            double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);
            if (sensitivityFailureOvercost > 0) {
                this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, sensitivityFailureOvercost));
            }

            return this.build();
        }

        public ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                       Set<FlowCnec> loopFlowCnecs,
                                       FlowResult initialFlowResult,
                                       FlowResult prePerimeterFlowResult,
                                       Set<String> operatorsNotToOptimizeInCurative,
                                       RaoParameters raoParameters) {

            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            // Unoptimized cnecs in operatorsNotToOptimizeInCurative countries
            if (raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras()
                && !operatorsNotToOptimizeInCurative.isEmpty()) {

                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getUnit(),
                    new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));
            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getUnit(), marginEvaluator));
            }

            // mnec virtual cost evaluator
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                Optional<MnecParameters> mnecParametersOptional = raoParameters.getMnecParameters();
                Optional<com.powsybl.openrao.raoapi.parameters.extensions.MnecParameters> mnecParametersExtensionOptional = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getMnecParameters();
                if (mnecParametersOptional.isPresent() && mnecParametersExtensionOptional.isPresent()) {
                    this.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                        flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                        raoParameters.getObjectiveFunctionParameters().getUnit(),
                        initialFlowResult,
                        mnecParametersOptional.get().getAcceptableMarginDecrease(),
                        mnecParametersExtensionOptional.get().getViolationCost()
                    ));
                }
            }

            // loop-flow virtual cost evaluator
            if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
                Optional<LoopFlowParameters> loopFlowParametersOptional = raoParameters.getLoopFlowParameters();
                Optional<com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParameters> loopFlowParametersExtensionOptional = raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoopFlowParameters();
                if (loopFlowParametersOptional.isPresent() && loopFlowParametersExtensionOptional.isPresent()) {
                    this.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                        loopFlowCnecs,
                        initialFlowResult,
                        loopFlowParametersExtensionOptional.get().getViolationCost(),
                        loopFlowParametersOptional.get().getAcceptableIncrease()
                    ));
                }
            }

            // If sensi failed, create a high virtual cost via SensitivityFailureOvercostEvaluator
            // to ensure that corresponding leaf is not selected
            double sensitivityFailureOvercost = getSensitivityFailureOvercost(raoParameters);
            if (sensitivityFailureOvercost > 0) {
                this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, sensitivityFailureOvercost));
            }

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
