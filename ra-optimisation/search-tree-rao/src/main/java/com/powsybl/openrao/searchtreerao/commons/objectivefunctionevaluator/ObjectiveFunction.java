/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
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

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus) {
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus);
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, Set<String> contingenciesToExclude) {
        return functionalCostEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, contingenciesToExclude);
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, String virtualCostName) {
        return getVirtualCostAndCostlyElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, virtualCostName, new HashSet<>());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, RangeActionActivationResult rangeActionActivationResult, SensitivityResult sensitivityResult, ComputationStatus sensitivityStatus, String virtualCostName, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> costEvaluator.computeCostAndLimitingElements(flowResult, rangeActionActivationResult, sensitivityResult, sensitivityStatus, contingenciesToExclude))
            .orElse(Pair.of(Double.NaN, new ArrayList<>()));
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
            if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            // Unoptimized cnecs in series with psts
            this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator));
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
            if (raoParameters.getObjectiveFunctionParameters().getType().relativePositiveMargins()) {
                marginEvaluator = new BasicRelativeMarginEvaluator();
            } else {
                marginEvaluator = new BasicMarginEvaluator();
            }

            // Unoptimized cnecs in operatorsNotToOptimizeInCurative countries
            if (raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras()
                && !operatorsNotToOptimizeInCurative.isEmpty()) {

                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(),
                    new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));
                // Unoptimized cnecs in series with psts
            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunctionParameters().getType().getUnit(), marginEvaluator));
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
