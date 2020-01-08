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
        // sensi
        SystematicSensitivityAnalysisResult analysisResult = SystematicSensitivityAnalysisService.runAnalysis(network, crac, computationManager);
        if (analysisResult == null) {
            LinearRangeActionRaoResult resultExtension = new LinearRangeActionRaoResult(LinearRangeActionRaoResult.SecurityStatus.UNSECURED);
            RaoComputationResult raoComputationResult =  new RaoComputationResult(RaoComputationResult.Status.FAILURE);
            raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);
            return CompletableFuture.completedFuture(raoComputationResult);
        }

        LinearRangeActionRaoResult resultExtension = new LinearRangeActionRaoResult(LinearRangeActionRaoResult.SecurityStatus.SECURED);

        // 1. do for pre
        Map<String, Double> preReferenceFlow = analysisResult.getPreReferenceFlow();
        List<MonitoredBranchResult> monitoredBranchResults = buildMonitoredBranchResultListFromReferenceFlows(crac, preReferenceFlow, resultExtension, null);
        PreContingencyResult preRao = new PreContingencyResult(monitoredBranchResults);

        // 2. do for each contingency
        Map<Contingency, Map<String, Double> > mapReferenceFlowsMap = analysisResult.getContingencyReferenceFlowsMap();
        List<ContingencyResult> contingencyResultsRao = new ArrayList<>();
        for (Contingency contingency : mapReferenceFlowsMap.keySet()) {
            String idContSensi = contingency.getId();
            String nameContSensi = contingency.getName();

            Map<String, Double> currentReferenceFlowMap = mapReferenceFlowsMap.get(contingency);
            List<MonitoredBranchResult> tmpMonitoredBranchResults = buildMonitoredBranchResultListFromReferenceFlows(crac, currentReferenceFlowMap, resultExtension, contingency);
            ContingencyResult contingencyResult = new ContingencyResult(idContSensi, nameContSensi, tmpMonitoredBranchResults);

            contingencyResultsRao.add(contingencyResult);
        }

        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preRao, contingencyResultsRao);
        raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);

        // 4. return
        return CompletableFuture.completedFuture(raoComputationResult);
    }

    private List<MonitoredBranchResult> buildMonitoredBranchResultListFromReferenceFlows(Crac crac,
                                                                                         Map<String, Double> referenceFlowsMap,
                                                                                         LinearRangeActionRaoResult resultExtension,
                                                                                         Contingency contingency) {
        List<MonitoredBranchResult> resultList = new ArrayList<>();
        for (Cnec cnec : crac.getCnecs()) {
            Contingency currentCnecContingnecy = cnec.getState().getContingency().orElse(null);
            String cnecContingencyId = (currentCnecContingnecy == null) ? "" : currentCnecContingnecy.getId();
            if ((contingency == null && cnecContingencyId.equals("")) || //pre and cnec's state is null
                (contingency != null && cnecContingencyId.equals(contingency.getId())) // filter for contingency id
            ) {
                double referenceFlow = referenceFlowsMap.getOrDefault(cnec.getCriticalNetworkElement().getId(), 0.0);
                LOGGER.info("Reference flow for cnec {} is {}", cnec.getCriticalNetworkElement().getId(), referenceFlow);

                // secure or unsecured test
                Optional<Double> maximumFlow = Optional.empty();
                try {
                    maximumFlow = cnec.getThreshold().getMaxThreshold();
                    LOGGER.info("Maximum threshold for cnec : {} is {}", cnec.getCriticalNetworkElement().getId(), maximumFlow);
                } catch (SynchronizationException ignored) {
                    LOGGER.warn("Cannot get maximum threshold for cnec: {}", cnec.getCriticalNetworkElement().getId());
                }
                if (maximumFlow.orElse(Double.MAX_VALUE) < referenceFlow) {
                    //unsecured
                    LOGGER.info("cnec {} maximumFlow {} < referenceFlow {} => UNSECURED", cnec.getCriticalNetworkElement().getId(), maximumFlow, referenceFlow);
                    resultExtension.setSecurityStatus(LinearRangeActionRaoResult.SecurityStatus.UNSECURED);
                }
                resultList.add(new MonitoredBranchResult(cnec.getId(), cnec.getName(), cnec.getCriticalNetworkElement().getId(), maximumFlow.orElse(0.0), referenceFlow, Double.NaN));
            }
        }
        return resultList;
    }
}
