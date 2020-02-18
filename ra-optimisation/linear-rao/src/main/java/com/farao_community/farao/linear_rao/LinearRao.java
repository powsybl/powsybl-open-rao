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
import com.farao_community.farao.data.crac_api.SynchronizationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private Map<String, RemedialActionResult> remedialActionResultHistoryMap;

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
        int iterationsLeft = linearRaoParameters.getMaxIterations();

        // setReferenceValue (only once!!)
        crac.setReferenceValues(network);
        preOptimSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
        postOptimSensitivityAnalysisResult = preOptimSensitivityAnalysisResult;
        crac.synchronize(network);
        double oldScore = getMinMargin(crac, preOptimSensitivityAnalysisResult);

        if (linearRaoParameters.getSecurityAnalysisWithoutRao() || linearRaoParameters.getMaxIterations() == 0 || crac.getRangeActions().isEmpty()) {
            return CompletableFuture.completedFuture(buildRaoComputationResult(crac, oldScore));
        }

        SystematicSensitivityAnalysisResult tempSensitivityAnalysisResult;

        String originalNetworkVariant = network.getVariantManager().getWorkingVariantId();
        createAndSwitchToNewVariant(network, originalNetworkVariant);

        LinearRaoModeller linearRaoModeller = createLinearRaoModeller(crac, network, preOptimSensitivityAnalysisResult);
        linearRaoModeller.buildProblem();

        RaoComputationResult raoComputationResult;
        List<RemedialActionResult> newRemedialActionsResult;
        remedialActionResultHistoryMap = new HashMap<>();

        while (iterationsLeft > 0) {
            raoComputationResult = linearRaoModeller.solve();
            if (raoComputationResult.getStatus() == RaoComputationResult.Status.FAILURE) {
                return CompletableFuture.completedFuture(raoComputationResult);
            }

            newRemedialActionsResult = raoComputationResult.getPreContingencyResult().getRemedialActionResults();
            //TODO: manage CRA
            if (newRemedialActionsResult.isEmpty()) {
                break;
            }

            updateRAResultHistoryList(newRemedialActionsResult);
            applyRAs(crac, network, newRemedialActionsResult);
            tempSensitivityAnalysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
            crac.synchronize(network);
            double newScore = getMinMargin(crac, tempSensitivityAnalysisResult);
            if (newScore < oldScore) {
                // TODO : limit the ranges
                LOGGER.warn("Linear Optimization found a worse result after an iteration: from {} to {}", oldScore, newScore);
                break;
            }

            postOptimSensitivityAnalysisResult = tempSensitivityAnalysisResult;
            oldScore = newScore;
            linearRaoModeller.updateProblem(network, tempSensitivityAnalysisResult, newRemedialActionsResult.stream().map(RemedialActionResult::getId).collect(Collectors.toList()));
            iterationsLeft -= 1;
        }
        crac.synchronize(network);
        return CompletableFuture.completedFuture(buildRaoComputationResult(crac, oldScore));
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

    private RemedialActionResult combineRAResults(RemedialActionResult oldRemedialActionResult, RemedialActionResult newRemedialActionResult) {
        //mapping the old RA elements to their IDs
        Map<String, RemedialActionElementResult> oldRAResultElementMap = oldRemedialActionResult.getRemedialActionElementResults().stream()
                .collect(Collectors.toMap(RemedialActionElementResult::getId, remedialActionElementResult -> remedialActionElementResult));
        //initializing an empty RA element list which will contain the combination of the elements from both RAs
        List<RemedialActionElementResult> combinedRAResultElementList = new ArrayList<>();
        /*
        For each RA element of the second RA, we create a new RA element combining that RA element and the corresponding
          one from the first RA (as long as they dont cancel each other out)
        */
        for (RemedialActionElementResult remedialActionElementResult : newRemedialActionResult.getRemedialActionElementResults()) {
            if (remedialActionElementResult instanceof PstElementResult) {
                PstElementResult newPstElementResult = (PstElementResult) remedialActionElementResult;
                PstElementResult oldPstElementResult = (PstElementResult) oldRAResultElementMap.get(remedialActionElementResult.getId());
                double totalChange = newPstElementResult.getPostOptimisationAngle() - oldPstElementResult.getPreOptimisationAngle();
                if (Math.abs(totalChange) > MIN_CHANGE_THRESHOLD) {
                    combinedRAResultElementList.add(new PstElementResult(oldPstElementResult.getId(),
                            oldPstElementResult.getPreOptimisationAngle(),
                            oldPstElementResult.getPreOptimisationTapPosition(),
                            newPstElementResult.getPostOptimisationAngle(),
                            newPstElementResult.getPostOptimisationTapPosition()));
                }
            } else if (remedialActionElementResult instanceof RedispatchElementResult) {
                RedispatchElementResult newRedispatchElementResult = (RedispatchElementResult) remedialActionElementResult;
                RedispatchElementResult oldRedispatchElementResult = (RedispatchElementResult) oldRAResultElementMap.get(remedialActionElementResult.getId());
                double totalChange = newRedispatchElementResult.getPostOptimisationTargetP() - oldRedispatchElementResult.getPreOptimisationTargetP();
                if (Math.abs(totalChange) > MIN_CHANGE_THRESHOLD) {
                    combinedRAResultElementList.add(new RedispatchElementResult(oldRedispatchElementResult.getId(),
                            oldRedispatchElementResult.getPreOptimisationTargetP(),
                            newRedispatchElementResult.getPostOptimisationTargetP(),
                            oldRedispatchElementResult.getRedispatchCost()));
                }
            }
        }
        return new RemedialActionResult(oldRemedialActionResult.getId(), oldRemedialActionResult.getName(), oldRemedialActionResult.isApplied(), combinedRAResultElementList);
    }

    private void updateRAResultHistoryList(List<RemedialActionResult> newRemedialActionResultList) {
        /*
        If an element is in the new list but not the history, we add it to the history.
        If it is in both lists (they will be set to different values) we combine them using the combineRAResults method.
          If they end up canceling each other out (for instance the old RA could set a tap from 4 to 8 and the new RA
          could set the same tap from 8 to 4), the combined RA will be empty and we remove it from the history.
        */
        for (RemedialActionResult remedialActionResult : newRemedialActionResultList) {
            if (!remedialActionResultHistoryMap.containsKey(remedialActionResult.getId())) {
                remedialActionResultHistoryMap.put(remedialActionResult.getId(), remedialActionResult);
            } else {
                RemedialActionResult combinedRemedialActionResult = combineRAResults(remedialActionResultHistoryMap.get(remedialActionResult.getId()), remedialActionResult);
                if (combinedRemedialActionResult.getRemedialActionElementResults().isEmpty()) {
                    remedialActionResultHistoryMap.remove(combinedRemedialActionResult.getId());
                } else {
                    remedialActionResultHistoryMap.put(combinedRemedialActionResult.getId(), combinedRemedialActionResult);
                }
            }
        }
    }

    private void createAndSwitchToNewVariant(Network network, String referenceNetworkVariant) {
        Objects.requireNonNull(referenceNetworkVariant);
        String uniqueId = getUniqueVariantId(network);
        network.getVariantManager().cloneVariant(referenceNetworkVariant, uniqueId);
        network.getVariantManager().setWorkingVariant(uniqueId);
    }

    private String getUniqueVariantId(Network network) {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    private void applyRAs(Crac crac, Network network, List<RemedialActionResult> raResultList) {
        for (RemedialActionResult remedialActionResult : raResultList) {
            crac.getRangeAction(remedialActionResult.getId()).apply(network, getRemedialActionResultPostOptimisationValue(remedialActionResult));
        }

    }

    private double getMinMargin(Crac crac, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        double minMargin = Double.POSITIVE_INFINITY;
        try {
            for (Cnec cnec : crac.getCnecs()) {
                double margin;
                double flow = systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
                double margin1 = cnec.getThreshold().getMaxThreshold(Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY) - flow;
                double margin2 = flow - cnec.getThreshold().getMinThreshold(Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
                margin = Math.min(margin1, margin2);
                if (Double.isNaN(margin)) {
                    throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
                }
                minMargin = Math.min(minMargin, margin);
            }
        } catch (SynchronizationException e) {
            throw new FaraoException(e);
        }
        return minMargin;
    }

    private RaoComputationResult buildRaoComputationResult(Crac crac, double minMargin) {
        LinearRaoResult resultExtension = new LinearRaoResult(
                minMargin >= 0 ? LinearRaoResult.SecurityStatus.SECURED : LinearRaoResult.SecurityStatus.UNSECURED);
        PreContingencyResult preContingencyResult = createPreContingencyResultAndUpdateLinearRaoResult(crac, resultExtension, new ArrayList<>(remedialActionResultHistoryMap.values()));
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

        double limitingThreshold;
        try {
            double margin1 = cnec.getThreshold().getMaxThreshold(Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY) - postOptimFlow;
            double margin2 = postOptimFlow - cnec.getThreshold().getMinThreshold(Unit.MEGAWATT).orElse(Double.POSITIVE_INFINITY);
            double marginPostOptim =  Math.min(margin1, margin2);
            limitingThreshold = cnec.getThreshold().getMaxThreshold(Unit.MEGAWATT).orElse(-cnec.getThreshold().getMinThreshold(Unit.MEGAWATT).orElseThrow(FaraoException::new));
            linearRaoResult.updateResult(marginPostOptim);
        } catch (SynchronizationException e) {
            throw new FaraoException(e);
        }
        return new MonitoredBranchResult(cnec.getId(), cnec.getName(), cnec.getNetworkElement().getId(), limitingThreshold, preOptimFlow, postOptimFlow);
    }
}
