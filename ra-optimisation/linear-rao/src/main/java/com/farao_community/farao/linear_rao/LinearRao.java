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
        network.getVariantManager().setWorkingVariant(variantId);
        Situation situation = new Situation(network, crac);
        String initialVariantId = situation.getWorkingVariantId();
        try {
            // check config
            raoParametersQualityCheck(raoParameters);

            // initiate engines
            LinearRaoParameters linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);
            LinearOptimisationEngine linearOptimisationEngine = new LinearOptimisationEngine(raoParameters);
            SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(linearRaoParameters, computationManager);

            // run RAO algorithm
            return runLinearRao(situation, systematicAnalysisEngine, linearOptimisationEngine, linearRaoParameters);

        } catch (FaraoException e) {
            return CompletableFuture.completedFuture(buildFailedRaoResultAndClearVariants(situation, initialVariantId, e));
        }
    }

    CompletableFuture<RaoResult> runLinearRao(Situation situation,
                                            SystematicAnalysisEngine systematicAnalysisEngine,
                                            LinearOptimisationEngine linearOptimisationEngine,
                                            LinearRaoParameters linearRaoParameters) {
        String initialVariantId = situation.getWorkingVariantId();
        systematicAnalysisEngine.run(situation);

        // stop here if no optimisation should be done
        if (skipOptim(linearRaoParameters, situation.getCrac())) {
            return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(situation, initialVariantId, initialVariantId, systematicAnalysisEngine));
        }

        String bestVariantId = initialVariantId;
        double bestCost = situation.getCost();
        String optimizedVariantId;
        double optimizedCost;

        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {
            optimizedVariantId = situation.cloneVariant(bestVariantId);
            situation.setWorkingVariant(optimizedVariantId);

            // Look for a new RangeAction combination, optimized with the LinearOptimisationEngine
            // Stores found solutions in crac extension working variant
            // Apply remedial actions on the network working variant
            linearOptimisationEngine.run(situation);

            // if the solution has not changed, stop the search
            if (situation.sameRemedialActions(bestVariantId, optimizedVariantId)) {
                break;
            }

            // evaluate sensitivity coefficients and cost on the newly optimised situation
            systematicAnalysisEngine.run(situation);

            optimizedCost = situation.getCost();
            if (optimizedCost < bestCost) { // if the solution has been improved, continue the search
                bestVariantId = optimizedVariantId;
                bestCost = optimizedCost;
            } else { // unexpected behaviour, stop the search
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} MW to {} MW", -bestCost, -optimizedCost);
                break;
            }
        }

        return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(situation, initialVariantId, bestVariantId, systematicAnalysisEngine));
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
    private RaoResult buildSuccessfulRaoResultAndClearVariants(Situation situation, String initialVariantId, String postOptimVariantId, SystematicAnalysisEngine systematicAnalysisEngine) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(initialVariantId);
        raoResult.setPostOptimVariantId(postOptimVariantId);

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(systematicAnalysisEngine.isFallback());
        resultExtension.setLpStatus(LinearRaoResult.LpStatus.RUN_OK);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        // log
        double minMargin = -situation.setWorkingVariant(postOptimVariantId).getCost();
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin > 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);

        situation.clear(Arrays.asList(initialVariantId, postOptimVariantId));
        return raoResult;
    }

    /**
     * Build the RaoResult in case of optimisation failure
     */
    private RaoResult buildFailedRaoResultAndClearVariants(Situation situation, String initialVariantId, Exception e) {

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

        situation.clear(Arrays.asList(initialVariantId, situation.getWorkingVariantId()));

        return raoResult;
    }
}
