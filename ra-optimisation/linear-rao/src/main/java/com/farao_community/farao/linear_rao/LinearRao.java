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
import com.powsybl.sensitivity.SensitivityComputationParameters;
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
        try {
            return runLinearRao(network, crac, variantId, computationManager, raoParameters);
        } catch (FaraoException e) {
            return CompletableFuture.completedFuture(buildFailedCompletableRaoResult(e));
        }
    }

    private CompletableFuture<RaoResult> runLinearRao(Network network,
                                            Crac crac,
                                            String variantId,
                                            ComputationManager computationManager,
                                            RaoParameters raoParameters) throws FaraoException {
        // check config
        raoParametersQualityCheck(raoParameters);
        LinearRaoParameters linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);

        // initiate engines
        LinearOptimisationEngine linearOptimisationEngine = new LinearOptimisationEngine(raoParameters);
        SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(linearRaoParameters, computationManager);

        // evaluate initial sensitivity coefficients and costs on the initial network situation
        InitialSituation initialSituation = new InitialSituation(network, variantId, crac);
        systematicAnalysisEngine.run(initialSituation);

        // stop here if no optimisation should be done
        if (skipOptim(linearRaoParameters, crac)) {
            return CompletableFuture.completedFuture(buildSucessfullRaoResult(initialSituation, initialSituation, systematicAnalysisEngine));
        }

        // start optimisation loop
        AbstractSituation bestSituation = initialSituation;
        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {

            OptimizedSituation optimizedSituation = linearOptimisationEngine.run(bestSituation);

            if (bestSituation.sameRaResults(optimizedSituation)) { // the solution cannot be improved, stop the search
                optimizedSituation.deleteResultVariant();
                break;
            }

            systematicAnalysisEngine.run(optimizedSituation);

            if (optimizedSituation.getCost() < bestSituation.getCost()) { // the solution has been improved, continue the search
                bestSituation.deleteResultVariant();
                bestSituation = optimizedSituation;
            } else { // unexpected behaviour, stop the search
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} MW to {} MW", -bestSituation.getCost(), -optimizedSituation.getCost());
                break;
            }
        }

        return CompletableFuture.completedFuture(buildSucessfullRaoResult(initialSituation, bestSituation, systematicAnalysisEngine));
    }

    /**
     * Quality check of the configuration
     */
    private void raoParametersQualityCheck(RaoParameters parameters) {
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }
    }

    /**
     * Method returning a boolean indicating whether an optimisation should be done,
     * or whether the LinearRao should only perform a security analysis
     */
    private boolean skipOptim(LinearRaoParameters linearRaoParameters, Crac crac) {
        return linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty();
    }

    /**
     * Build the RaoResult in case of optimisation success
     */
    private RaoResult buildSucessfullRaoResult(InitialSituation preOptimSituation, AbstractSituation postOptimVariantId, SystematicAnalysisEngine systematicAnalysisEngine) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOptimSituation.getResultVariant());
        raoResult.setPostOptimVariantId(postOptimVariantId.getResultVariant());

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(systematicAnalysisEngine.isFallback());
        resultExtension.setLpStatus(LinearRaoResult.LpStatus.RUN_OK);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        // log
        double minMargin = -postOptimVariantId.getCost();
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin > 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);

        return raoResult;
    }

    /**
     * Build the RaoResult in case of optimisation failure
     */
    private RaoResult buildFailedCompletableRaoResult(Exception e) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        if (e instanceof SensitivityComputationException) {
            resultExtension.setSystematicSensitivityAnalysisStatus(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE);
        } else if (e instanceof LinearOptimisationException) {
            resultExtension.setLpStatus(LinearRaoResult.LpStatus.FAILURE);
        }
        resultExtension.setErrorMessage(e.getMessage());
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        return raoResult;
    }
}
