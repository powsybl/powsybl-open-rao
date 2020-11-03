/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_commons.LoopFlowUtil;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerWithLoopFlows extends IteratingLinearOptimizer {

    private boolean loopFlowApproximation;

    public IteratingLinearOptimizerWithLoopFlows(List<ProblemFiller> fillers,
                                                 SystematicSensitivityInterface systematicSensitivityInterface,
                                                 ObjectiveFunctionEvaluator objectiveFunctionEvaluator,
                                                 IteratingLinearOptimizerWithLoopFLowsParameters parameters) {
        super(fillers, systematicSensitivityInterface, objectiveFunctionEvaluator, parameters);
        loopFlowApproximation = parameters.isLoopflowApproximation();
        linearOptimizer = new LinearOptimizer(fillers);
    }

    @Override
    void runSensitivityAndUpdateResults() {

        raoData.setSystematicSensitivityResult(
            systematicSensitivityInterface.run(raoData.getNetwork(), objectiveFunctionEvaluator.getUnit()));

        LoopFlowUtil.buildLoopFlowsWithLatestSensi(raoData, loopFlowApproximation);

        raoData.getCracResultManager().fillCnecResultWithFlows();
        raoData.getCracResultManager().fillCracResultWithCosts(objectiveFunctionEvaluator.getFunctionalCost(raoData), objectiveFunctionEvaluator.getVirtualCost(raoData));

    }
}
