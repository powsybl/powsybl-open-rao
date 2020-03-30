/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
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
    private static final double MIN_CHANGE_THRESHOLD = 0.0001;

    private SystematicSensitivityAnalysisResult preOptimSensitivityAnalysisResult;
    private SystematicSensitivityAnalysisResult postOptimSensitivityAnalysisResult;
    private List<RemedialActionResult> oldRemedialActionResultList = new ArrayList<>();

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
                                                       RaoParameters parameters) {
        // quality check
        List<String> configQualityCheck = LinearRaoConfigurationUtil.checkLinearRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }

        LinearRaoParameters linearRaoParameters = parameters.getExtensionByName("LinearRaoParameters");
        SensitivityComputationParameters sensitivityComputationParameters = linearRaoParameters.getSensitivityComputationParameters();

        preOptimSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (preOptimSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || preOptimSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            return CompletableFuture.completedFuture(new RaoComputationResult(RaoComputationResult.Status.FAILURE));
        }
        postOptimSensitivityAnalysisResult = preOptimSensitivityAnalysisResult;
        double oldScore = getMinMargin(crac, preOptimSensitivityAnalysisResult);

        if (linearRaoParameters.isSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty()) {
            return CompletableFuture.completedFuture(buildRaoComputationResult(crac, oldScore));
        }

        SystematicSensitivityAnalysisResult tempSensitivityAnalysisResult;

        LinearRaoModeller linearRaoModeller = createLinearRaoModeller(crac, network, preOptimSensitivityAnalysisResult);
        linearRaoModeller.buildProblem();

        RaoComputationResult raoComputationResult;
        List<RemedialActionResult> newRemedialActionsResultList;

        for (int iteration = 1; iteration <= linearRaoParameters.getMaxIterations(); iteration++) {
            raoComputationResult = linearRaoModeller.solve();
            if (raoComputationResult.getStatus() == RaoComputationResult.Status.FAILURE) {
                return CompletableFuture.completedFuture(raoComputationResult);
            }

            newRemedialActionsResultList = raoComputationResult.getPreContingencyResult().getRemedialActionResults();
            //TODO: manage CRA
            if (sameRemedialActionResultLists(oldRemedialActionResultList, newRemedialActionsResultList)) {
                break;
            }

            applyRAs(crac, network, newRemedialActionsResultList);
            tempSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

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
            oldRemedialActionResultList = newRemedialActionsResultList;
            linearRaoModeller.updateProblem(network, tempSensitivityAnalysisResult);
        }
        RaoComputationResult linearRaoComputationResult = buildRaoComputationResult(crac, oldScore);
        preOptimSensitivityAnalysisResult = null;
        postOptimSensitivityAnalysisResult = null;
        oldRemedialActionResultList = new ArrayList<>();
        return CompletableFuture.completedFuture(linearRaoComputationResult);
    }

    //defined to be able to run unit tests
    LinearRaoModeller createLinearRaoModeller(Crac crac,
                                              Network network,
                                              SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        return new LinearRaoModeller(crac, network, systematicSensitivityAnalysisResult, new LinearRaoProblem());

    }

    private double getRemedialActionResultPostOptimisationValue(RemedialActionResult remedialActionResult) {
        RemedialActionElementResult remedialActionElementResult = remedialActionResult.getRemedialActionElementResults().get(0);
        if (remedialActionElementResult instanceof PstElementResult) {
            PstElementResult pstElementResult = (PstElementResult) remedialActionElementResult;
            return pstElementResult.getPostOptimisationAngle();
        } else if (remedialActionElementResult instanceof RedispatchElementResult) {
            RedispatchElementResult redispatchElementResult = (RedispatchElementResult) remedialActionElementResult;
            return redispatchElementResult.getPostOptimisationTargetP();
        }
        throw new FaraoException("Range action type of " + remedialActionElementResult.getId() + " is not supported yet");
    }

    private boolean sameRemedialActionResultLists(List<RemedialActionResult> firstList, List<RemedialActionResult> secondList) {
        if (firstList.size() != secondList.size()) {
            return false;
        }
        Map<String, Double> firstMap = new HashMap<>();
        for (RemedialActionResult remedialActionResult : firstList) {
            firstMap.put(remedialActionResult.getId(), getRemedialActionResultPostOptimisationValue(remedialActionResult));
        }
        for (RemedialActionResult remedialActionResult : secondList) {
            if (!firstMap.containsKey(remedialActionResult.getId()) ||
                    Math.abs(firstMap.get(remedialActionResult.getId()) - getRemedialActionResultPostOptimisationValue(remedialActionResult)) > MIN_CHANGE_THRESHOLD) {
                return false;
            }
        }
        return true;
    }

    private void applyRAs(Crac crac, Network network, List<RemedialActionResult> raResultList) {
        for (RemedialActionResult remedialActionResult : raResultList) {
            crac.getRangeAction(remedialActionResult.getId()).apply(network, getRemedialActionResultPostOptimisationValue(remedialActionResult));
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

    private RaoComputationResult buildRaoComputationResult(Crac crac, double minMargin) {
        LinearRaoResult resultExtension = new LinearRaoResult(
                minMargin >= 0 ? LinearRaoResult.SecurityStatus.SECURED : LinearRaoResult.SecurityStatus.UNSECURED);
        PreContingencyResult preContingencyResult = createPreContingencyResultAndUpdateLinearRaoResult(crac, resultExtension, oldRemedialActionResultList);
        List<ContingencyResult> contingencyResults = createContingencyResultsAndUpdateLinearRaoResult(crac, resultExtension);
        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preContingencyResult, contingencyResults);
        raoComputationResult.addExtension(LinearRaoResult.class, resultExtension);
        LOGGER.info("LinearRaoResult: mininum margin = {}, security status: {}", (int) resultExtension.getMinMargin(), resultExtension.getSecurityStatus());
        return raoComputationResult;
    }

    private List<ContingencyResult> createContingencyResultsAndUpdateLinearRaoResult(Crac crac, LinearRaoResult linearRaoResult) {
        List<ContingencyResult> contingencyResults = new ArrayList<>();
        crac.getContingencies().forEach(contingency -> {
            List<MonitoredBranchResult> contingencyMonitoredBranches = new ArrayList<>();
            crac.getStates(contingency).forEach(state -> crac.getCnecs(state).forEach(cnec ->
                    contingencyMonitoredBranches.add(createMonitoredBranchResultAndUpdateLinearRaoResult(cnec, linearRaoResult))));
            contingencyResults.add(new ContingencyResult(contingency.getId(), contingency.getName(), contingencyMonitoredBranches));
        });
        return contingencyResults;
    }

    private PreContingencyResult createPreContingencyResultAndUpdateLinearRaoResult(Crac crac, LinearRaoResult linearRaoResult, List<RemedialActionResult> raResultList) {
        List<MonitoredBranchResult> preContingencyMonitoredBranches = new ArrayList<>();
        if (crac.getPreventiveState() != null) {
            crac.getCnecs(crac.getPreventiveState()).forEach(cnec ->
                preContingencyMonitoredBranches.add(createMonitoredBranchResultAndUpdateLinearRaoResult(cnec, linearRaoResult)
                ));
        }
        return new PreContingencyResult(preContingencyMonitoredBranches, raResultList);
    }

    private MonitoredBranchResult createMonitoredBranchResultAndUpdateLinearRaoResult(Cnec cnec, LinearRaoResult linearRaoResult) {
        double preOptimFlow = preOptimSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
        double postOptimFlow = postOptimSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);

        if (Double.isNaN(preOptimFlow) || Double.isNaN(postOptimFlow)) {
            throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
        }

        double marginPostOptim =  cnec.computeMargin(postOptimFlow, Unit.MEGAWATT);
        double absoluteThreshold = cnec.getMaxThreshold(Unit.MEGAWATT).orElse(-cnec.getMinThreshold(Unit.MEGAWATT).orElseThrow(FaraoException::new));
        linearRaoResult.updateResult(marginPostOptim);

        return new MonitoredBranchResult(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId(), absoluteThreshold, preOptimFlow, postOptimFlow);
    }
}
