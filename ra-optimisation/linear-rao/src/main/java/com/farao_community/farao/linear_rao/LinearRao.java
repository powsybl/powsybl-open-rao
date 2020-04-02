/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.linear_rao.config.LinearRaoConfigurationUtil;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRao implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRao.class);

    @Override
    public String getName() {
        return "LinearRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public CompletableFuture<RaoResult> run2(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters parameters) {
        // qualityCheck(RaoParameters);

        LinearRaoParameters linearRaoParameters = parameters.getExtension(LinearRaoParameters.class);

        LinearRaoInitialSituation initialSituation = new LinearRaoInitialSituation(crac);

        initialSituation.evaluateSensiAndCost(network, computationManager, linearRaoParameters.getSensitivityComputationParameters());
        initialSituation.completeResults();


        // initialSituation.completeResults(crac)

        // if ! doOptim() break

        // initiate LP

        /* for(it = ...) {

            situation = new LinearRaoOptimizedSituation()

            situation.solveLp(LP)
            situation.evaluateSensiAndCost()




         */



    }


    @Override
    public CompletableFuture<RaoResult> run(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters parameters) {

        ResultBuilder resultBuilder = new ResultBuilder(crac);

        // quality check
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }

        // Initiate result variants
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }
        String preOptimVariant = resultVariantManager.createNewUniqueVariantId();
        String bestResultVariant = resultVariantManager.createNewUniqueVariantId();

        LinearRaoParameters linearRaoParameters = parameters.getExtensionByName("LinearRaoParameters");
        SensitivityComputationParameters sensitivityComputationParameters = linearRaoParameters.getSensitivityComputationParameters();

        // Initiate sensitivity analysis results
        SystematicSensitivityAnalysisResult currentSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        // Failure if some sensitivities are not computed
        if (currentSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || currentSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            resultVariantManager.deleteVariants(preOptimVariant, bestResultVariant);
            return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
        }

        double bestScore = 0;

        try {
            bestScore = getMinMargin(crac, currentSensitivityAnalysisResult);
        } catch (FaraoException e) {
            resultVariantManager.deleteVariants(preOptimVariant, bestResultVariant);
            throw e;
        }

        // Complete result extensions for pre optim variant
        resultBuilder.updateResultExtensions(bestScore, preOptimVariant, currentSensitivityAnalysisResult);
        resultBuilder.fillPreOptimRangeActionResultsFromNetwork(preOptimVariant, network);

        // Check if we need to optimize Range Actions
        if (linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty()) {
            resultBuilder.updateResultExtensions(bestScore, bestResultVariant, currentSensitivityAnalysisResult);
            return CompletableFuture.completedFuture(resultBuilder.buildRaoResult(bestScore, preOptimVariant, bestResultVariant));
        }

        // Initiate the LP
        LinearRaoModeller linearRaoModeller = createLinearRaoModeller(crac, network, currentSensitivityAnalysisResult, parameters);
        linearRaoModeller.buildProblem();

        // Initiate looping variables
        RaoResult raoResult;
        String currentResultVariant = resultVariantManager.createNewUniqueVariantId();
        SystematicSensitivityAnalysisResult bestOptimSensitivityAnalysisResult = currentSensitivityAnalysisResult;

        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {
            raoResult = linearRaoModeller.solve(currentResultVariant);
            if (raoResult.getStatus() == RaoResult.Status.FAILURE) {
                resultVariantManager.deleteVariants(preOptimVariant, bestResultVariant, currentResultVariant);
                return CompletableFuture.completedFuture(raoResult);
            }

            if (sameRemedialActions(crac, bestResultVariant, currentResultVariant)) {
                break;
            }

            applyRAs(crac, network, currentResultVariant);
            currentSensitivityAnalysisResult = SystematicSensitivityAnalysisService
                .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

            // If some sensitivities are not computed, the bes result found so far is returned
            if (currentSensitivityAnalysisResult.getStateSensiMap().containsValue(null)) {
                break;
            }

            double newScore = 0;
            try {
                newScore = getMinMargin(crac, currentSensitivityAnalysisResult);
            } catch (FaraoException e) {
                resultVariantManager.deleteVariants(preOptimVariant, bestResultVariant);
                throw e;
            }
            if (newScore < bestScore) {
                // TODO : limit the ranges
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} to {}", bestScore, newScore);
                break;
            }

            bestOptimSensitivityAnalysisResult = currentSensitivityAnalysisResult;
            bestScore = newScore;
            resultVariantManager.deleteVariant(bestResultVariant);
            bestResultVariant = currentResultVariant;
            currentResultVariant = resultVariantManager.createNewUniqueVariantId();
            linearRaoModeller.updateProblem(network, currentSensitivityAnalysisResult);
        }

        resultVariantManager.deleteVariant(currentResultVariant);
        resultBuilder.updateResultExtensions(bestScore, bestResultVariant, bestOptimSensitivityAnalysisResult);
        return CompletableFuture.completedFuture(resultBuilder.buildRaoResult(bestScore, preOptimVariant, bestResultVariant));
    }

    //defined to be able to run unit tests
    LinearRaoModeller createLinearRaoModeller(Crac crac,
                                              Network network,
                                              SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult,
                                              RaoParameters raoParameters) {
        return new LinearRaoModeller(crac, network, systematicSensitivityAnalysisResult, new LinearRaoProblem(), raoParameters);
    }

    private boolean sameRemedialActions(Crac crac, String resultVariant1, String resultVariant2) {
        //TODO: manage curative RA
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(resultVariant1).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(resultVariant2).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    private void applyRAs(Crac crac, Network network, String variantId) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(network, rangeActionResultMap.getVariant(variantId).getSetPoint(preventiveState));
        }
    }

}
