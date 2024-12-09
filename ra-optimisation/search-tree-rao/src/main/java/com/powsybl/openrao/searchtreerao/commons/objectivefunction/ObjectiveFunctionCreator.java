/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunction;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluator;
import com.powsybl.openrao.searchtreerao.commons.marginevaluator.MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.CostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.LoopFlowViolationCostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.MinMarginViolationEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.MnecViolationCostEvaluator;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.SensitivityFailureOvercostEvaluator;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class ObjectiveFunctionCreator extends AbstractObjectiveFunctionCreator {
    private final Set<FlowCnec> loopFlowCnecs;
    private final FlowResult initialFlowResult;
    private final FlowResult prePerimeterFlowResult;
    private final Set<String> operatorsNotToOptimizeInCurative;

    protected ObjectiveFunctionCreator(Set<FlowCnec> flowCnecs, Set<State> optimizedStates, RaoParameters raoParameters, Set<FlowCnec> loopFlowCnecs, FlowResult initialFlowResult, FlowResult prePerimeterFlowResult, Set<String> operatorsNotToOptimizeInCurative) {
        super(flowCnecs, optimizedStates, raoParameters);
        this.loopFlowCnecs = loopFlowCnecs;
        this.initialFlowResult = initialFlowResult;
        this.prePerimeterFlowResult = prePerimeterFlowResult;
        this.operatorsNotToOptimizeInCurative = operatorsNotToOptimizeInCurative;
    }

    @Override
    protected MarginEvaluator getMarginEvaluator() {
        // Unoptimized cnecs in operatorsNotToOptimizeInCurative countries
        return raoParameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras() && !operatorsNotToOptimizeInCurative.isEmpty() ? new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(super.getMarginEvaluator(), operatorsNotToOptimizeInCurative, prePerimeterFlowResult) : super.getMarginEvaluator();
    }

    @Override
    protected List<CostEvaluator> getVirtualCostEvaluators(MarginEvaluator marginEvaluator) {
        List<CostEvaluator> virtualCostEvaluators = new ArrayList<>();

        if (raoParameters.getObjectiveFunctionParameters().getType().costOptimization()) {
            virtualCostEvaluators.add(new MinMarginViolationEvaluator(flowCnecs, unit, marginEvaluator));
        }

        // mnec virtual cost evaluator
        if (raoParameters.hasExtension(MnecParametersExtension.class)) {
            virtualCostEvaluators.add(new MnecViolationCostEvaluator(
                flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                raoParameters.getObjectiveFunctionParameters().getType().getUnit(),
                initialFlowResult,
                raoParameters.getExtension(MnecParametersExtension.class)
            ));
        }

        // loop-flow virtual cost evaluator
        if (raoParameters.hasExtension(LoopFlowParametersExtension.class)) {
            virtualCostEvaluators.add(new LoopFlowViolationCostEvaluator(
                loopFlowCnecs,
                initialFlowResult,
                raoParameters.getExtension(LoopFlowParametersExtension.class)
            ));
        }

        // If sensi failed, create a high virtual cost via SensitivityFailureOvercostEvaluator
        // to ensure that corresponding leaf is not selected
        if (raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost() > 0) {
            virtualCostEvaluators.add(new SensitivityFailureOvercostEvaluator(flowCnecs, raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
        }

        return virtualCostEvaluators;
    }
}
