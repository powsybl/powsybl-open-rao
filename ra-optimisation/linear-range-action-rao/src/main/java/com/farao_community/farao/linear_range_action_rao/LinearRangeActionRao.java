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
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;
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
    public CompletableFuture<RaoComputationResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        return null;
    }

    public CompletableFuture<RaoComputationResult> run(Network network, Crac crac, String variantId,
                                                       ComputationManager computationManager, RaoParameters parameters,
                                                       SensitivityComputationFactory sensitivityComputationFactory) {
        SystematicSensitivityAnalysisResult sensiSaResults = SystematicSensitivityAnalysisService.runSensitivity(network, crac, computationManager, sensitivityComputationFactory);
        if (sensiSaResults == null) {
            LinearRangeActionRaoResult resultExtension = new LinearRangeActionRaoResult(LinearRangeActionRaoResult.SecurityStatus.UNSECURED);
            RaoComputationResult raoComputationResult =  new RaoComputationResult(RaoComputationResult.Status.FAILURE);
            raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);
            return CompletableFuture.completedFuture(raoComputationResult);
        }

        LinearRangeActionRaoResult resultExtension = new LinearRangeActionRaoResult(LinearRangeActionRaoResult.SecurityStatus.SECURED);

        // 1. do for pre
        SensitivityComputationResults preSensi = sensiSaResults.getPrecontingencyResult();
        List<MonitoredBranchResult> monitoredBranchResults = getMonitoredBranchResultList(crac, preSensi, resultExtension);
        PreContingencyResult preRao = new PreContingencyResult(monitoredBranchResults);

        // 2. do for each contingency
        Map<Contingency, SensitivityComputationResults>  mapSensi = sensiSaResults.getResultMap();
        List<ContingencyResult> contingencyResultsRao = new ArrayList<>();
        for (Contingency contingency : mapSensi.keySet()) {
            String idContSensi = contingency.getId();
            String nameContSensi = contingency.getName();

            SensitivityComputationResults sensitivityComputationResults = mapSensi.get(contingency);
            List<MonitoredBranchResult> tmpMonitoredBranchResults = getMonitoredBranchResultList(crac, sensitivityComputationResults, resultExtension);
            ContingencyResult contingencyResult = new ContingencyResult(idContSensi, nameContSensi, tmpMonitoredBranchResults);

            contingencyResultsRao.add(contingencyResult);
        }

        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preRao, contingencyResultsRao);
        raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);

        // 4. return
        return CompletableFuture.completedFuture(raoComputationResult);
    }

    private List<MonitoredBranchResult> getMonitoredBranchResultList(Crac crac, SensitivityComputationResults results, LinearRangeActionRaoResult resultExtension) {
        List<MonitoredBranchResult> returnlist = new ArrayList<>();
        crac.getCnecs().forEach(cnec -> {
            results.getSensitivityValues().forEach(sensitivityValue -> {
                if (sensitivityValue.getFactor().getFunction().getId() == cnec.getId()) { //id filter: get sensiValue result for current cnec
                    MonitoredBranchResult monitoredBranchResult = getMonitoredBranchResult(cnec, sensitivityValue, resultExtension);
                    returnlist.add(monitoredBranchResult);
                }
            });
        });
        return returnlist;
    }

    private MonitoredBranchResult getMonitoredBranchResult(Cnec cnec, SensitivityValue sensitivityValue, LinearRangeActionRaoResult resultExtension) {
        String id = sensitivityValue.getFactor().getFunction().getId();
        String name = sensitivityValue.getFactor().getFunction().getName();
        Optional<Double> maximumFlow = Optional.empty();
        try {
            maximumFlow = cnec.getThreshold().getMaxThreshold();
        } catch (SynchronizationException ignored) {
        }
        double preOptimisationFlow = sensitivityValue.getFunctionReference(); //todo: make a separate map for reference flow
        if (maximumFlow.orElse(Double.MIN_VALUE) < preOptimisationFlow) {
            //unsecured
            resultExtension.setSecurityStatus(LinearRangeActionRaoResult.SecurityStatus.UNSECURED);
        }
        return new MonitoredBranchResult(id, name, id, maximumFlow.orElse(Double.MIN_VALUE), preOptimisationFlow, preOptimisationFlow);
    }

}
