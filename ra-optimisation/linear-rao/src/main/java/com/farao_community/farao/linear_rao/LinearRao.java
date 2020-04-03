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
import com.farao_community.farao.linear_rao.engines.LinearOptimisationEngine;
import com.farao_community.farao.linear_rao.engines.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    @Override
    public CompletableFuture<RaoResult> run(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters raoParameters) {

        raoParametersQualityCheck(raoParameters);
        LinearRaoParameters linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);

        // evaluate initial sensitivity coefficients and costs on the initial network situation
        InitialSituation initialSituation = new InitialSituation(crac);
        initialSituation.evaluateSensiAndCost(network, computationManager, linearRaoParameters.getSensitivityComputationParameters());
        if (initialSituation.getSensiStatus() != AbstractSituation.ComputationStatus.RUN_OK) {
            initialSituation.deleteResultVariant();
            return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
        }

        // if ! doOptim() break
        if (skipOptim(linearRaoParameters, crac)) {
            return CompletableFuture.completedFuture(buildRaoResult(initialSituation.getCost(), initialSituation.getResultVariant(), initialSituation.getResultVariant()));
        }

        // initiate LP
        LinearOptimisationEngine linearOptimisationEngine = createLinearRaoModeller(crac, network, initialSituation.getSystematicSensitivityAnalysisResult(), raoParameters);
        linearOptimisationEngine.buildProblem();

        AbstractSituation bestSituation = initialSituation;
        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {
            OptimizedSituation currentSituation = new OptimizedSituation(crac);

            currentSituation.solveLp(linearOptimisationEngine);
            if (currentSituation.getLpStatus() != AbstractSituation.ComputationStatus.RUN_OK) {
                currentSituation.deleteResultVariant();
                return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
            }

            if (bestSituation.sameRaResults(currentSituation)) {
                currentSituation.deleteResultVariant();
                break;
            }

            currentSituation.applyRAs(network);
            currentSituation.evaluateSensiAndCost(network, computationManager, linearRaoParameters.getSensitivityComputationParameters());

            if (currentSituation.getCost() >= bestSituation.getCost()) {
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} MW to {} MW", -bestSituation.getCost(), -currentSituation.getCost());
                break;
            }

            bestSituation.deleteResultVariant();
            bestSituation = currentSituation;
            linearOptimisationEngine.updateProblem(network, currentSituation.getSystematicSensitivityAnalysisResult());

        }

        return CompletableFuture.completedFuture(buildRaoResult(bestSituation.getCost(), initialSituation.getResultVariant(), bestSituation.getResultVariant()));

    }

    private void raoParametersQualityCheck(RaoParameters parameters) {
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }
    }

    LinearOptimisationEngine createLinearRaoModeller(Crac crac,
                                                     Network network,
                                                     SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult,
                                                     RaoParameters raoParameters) {
        return new LinearOptimisationEngine(crac, network, systematicSensitivityAnalysisResult, new LinearRaoProblem(), raoParameters);
    }

    private boolean skipOptim(LinearRaoParameters linearRaoParameters, Crac crac) {
        return linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty();
    }

    private RaoResult buildRaoResult(double cost, String preOptimVariantId, String postOptimVariantId) {
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOptimVariantId);
        raoResult.setPostOptimVariantId(postOptimVariantId);
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) -cost, cost <= 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
        return raoResult;
    }
}
