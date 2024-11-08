/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ObjectiveFunction {
    private final FunctionalCostEvaluator functionalCostEvaluator;
    private final List<VirtualCostEvaluator> virtualCostEvaluators;

    private ObjectiveFunction(FunctionalCostEvaluator functionalCostEvaluator, List<VirtualCostEvaluator> virtualCostEvaluators) {
        this.functionalCostEvaluator = functionalCostEvaluator;
        this.virtualCostEvaluators = virtualCostEvaluators;
    }

    public ObjectiveFunctionResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        return new ObjectiveFunctionResultImpl(this, flowResult, remedialActionActivationResult);
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
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult, null);
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, contingenciesToExclude);
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(VirtualCostEvaluator::getName).collect(Collectors.toSet());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, String virtualCostName, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> costEvaluator.computeCostAndLimitingElements(flowResult, remedialActionActivationResult, contingenciesToExclude))
            .orElse(Pair.of(Double.NaN, new ArrayList<>()));
    }

    public static class ObjectiveFunctionBuilder {
        private FunctionalCostEvaluator functionalCostEvaluator;
        private final List<VirtualCostEvaluator> virtualCostEvaluators = new ArrayList<>();

        public ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs,
                                                                       RaoParameters raoParameters,
                                                                       Set<State> optimizedStates) {
            // min margin objective function
            MarginEvaluator marginEvaluator;
            if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                addEvaluatorsForCostlyOptimization(flowCnecs, raoParameters, optimizedStates, marginEvaluator);
            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator));
            }

            // sensitivity failure over-cost should be computed on initial sensitivity result too
            // (this allows the RAO to prefer RAs that can remove sensitivity failures)
            if (raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost() > 0) {
                this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
            }

            return this.build();
        }

        public ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                       Set<FlowCnec> loopFlowCnecs,
                                       FlowResult initialFlowResult,
                                       FlowResult prePerimeterFlowResult,
                                       Set<String> operatorsNotToOptimizeInCurative,
                                       RaoParameters raoParameters,
                                       Set<State> optimizedStates) {

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
                if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                    this.withFunctionalCostEvaluator(new RemedialActionCostEvaluator(optimizedStates, flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator, raoParameters.getRangeActionsOptimizationParameters()));
                } else {
                    this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(),
                        new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));
                }
            } else {
                if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                    addEvaluatorsForCostlyOptimization(flowCnecs, raoParameters, optimizedStates, marginEvaluator);
                } else {
                    this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator));
                }
            }

            // mnec virtual cost evaluator
            if (raoParameters.hasExtension(MnecParametersExtension.class)) {
                this.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                    flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                    raoParameters.getObjectiveFunctionParameters().getType().getUnit(),
                    initialFlowResult,
                    raoParameters.getExtension(MnecParametersExtension.class)
                ));
            }

            // loop-flow virtual cost evaluator
            if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
                this.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                    loopFlowCnecs,
                    initialFlowResult,
                    raoParameters.getExtension(LoopFlowParametersExtension.class)
                ));
            }

            // If sensi failed, create a high virtual cost via SensitivityFailureOvercostEvaluator
            // to ensure that corresponding leaf is not selected
            if (raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost() > 0) {
                this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
            }

            return this.build();
        }

        private void addEvaluatorsForCostlyOptimization(Set<FlowCnec> flowCnecs, RaoParameters raoParameters, Set<State> optimizedStates, MarginEvaluator marginEvaluator) {
            this.withFunctionalCostEvaluator(new RemedialActionCostEvaluator(optimizedStates, flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator, raoParameters.getRangeActionsOptimizationParameters()));
            this.withVirtualCostEvaluator(new OverloadEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator));
        }

        public ObjectiveFunctionBuilder withFunctionalCostEvaluator(FunctionalCostEvaluator costEvaluator) {
            this.functionalCostEvaluator = costEvaluator;
            return this;
        }

        public ObjectiveFunctionBuilder withVirtualCostEvaluator(VirtualCostEvaluator costEvaluator) {
            virtualCostEvaluators.add(costEvaluator);
            return this;
        }

        public ObjectiveFunction build() {
            Objects.requireNonNull(functionalCostEvaluator);
            return new ObjectiveFunction(functionalCostEvaluator, virtualCostEvaluators);
        }
    }
}
