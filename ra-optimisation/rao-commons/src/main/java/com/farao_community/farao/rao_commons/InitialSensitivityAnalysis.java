/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_computation.SystematicSensitivityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class aims at performing the initial sensitivity analysis of a RAO, the one
 * which defines the pre-optimisation variant. It is common to both the Search Tree
 * and the Linear RAO.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InitialSensitivityAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialSensitivityAnalysis.class);
    private RaoData raoData;
    private RaoParameters raoParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;

    public InitialSensitivityAnalysis(RaoData raoData, RaoParameters raoParameters) {
        this.raoData = raoData;
        this.raoParameters = raoParameters;
        this.systematicSensitivityInterface = RaoUtil.createSystematicSensitivityInterface(raoParameters, raoData);
    }

    public void run() {
        LOGGER.info("Initial systematic analysis [start]");

        runSensitivityComputation();

        if (raoParameters.isRaoWithLoopFlowLimitation()) {
            LOGGER.info("Initial systematic analysis [...] - fill reference loop-flow values");
            fillReferenceLoopFlow();
        }

        fillReferenceFlowsAndObjectiveFunction();

        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -raoData.getCracResult().getFunctionalCost());
    }

    private void runSensitivityComputation() {
        SystematicSensitivityResult sensitivityResult = systematicSensitivityInterface.run(raoData.getNetwork(), raoParameters.getObjectiveFunction().getUnit());
        raoData.setSystematicSensitivityResult(sensitivityResult);
    }

    private void fillReferenceFlowsAndObjectiveFunction() {
        ObjectiveFunctionEvaluator objectiveFunction = RaoUtil.createObjectiveFunction(raoParameters);
        raoData.getRaoDataManager().fillCnecResultWithFlows();
        raoData.getRaoDataManager().fillCracResultWithCosts(
            objectiveFunction.getFunctionalCost(raoData),
            (systematicSensitivityInterface.isFallback() ? raoParameters.getFallbackOverCost() : 0)
                + objectiveFunction.getVirtualCost(raoData));
    }

    private void fillReferenceLoopFlow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getCrac(), raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getNetwork());
        raoData.getRaoDataManager().fillCnecLoopExtensionsWithInitialResults(lfResults, raoData.getNetwork());
        raoData.getRaoDataManager().fillCnecResultsWithLoopFlows(lfResults);
    }
}
