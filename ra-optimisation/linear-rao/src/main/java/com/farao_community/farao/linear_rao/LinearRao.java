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
import com.farao_community.farao.linear_rao.optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SensitivityComputationException;
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

    static {
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

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
        CompletableFuture<RaoResult> result;
        try {
            result = run2(network, crac, variantId, computationManager, raoParameters);
        } catch (Exception e) {
            result = buildFailedCompletableRaoResult(e);
        }
        return result;
    }

    public CompletableFuture<RaoResult> run2(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters raoParameters) throws FaraoException, SensitivityComputationException {
        raoParametersQualityCheck(raoParameters);
        LinearRaoParameters linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);

        // initiate engines
        LinearOptimisationEngine linearOptimisationEngine = new LinearOptimisationEngine(raoParameters);
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(linearRaoParameters, computationManager);

        // evaluate initial sensitivity coefficients and costs on the initial network situation
        InitialSituation initialSituation = new InitialSituation(network, crac);
        systematicAnalysisEngine.run(initialSituation);

        // if ! doOptim() break
        if (skipOptim(linearRaoParameters, crac)) {
            return CompletableFuture.completedFuture(buildRaoResult(initialSituation.getCost(), initialSituation.getResultVariant(), initialSituation.getResultVariant(), systematicAnalysisEngine.isSensiFallback()));
        }

        AbstractSituation bestSituation = initialSituation;
        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {

            OptimizedSituation currentSituation = linearOptimisationEngine.run(bestSituation);

            if (bestSituation.sameRaResults(currentSituation)) {
                currentSituation.deleteResultVariant();
                break;
            }

            systematicAnalysisEngine.run(currentSituation);

            if (currentSituation.getCost() < bestSituation.getCost()) {
                bestSituation.deleteResultVariant();
                bestSituation = currentSituation;
            } else {
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} MW to {} MW", -bestSituation.getCost(), -currentSituation.getCost());
                break;
            }
        }

        return CompletableFuture.completedFuture(buildRaoResult(bestSituation.getCost(), initialSituation.getResultVariant(), bestSituation.getResultVariant(), systematicAnalysisEngine.isSensiFallback()));

    }

    private void raoParametersQualityCheck(RaoParameters parameters) {
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }
    }

    private boolean skipOptim(LinearRaoParameters linearRaoParameters, Crac crac) {
        return linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty();
    }

    private RaoResult buildRaoResult(double cost, String preOptimVariantId, String postOptimVariantId, boolean useFallbackSensiParams) {
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(useFallbackSensiParams);
        resultExtension.setLpStatus(LinearRaoResult.LpStatus.RUN_OK);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);
        raoResult.setPreOptimVariantId(preOptimVariantId);
        raoResult.setPostOptimVariantId(postOptimVariantId);
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) -cost, cost <= 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
        return raoResult;
    }

    private CompletableFuture<RaoResult> buildFailedCompletableRaoResult(Exception e) {
        RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
        LinearRaoResult resultExtension = new LinearRaoResult();
        if (e instanceof SensitivityComputationException) {
            resultExtension.setSystematicSensitivityAnalysisStatus(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE);
        } else if (e instanceof LinearOptimisationException) {
            resultExtension.setLpStatus(LinearRaoResult.LpStatus.FAILURE);
        }
        resultExtension.setErrorMessage(e.getMessage());
        raoResult.addExtension(LinearRaoResult.class, resultExtension);
        return CompletableFuture.completedFuture(raoResult);
    }
}
