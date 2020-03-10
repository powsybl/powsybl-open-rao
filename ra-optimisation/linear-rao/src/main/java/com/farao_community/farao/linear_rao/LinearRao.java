/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.linear_rao.config.LinearRaoConfigurationUtil;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.ra_optimisation.*;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
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

    private SystematicSensitivityAnalysisResult postOptimSensitivityAnalysisResult;

    @Override
    public String getName() {
        return "LinearRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoComputationResult> run(Network network,
                                                       Crac crac,
                                                       String variantId,
                                                       ComputationManager computationManager,
                                                       RaoParameters parameters,
                                                       String resultVariantId) {
        // quality check
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }

        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }
        if (!resultVariantManager.getVariants().contains(resultVariantId)) {
            resultVariantManager.createVariant(resultVariantId);
        }
        String bestResultVariant = resultVariantManager.createNewUniqueVariant();
        String currentResultVariant = resultVariantManager.createNewUniqueVariant();

        SystematicSensitivityAnalysisResult tempSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
        // Failure if some sensitivities are not computed
        if (tempSensitivityAnalysisResult.getStateSensiMap().containsValue(null)) {
            return CompletableFuture.completedFuture(new RaoComputationResult(RaoComputationResult.Status.FAILURE));
        }
        postOptimSensitivityAnalysisResult = tempSensitivityAnalysisResult;
        double oldScore = getMinMargin(crac, tempSensitivityAnalysisResult);

        LinearRaoParameters linearRaoParameters = parameters.getExtensionByName("LinearRaoParameters");
        if (linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty()) {
            return CompletableFuture.completedFuture(buildRaoComputationResult(crac, oldScore, resultVariantId, resultVariantId));
        }

        LinearRaoModeller linearRaoModeller = createLinearRaoModeller(crac, network, tempSensitivityAnalysisResult);
        linearRaoModeller.buildProblem();
        RaoComputationResult raoComputationResult;

        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {
            raoComputationResult = linearRaoModeller.solve(currentResultVariant);
            if (raoComputationResult.getStatus() == RaoComputationResult.Status.FAILURE) {
                return CompletableFuture.completedFuture(raoComputationResult);
            }

            if (sameRemedialActions(crac, bestResultVariant, currentResultVariant)) {
                break;
            }

            applyRAs(crac, network, currentResultVariant);
            tempSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);

            // If some sensitivities are not computed, the bes result found so far is returned
            if (tempSensitivityAnalysisResult.getStateSensiMap().containsValue(null)) {
                break;
            }
            double newScore = getMinMargin(crac, tempSensitivityAnalysisResult);
            if (newScore < oldScore) {
                // TODO : limit the ranges
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} to {}", oldScore, newScore);
                break;
            }

            postOptimSensitivityAnalysisResult = tempSensitivityAnalysisResult;
            oldScore = newScore;
            resultVariantManager.deleteVariant(bestResultVariant);
            bestResultVariant = currentResultVariant;
            linearRaoModeller.updateProblem(network, tempSensitivityAnalysisResult);
        }
        RaoComputationResult linearRaoComputationResult = buildRaoComputationResult(crac, oldScore, resultVariantId, bestResultVariant);
        postOptimSensitivityAnalysisResult = null;
        return CompletableFuture.completedFuture(linearRaoComputationResult);
    }

    //defined to be able to run unit tests
    LinearRaoModeller createLinearRaoModeller(Crac crac,
                                              Network network,
                                              SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        return new LinearRaoModeller(crac, network, systematicSensitivityAnalysisResult, new LinearRaoProblem());

    }

    private boolean sameRemedialActions(Crac crac, String resultVariant1, String resultVariant2) {
        //TODO: manage curative RA
        State preventiveState = crac.getPreventiveState();
        crac.getRangeActions().forEach(rangeAction -> {
            RangeActionResult<?> rangeActionResult = (RangeActionResult<?>) rangeAction.getExtension(RangeActionResult.class);
            if (rangeActionResult.getSetPoint(preventiveState, resultVariant1) != rangeActionResult.getSetPoint(preventiveState, resultVariant2)) {
                return false;
            }
        });
        return true;
    }

    private void applyRAs(Crac crac, Network network, String variantId) {
        State preventiveState = crac.getPreventiveState();
        crac.getRangeActions().forEach(rangeAction -> {
            double setPoint = ((RangeActionResult<?>) rangeAction.getExtension(RangeActionResult.class)).getSetPoint(preventiveState);
            rangeAction.apply(network, setPoint);
        });
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

    private void updateRangeActionExtensions(Crac crac, String resultVariantId, String bestResultVariant) {
        for (RangeAction<?> rangeAction : crac.getRangeActions()) {
            RangeActionResultsExtension rarExtension = rangeAction.getExtension(RangeActionResultsExtension.class);
            rarExtension.get(resultVariantId).setSetPoint(rarExtension.get(bestResultVariant.getSetPoint()));
        }
    }

    private void updateCnecExtensions(Crac crac, String resultVariantId) {
        crac.getCnecs().forEach(cnec -> {
            cnec.getExtension(CnecResult.class).setFlowInMW(resultVariantId, postOptimSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnec.getExtension(CnecResult.class).setFlowInA(resultVariantId, postOptimSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
        });
    }

    private void updateResultExtensions(Crac crac, double minMargin, String resultVariantId, String bestResultVariant) {
        crac.getExtension(CracResultsExtension.class).get(resultVariantId).setCost(minMargin);

        updateRangeActionExtensions(crac, resultVariantId, bestResultVariant);
        updateCnecExtensions(crac, resultVariantId);
    }

    private RaoComputationResult buildRaoComputationResult(Crac crac, double minMargin, String resultVariantId, String bestResultVariant) {
        LinearRaoResult resultExtension = new LinearRaoResult(minMargin >= 0 ? LinearRaoResult.SecurityStatus.SECURED : LinearRaoResult.SecurityStatus.UNSECURED);
        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        raoComputationResult.addExtension(LinearRaoResult.class, resultExtension);
        LOGGER.info("LinearRaoResult: mininum margin = {}, security status: {}", (int) resultExtension.getMinMargin(), resultExtension.getSecurityStatus());

        updateResultExtensions(crac, minMargin, resultVariantId, bestResultVariant);

        return raoComputationResult;

    }
}
