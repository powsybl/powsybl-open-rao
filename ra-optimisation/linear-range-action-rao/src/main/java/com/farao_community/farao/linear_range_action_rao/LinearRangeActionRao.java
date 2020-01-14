/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;

import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.MonitoredBranchResult;
import com.farao_community.farao.ra_optimisation.PreContingencyResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
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

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRangeActionRao implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearRangeActionRao.class);

    @Override
    public String getName() {
        return "Linear Range Action Rao";
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
        crac.synchronize(network);
        // sensi
        SystematicSensitivityAnalysisResult analysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);

        LinearRangeActionRaoResult resultExtension = new LinearRangeActionRaoResult(LinearRangeActionRaoResult.SecurityStatus.SECURED);

        // 1. do for pre
        LOGGER.info("Building result for precontingency:");
        Map<String, Double> preMargin = analysisResult.getPreReferenceMargin();
        List<MonitoredBranchResult> monitoredBranchResults = buildMonitoredBranchResultAndResultExtension(crac, preMargin, resultExtension, null);
        PreContingencyResult preRao = new PreContingencyResult(monitoredBranchResults);

        // 2. do for each contingency
        Map<Contingency, Map<String, Double> > mapMarginsMap = analysisResult.getContingencyReferenceMarginsMap();
        List<ContingencyResult> contingencyResultsRao = new ArrayList<>();
        for (Map.Entry<Contingency, Map<String, Double> > entry : mapMarginsMap.entrySet()) {
            Contingency contingency = entry.getKey();
            Map<String, Double> currentMarginMap = mapMarginsMap.get(contingency);

            String idContSensi = contingency.getId();
            String nameContSensi = contingency.getName();
            LOGGER.info("Building result for post-contingency {}:", idContSensi);

            List<MonitoredBranchResult> tmpMonitoredBranchResults = buildMonitoredBranchResultAndResultExtension(crac, currentMarginMap, resultExtension, contingency);
            ContingencyResult contingencyResult = new ContingencyResult(idContSensi, nameContSensi, tmpMonitoredBranchResults);

            contingencyResultsRao.add(contingencyResult);
        }

        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preRao, contingencyResultsRao);
        raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);
        LOGGER.info("LinearRangeActionRaoResult extension: mininum margin = {}, security status: {}", resultExtension.getMinMargin(), resultExtension.getSecurityStatus());
        // 4. return
        return CompletableFuture.completedFuture(raoComputationResult);
    }

    private List<MonitoredBranchResult> buildMonitoredBranchResultAndResultExtension(Crac crac,
                                                                                     Map<String, Double> marginsMap,
                                                                                     LinearRangeActionRaoResult resultExtension,
                                                                                     Contingency contingency) {
        List<MonitoredBranchResult> resultList = new ArrayList<>();
        for (Cnec cnec : crac.getCnecs()) {
            Contingency currentCnecContingency = cnec.getState().getContingency().orElse(null);
            String cnecContingencyId = (currentCnecContingency == null) ? "" : currentCnecContingency.getId();
            if ((contingency == null && cnecContingencyId.equals("")) || //pre and cnec's state is null
                (contingency != null && cnecContingencyId.equals(contingency.getId())) // filter for contingency id
            ) {
                double margin = marginsMap.getOrDefault(cnec.getCriticalNetworkElement().getId(), 0.0);
                LOGGER.info("Reference margin for cnec {} of contingency {} is {}", cnec.getId(), cnecContingencyId, margin);

                resultExtension.updateResult(margin); // update mininum margin and security status in LinearRangeActionRaoResult

                double maximumFlow = 0;
                try {
                    maximumFlow = cnec.getThreshold().getMaxThreshold().orElse(0.0);
                } catch (SynchronizationException e) {
                    LOGGER.error("Cannot get max threshold for cnec {}", cnec.getId());
                }
                double referenceFlow = maximumFlow - margin;
                resultList.add(new MonitoredBranchResult(cnec.getId(), cnec.getName(), cnec.getCriticalNetworkElement().getId(), maximumFlow, referenceFlow, Double.NaN));
            }
        }
        return resultList;
    }
}
