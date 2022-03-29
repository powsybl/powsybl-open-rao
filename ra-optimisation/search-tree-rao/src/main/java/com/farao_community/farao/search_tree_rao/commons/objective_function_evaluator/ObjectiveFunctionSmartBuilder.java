/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.LoopFlowParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.MnecParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class ObjectiveFunctionSmartBuilder {

    private ObjectiveFunctionSmartBuilder() {
        // utility class
    }

    public static ObjectiveFunction buildForInitialSensitivityComputation(Set<FlowCnec> flowCnecs,
                                                                          RaoParameters raoParameters) {

        ObjectiveFunction.ObjectiveFunctionBuilder builder = ObjectiveFunction.create();

        // min margin objective function
        MarginEvaluator marginEvaluator;
        if (raoParameters.getObjectiveFunction().relativePositiveMargins()) {
            marginEvaluator = FlowResult::getRelativeMargin;
        } else {
            marginEvaluator = FlowResult::getMargin;
        }
        builder.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(), marginEvaluator));

        return builder.build();
    }

    public static ObjectiveFunction build(Set<FlowCnec> flowCnecs,
                                          Set<FlowCnec> loopFlowCnecs,
                                          FlowResult initialFlowResult,
                                          FlowResult prePerimeterFlowResult,
                                          Set<String> operatorsNotToOptimizeInCurative,
                                          RaoParameters raoParameters) {

        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }

        ObjectiveFunction.ObjectiveFunctionBuilder builder = ObjectiveFunction.create();

        // min margin objective function
        MarginEvaluator marginEvaluator;
        if (raoParameters.getObjectiveFunction().relativePositiveMargins()) {
            marginEvaluator = FlowResult::getRelativeMargin;
        } else {
            marginEvaluator = FlowResult::getMargin;
        }

        if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()
            && !operatorsNotToOptimizeInCurative.isEmpty()) {

            builder.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(),
                new MarginEvaluatorWithUnoptimizedCnecs(marginEvaluator, operatorsNotToOptimizeInCurative, prePerimeterFlowResult)));

        } else {
            builder.withFunctionalCostEvaluator(new MinMarginEvaluator(flowCnecs, raoParameters.getObjectiveFunction().getUnit(), marginEvaluator));
        }

        // mnec virtual cost evaluator
        if (raoParameters.isRaoWithMnecLimitation()) {

            builder.withVirtualCostEvaluator(new MnecViolationCostEvaluator(
                flowCnecs.stream().filter(Cnec::isMonitored).collect(Collectors.toSet()),
                initialFlowResult,
                MnecParameters.buildFromRaoParameters(raoParameters)
            ));
        }

        // loop-flow virtual cost evaluator
        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            builder.withVirtualCostEvaluator(new LoopFlowViolationCostEvaluator(
                loopFlowCnecs,
                initialFlowResult,
                LoopFlowParameters.buildFromRaoParameters(raoParameters)
            ));
        }

        // sensi fall-back overcost
        builder.withVirtualCostEvaluator(new SensitivityFallbackOvercostEvaluator(raoParameters.getFallbackOverCost()));

        return builder.build();
    }
}
