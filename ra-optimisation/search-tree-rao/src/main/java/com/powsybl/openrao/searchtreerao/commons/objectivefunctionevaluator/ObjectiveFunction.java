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
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.BasicMarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.BasicRelativeMarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs;
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
    private final CnecMarginManager cnecMarginManager;

    private ObjectiveFunction(CostEvaluator functionalCostEvaluator, List<CostEvaluator> virtualCostEvaluators, CnecMarginManager cnecMarginManager) {
        this.functionalCostEvaluator = functionalCostEvaluator;
        this.virtualCostEvaluators = virtualCostEvaluators;
        this.cnecMarginManager = cnecMarginManager;
    }

    public ObjectiveFunctionResult evaluate(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult) {
        return new ObjectiveFunctionResultImpl(this, flowResult, remedialActionActivationResult);
    }

    public static ObjectiveFunctionBuilder create() {
        return new ObjectiveFunctionBuilder();
    }

    public Set<FlowCnec> getFlowCnecs() {
        return new HashSet<>(cnecMarginManager.flowCnecs());
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult) {
        return getFunctionalCostAndLimitingElements(flowResult, null, Set.of());
    }

    public Pair<Double, List<FlowCnec>> getFunctionalCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        return Pair.of(functionalCostEvaluator.evaluate(flowResult, remedialActionActivationResult, contingenciesToExclude), cnecMarginManager.sortFlowCnecsByMargin(flowResult, contingenciesToExclude));
    }

    public Set<String> getVirtualCostNames() {
        return virtualCostEvaluators.stream().map(CostEvaluator::getName).collect(Collectors.toSet());
    }

    public Pair<Double, List<FlowCnec>> getVirtualCostAndCostlyElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, String virtualCostName, Set<String> contingenciesToExclude) {
        return virtualCostEvaluators.stream()
            .filter(costEvaluator -> costEvaluator.getName().equals(virtualCostName))
            .findAny()
            .map(costEvaluator -> Pair.of(costEvaluator.evaluate(flowResult, remedialActionActivationResult, contingenciesToExclude), getVirtualCostCostlyElements(costEvaluator, flowResult, contingenciesToExclude)))
            .orElse(Pair.of(Double.NaN, new ArrayList<>()));
    }

    private static List<FlowCnec> getVirtualCostCostlyElements(CostEvaluator virtualCostEvaluator, FlowResult flowResult, Set<String> contingenciesToExclude) {
        return virtualCostEvaluator instanceof CnecViolationCostEvaluator violationCostEvaluator ? violationCostEvaluator.getElementsInViolation(flowResult, contingenciesToExclude) : new ArrayList<>();
    }

    public static class ObjectiveFunctionBuilder {
        private CostEvaluator functionalCostEvaluator;
        private final List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();
        private CnecMarginManager cnecMarginManager;

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

            CnecMarginManager cmm = new CnecMarginManager(flowCnecs, marginEvaluator, raoParameters.getObjectiveFunctionParameters().getType().getUnit());

            if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                addEvaluatorsForCostlyOptimization(optimizedStates, cmm);
            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(cmm));
            }

            // sensitivity failure over-cost should be computed on initial sensitivity result too
            // (this allows the RAO to prefer RAs that can remove sensitivity failures)
            if (raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost() > 0) {
                this.withVirtualCostEvaluator(new SensitivityFailureOvercostEvaluator(flowCnecs, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
            }

            this.withCnecMarginManager(cmm);
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
                marginEvaluator = new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult);
            }

            CnecMarginManager cmm = new CnecMarginManager(flowCnecs, marginEvaluator, raoParameters.getObjectiveFunctionParameters().getType().getUnit());

            if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
                addEvaluatorsForCostlyOptimization(optimizedStates, cmm);
            } else {
                this.withFunctionalCostEvaluator(new MinMarginEvaluator(cmm));
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

            this.withCnecMarginManager(cmm);
            return this.build();
        }

        private void addEvaluatorsForCostlyOptimization(Set<State> optimizedStates, CnecMarginManager cnecMarginManager) {
            this.withFunctionalCostEvaluator(new RemedialActionCostEvaluator(optimizedStates));
            this.withVirtualCostEvaluator(new MinMarginViolationEvaluator(cnecMarginManager));
        }

        public ObjectiveFunctionBuilder withFunctionalCostEvaluator(CostEvaluator costEvaluator) {
            this.functionalCostEvaluator = costEvaluator;
            return this;
        }

        public ObjectiveFunctionBuilder withVirtualCostEvaluator(CostEvaluator costEvaluator) {
            virtualCostEvaluators.add(costEvaluator);
            return this;
        }

        public ObjectiveFunctionBuilder withCnecMarginManager(CnecMarginManager cnecMarginManager) {
            this.cnecMarginManager = cnecMarginManager;
            return this;
        }

        public ObjectiveFunction build() {
            Objects.requireNonNull(functionalCostEvaluator);
            Objects.requireNonNull(cnecMarginManager);
            return new ObjectiveFunction(functionalCostEvaluator, virtualCostEvaluators, cnecMarginManager);
        }
    }
}
