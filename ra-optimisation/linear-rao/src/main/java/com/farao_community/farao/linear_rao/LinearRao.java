/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowComputation;
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
        try {
            // check config
            raoParametersQualityCheck(raoParameters);

            // initiate engines
            LinearOptimisationEngine linearOptimisationEngine = new LinearOptimisationEngine(raoParameters);
            SystematicAnalysisEngine systematicAnalysisEngine = new SystematicAnalysisEngine(LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters), computationManager);

            // run RAO algorithm
            return runLinearRao(network, crac, variantId, systematicAnalysisEngine, linearOptimisationEngine, raoParameters);

        } catch (FaraoException e) {
            return CompletableFuture.completedFuture(buildFailedRaoResult(e));
        }
    }

    CompletableFuture<RaoResult> runLinearRao(Network network,
                                            Crac crac,
                                            String variantId,
                                            SystematicAnalysisEngine systematicAnalysisEngine,
                                            LinearOptimisationEngine linearOptimisationEngine,
                                            RaoParameters raoParameters) {

        LinearRaoParameters linearRaoParameters = LinearRaoConfigurationUtil.getLinearRaoParameters(raoParameters);

        // evaluate sensitivity coefficients and cost on the initial network situation
        InitialSituation initialSituation = new InitialSituation(network, variantId, crac);
        systematicAnalysisEngine.run(initialSituation);

        // compute Loopflow on initial situation
        if (useLoopFlowExtension(raoParameters)) {
            computeLoopflowOnCurrentSituation(initialSituation);
        }

        // stop here if no optimisation should be done
        if (skipOptim(linearRaoParameters, crac)) {
            return CompletableFuture.completedFuture(buildSuccessfulRaoResult(initialSituation, initialSituation, systematicAnalysisEngine));
        }

        // start optimisation loop
        AbstractSituation bestSituation = initialSituation;
        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {

            // look for a new RangeAction combination, optimized with the LinearOptimisationEngine
            OptimizedSituation optimizedSituation = linearOptimisationEngine.run(bestSituation);

            // if the solution has not changed, stop the search
            if (bestSituation.sameRaResults(optimizedSituation)) {
                optimizedSituation.deleteCracResultVariant();
                optimizedSituation.deleteNetworkVariant();
                break;
            }

            // evaluate sensitivity coefficients and cost on the newly optimised situation
            systematicAnalysisEngine.run(optimizedSituation);

            // update Loopflow
            if (useLoopFlowExtension(raoParameters)) {
                computeLoopflowOnCurrentSituation(optimizedSituation);
            }

            if (optimizedSituation.getCost() < bestSituation.getCost()) { // if the solution has been improved, continue the search
                if (!(bestSituation instanceof InitialSituation)) {
                    bestSituation.deleteCracResultVariant();
                    bestSituation.deleteNetworkVariant();
                }
                bestSituation = optimizedSituation;
            } else { // unexpected behaviour, stop the search
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} MW to {} MW", -bestSituation.getCost(), -optimizedSituation.getCost());
                break;
            }
        }

        return CompletableFuture.completedFuture(buildSuccessfulRaoResult(initialSituation, bestSituation, systematicAnalysisEngine));
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
    private RaoResult buildSuccessfulRaoResult(InitialSituation preOptimSituation, AbstractSituation postOptimSituation, SystematicAnalysisEngine systematicAnalysisEngine) {

        // build RaoResult
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOptimSituation.getCracResultVariant());
        raoResult.setPostOptimVariantId(postOptimSituation.getCracResultVariant());

        // build extension
        LinearRaoResult resultExtension = new LinearRaoResult();
        resultExtension.setSuccessfulSystematicSensitivityAnalysisStatus(systematicAnalysisEngine.isFallback());
        resultExtension.setLpStatus(LinearRaoResult.LpStatus.RUN_OK);
        raoResult.addExtension(LinearRaoResult.class, resultExtension);

        // remove network variants
        preOptimSituation.deleteNetworkVariant();
        if (!preOptimSituation.getNetworkVariantId().equals(postOptimSituation.getNetworkVariantId())) {
            postOptimSituation.deleteNetworkVariant();
        }

        // log
        double minMargin = -postOptimSituation.getCost();
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin > 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);

        return raoResult;
    }

    /**
     * Build the RaoResult in case of optimisation failure
     */
    private RaoResult buildFailedRaoResult(Exception e) {

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

    private void computeLoopflowOnCurrentSituation(AbstractSituation situation) {
        Crac crac = situation.getCrac();
        CracLoopFlowExtension cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
        // compute maximum loop flow value F_(0,all)_MAX, and update it for each Cnec in Crac
        if (!Objects.isNull(cracLoopFlowExtension)) {
            LoopFlowComputation loopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
            Map<String, Double> loopFlows = loopFlowComputation.calculateLoopFlows(situation.getNetwork());
            updateCnecsLoopFlowConstraint(crac, loopFlows);
        }
    }

    private void updateCnecsLoopFlowConstraint(Crac crac, Map<String, Double> fZeroAll) {
        // For each Cnec, get the maximum F_(0,all)_MAX = Math.max(F_(0,all)_init, loop flow threshold
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            if (!Objects.isNull(cnecLoopFlowExtension)) {
                //!!! note here we use the result of branch flow of preventive state for all cnec of all states
                //this could be ameliorated by re-calculating loopflow for each cnec in curative state: [network + cnec's contingencies + current applied remedial actions]
                double initialLoopFlow = fZeroAll.get(cnec.getNetworkElement().getId());
                double loopFlowThreshold = cnecLoopFlowExtension.getInputLoopFlow();
                cnecLoopFlowExtension.setLoopFlowConstraint(Math.max(initialLoopFlow, loopFlowThreshold)); //todo: cnec loop flow extension need to be based on ResultVariantManger
            }
        });
    }

    private static boolean useLoopFlowExtension(RaoParameters parameters) {
        return parameters.isRaoWithLoopFlowLimitation();
    }
}
