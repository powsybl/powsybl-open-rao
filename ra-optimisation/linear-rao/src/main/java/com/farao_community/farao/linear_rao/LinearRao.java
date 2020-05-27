/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.linear_rao.config.LinearRaoConfigurationUtil;
import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizerParameters;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputationParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimisationException;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_commons.RaoInput;
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
        RaoInput.cleanCrac(crac, network);
        RaoInput.synchronize(crac, network);
        RaoData raoData = new RaoData(network, crac);
        try {
            // check config
            linearRaoParametersQualityCheck(raoParameters, raoData);
            // run RAO algorithm
            return runLinearRao(raoData, raoParameters, computationManager);

        } catch (FaraoException e) {
            return CompletableFuture.completedFuture(buildFailedRaoResultAndClearVariants(raoData, e));
        }
    }

    CompletableFuture<RaoResult> runLinearRao(RaoData raoData,
                                              RaoParameters raoParameters,
                                              ComputationManager computationManager) {
        SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(
            raoParameters.getExtension(SystematicSensitivityComputationParameters.class),
            computationManager);

        raoData.fillRangeActionResultsWithNetworkValues();
        LOGGER.info("Initial systematic analysis [start]");
        systematicSensitivityComputation.run(raoData);
        LOGGER.info("Initial systematic analysis [end] - with initial min margin of {} MW", -raoData.getCracResult().getCost());

        // stop here if no optimisation should be done
        if (skipOptim(raoParameters, raoData.getCrac())) {
            return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(raoData, raoData.getInitialVariantId(), systematicSensitivityComputation));
        }

        String bestVariantId = IteratingLinearOptimizer.optimize(raoData, systematicSensitivityComputation, raoParameters);

        return CompletableFuture.completedFuture(buildSuccessfulRaoResultAndClearVariants(raoData, bestVariantId, systematicSensitivityComputation));
    }

    /**
     * Quality check of the configuration
     */
    private void linearRaoParametersQualityCheck(RaoParameters parameters, RaoData raoData) {
        if (parameters.isRaoWithLoopFlowLimitation() && Objects.isNull(raoData.getCrac().getExtension(CracLoopFlowExtension.class))) {
            throw new FaraoException("Loop flow parameters are inconsistent with CRAC loopflow extension");
        }
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }
    }

    /**
     * Method returning a boolean indicating whether an optimisation should be done,
     * or whether the LinearRao should only perform a security analysis
     */
    private boolean skipOptim(RaoParameters raoParameters, Crac crac) {
        return raoParameters.getExtension(LinearRaoParameters.class).isSecurityAnalysisWithoutRao()
            || raoParameters.getExtension(IteratingLinearOptimizerParameters.class).getMaxIterations() == 0
            || crac.getRangeActions().isEmpty();
    }

    /**
     * Build the RaoResult in case of optimisation success
     */
    private RaoResult buildSuccessfulRaoResultAndClearVariants(RaoData raoData, String postOptimVariantId, SystematicSensitivityComputation systematicSensitivityComputation) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(raoData.getInitialVariantId());
        raoResult.setPostOptimVariantId(postOptimVariantId);

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(systematicSensitivityComputation.isFallback());
        resultExtension.setLpStatus(LinearRaoResult.LpStatus.RUN_OK);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        // log
        double minMargin = -raoData.getCracResult(postOptimVariantId).getCost();
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin > 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);

        raoData.clearWithKeepingCracResults(Arrays.asList(raoData.getInitialVariantId(), postOptimVariantId));
        return raoResult;
    }

    /**
     * Build the RaoResult in case of optimisation failure
     */
    private RaoResult buildFailedRaoResultAndClearVariants(RaoData raoData, Exception e) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.FAILURE);
        raoResult.setPreOptimVariantId(raoData.getInitialVariantId());
        raoResult.setPostOptimVariantId(raoData.getWorkingVariantId());

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        if (e instanceof SensitivityComputationException) {
            resultExtension.setSystematicSensitivityAnalysisStatus(LinearRaoResult.SystematicSensitivityAnalysisStatus.FAILURE);
        } else if (e instanceof LinearOptimisationException) {
            resultExtension.setLpStatus(LinearRaoResult.LpStatus.FAILURE);
        }
        resultExtension.setErrorMessage(e.getMessage());
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        raoData.clearWithKeepingCracResults(Arrays.asList(raoData.getInitialVariantId(), raoData.getWorkingVariantId()));

        return raoResult;
    }
}
