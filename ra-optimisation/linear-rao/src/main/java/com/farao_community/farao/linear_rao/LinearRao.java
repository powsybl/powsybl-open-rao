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
import com.farao_community.farao.util.SensitivityComputationException;
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
    public enum ActiveSensiParameters {
        DEFAULT,
        FALLBACK
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRao.class);
    private ActiveSensiParameters activeSensiParameters;

    @Override
    public String getName() {
        return "LinearRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters parameters) {
        activeSensiParameters = ActiveSensiParameters.DEFAULT;

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
        SystematicSensitivityAnalysisResult currentSensitivityAnalysisResult;
        try {
            currentSensitivityAnalysisResult = runWithParamSelection(network, crac, computationManager, linearRaoParameters);
        } catch (SensitivityComputationException e) { // Failure if some sensitivities are not computed
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
        updateResultExtensions(crac, bestScore, preOptimVariant, currentSensitivityAnalysisResult);
        fillPreOptimRangeActionResultsFromNetwork(crac, preOptimVariant, network);

        // Check if we need to optimize Range Actions
        if (linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty()) {
            updateResultExtensions(crac, bestScore, bestResultVariant, currentSensitivityAnalysisResult);
            return CompletableFuture.completedFuture(buildRaoResult(bestScore, preOptimVariant, bestResultVariant));
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

            try { // If some sensitivities are not computed, the best result found so far is returned
                currentSensitivityAnalysisResult = runWithParamSelection(network, crac, computationManager, linearRaoParameters);
            } catch (SensitivityComputationException e) {
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
        updateResultExtensions(crac, bestScore, bestResultVariant, bestOptimSensitivityAnalysisResult);
        return CompletableFuture.completedFuture(buildRaoResult(bestScore, preOptimVariant, bestResultVariant));
    }

    private SensitivityComputationParameters selectSensiParameters(LinearRaoParameters parameters) {

        if (activeSensiParameters.equals(ActiveSensiParameters.DEFAULT)) {
            return parameters.getSensitivityComputationParameters();
        } else if (activeSensiParameters.equals(ActiveSensiParameters.FALLBACK) && parameters.getFallbackSensiParameters() != null) {
            return parameters.getFallbackSensiParameters();
        } else {
            throw new SensitivityComputationException(String.format("No sensitivity parameters available for %s configuration.", activeSensiParameters.toString()));
        }
    }

    private SystematicSensitivityAnalysisResult runSensi(Network network,
                                                         Crac crac,
                                                         ComputationManager computationManager,
                                                         LinearRaoParameters parameters) {

        // Run sensitivity analysis
        SystematicSensitivityAnalysisResult sensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, selectSensiParameters(parameters));
        if (sensitivityAnalysisResult.getStateSensiMap().containsValue(null) || sensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            throw new SensitivityComputationException(String.format("Sensitivity computation failed with %s sensitivity parameters.", activeSensiParameters.toString()));
        } else {
            return sensitivityAnalysisResult;
        }
    }

    private SystematicSensitivityAnalysisResult runWithParamSelection(Network network,
                                                                      Crac crac,
                                                                      ComputationManager computationManager,
                                                                      LinearRaoParameters parameters) {
        try { // with default parameters
            return runSensi(network, crac, computationManager, parameters);
        } catch (SensitivityComputationException e1) {
            activeSensiParameters = ActiveSensiParameters.FALLBACK;
            try { // with fallback parameters
                return runSensi(network, crac, computationManager, parameters);
            } catch (SensitivityComputationException e2) {
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.");
            }
        }
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

    private double getMinMargin(Crac crac, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : crac.getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    private void updateCnecExtensions(Crac crac, String resultVariantId, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        crac.getCnecs().forEach(cnec -> {
            CnecResultExtension cnecResultMap = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultMap.getVariant(resultVariantId);
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setThresholds(cnec);
        });
    }

    //this method is only used for pre optim result (to store all the rangeAction initial setPoints)
    private void fillPreOptimRangeActionResultsFromNetwork(Crac crac, String resultVariantId, Network network) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(network);
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(resultVariantId);
            rangeActionResult.setSetPoint(preventiveState, valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(preventiveState, ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }

    private void updateResultExtensions(Crac crac, double minMargin, String resultVariantId, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        updateCracExtension(crac, resultVariantId, minMargin);
        updateCnecExtensions(crac, resultVariantId, systematicSensitivityAnalysisResult);
        //The range action extensions are already updated by the solve method.
        //The network action extensions are not to be updated by the linear rao. They will be updated by the search tree rao if required.
    }

    private void updateCracExtension(Crac crac, String resultVariantId, double minMargin) {
        CracResultExtension cracResultMap = crac.getExtension(CracResultExtension.class);
        CracResult cracResult = cracResultMap.getVariant(resultVariantId);
        cracResult.setCost(-minMargin);
    }

    private RaoResult buildRaoResult(double minMargin, String preOptimVariantId, String postOptimVariantId) {
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOptimVariantId);
        raoResult.setPostOptimVariantId(postOptimVariantId);
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin >= 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
        return raoResult;
    }
}
