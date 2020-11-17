/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

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
        this.systematicSensitivityInterface = getSystematicSensitivityInterface();
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
        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            fillAbsolutePtdfSums(raoData, raoParameters.getExtension(RaoPtdfParameters.class).getBoundaries(), sensitivityResult);
        }
    }

    private void fillReferenceFlowsAndObjectiveFunction() {
        ObjectiveFunctionEvaluator objectiveFunction = RaoUtil.createObjectiveFunction(raoParameters);
        raoData.getCracResultManager().fillCnecResultWithFlows();
        raoData.getCracResultManager().fillCracResultWithCosts(
            objectiveFunction.getFunctionalCost(raoData), objectiveFunction.getVirtualCost(raoData));
    }

    private void fillReferenceLoopFlow() {
        LoopFlowComputation loopFlowComputation = new LoopFlowComputation(raoData.getGlskProvider(), raoData.getReferenceProgram());
        LoopFlowResult lfResults = loopFlowComputation.buildLoopFlowsFromReferenceFlowAndPtdf(raoData.getSystematicSensitivityResult(), raoData.getNetwork(), raoData.getLoopflowCnecs());
        raoData.getCracResultManager().fillCnecLoopFlowExtensionsWithInitialResults(lfResults, raoData.getNetwork(), raoParameters.getLoopFlowAcceptableAugmentation());
        raoData.getCracResultManager().fillCnecResultsWithLoopFlows(lfResults);
    }

    private SystematicSensitivityInterface getSystematicSensitivityInterface() {
        SystematicSensitivityInterface.SystematicSensitivityInterfaceBuilder builder = SystematicSensitivityInterface.builder()
            .withDefaultParameters(raoParameters.getDefaultSensitivityAnalysisParameters())
            .withFallbackParameters(raoParameters.getFallbackSensitivityAnalysisParameters())
            .withRangeActionSensitivities(raoData.getAvailableRangeActions(), raoData.getCnecs());

        if (raoParameters.getObjectiveFunction().doesRequirePtdf()) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getCrac().getCnecs());
        } else if (raoParameters.isRaoWithLoopFlowLimitation()) {
            builder.withPtdfSensitivities(raoData.getGlskProvider(), raoData.getLoopflowCnecs());
        }

        return builder.build();
    }

    private static void fillAbsolutePtdfSums(RaoData raoData, List<Pair<Country, Country>> boundaries, SystematicSensitivityResult sensitivityResult) {
        Map<Cnec, Double> ptdfSums = AbsolutePtdfSumsComputation.computeAbsolutePtdfSums(raoData.getCnecs(), raoData.getNetwork(), raoData.getGlskProvider(), boundaries, sensitivityResult);
        raoData.getCracResultManager().fillCnecResultsWithAbsolutePtdfSums(ptdfSums);
    }
}
