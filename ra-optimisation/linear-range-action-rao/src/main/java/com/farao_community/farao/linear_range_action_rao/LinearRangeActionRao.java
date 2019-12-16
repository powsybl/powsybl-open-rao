/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;

import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.MonitoredBranchResult;
import com.farao_community.farao.ra_optimisation.PreContingencyResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.util.SensitivityComputationService;
import com.farao_community.farao.util.SensitivitySecurityAnalysisResult;
import com.farao_community.farao.util.SensitivitySecurityAnalysisService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class LinearRangeActionRao implements RaoProvider {

    private Network network;
    private Crac crac;
    private ComputationManager computationManager;

    public LinearRangeActionRao(Network network,
                                 Crac crac,
                                 ComputationManager computationManager,
                                 SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = network;
        this.crac = crac;
        this.computationManager = computationManager;
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
    }

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
        SensitivitySecurityAnalysisResult sensiSaResults = SensitivitySecurityAnalysisService.runSensitivity(network, crac, computationManager);

        SensitivityComputationResults preSensi = sensiSaResults.getPrecontingencyResult();
        PreContingencyResult preRao = new PreContingencyResult();
        // 1. do for pre
        // get cnec from crac
        crac.getCnecs().forEach(cnec -> {
            // cnec.getCriticalNetworkElement().getId()
            preSensi.getSensitivityValues().forEach(sensitivityValue -> {
                String id = sensitivityValue.getFactor().getFunction().getId();
                String name = sensitivityValue.getFactor().getFunction().getName();
                double maximumFlow = Integer.MAX_VALUE; //todo ? how to get max from Crac?
                double preOptimisationFlow = sensitivityValue.getFunctionReference();

                MonitoredBranchResult monitoredBranchResult = new MonitoredBranchResult(id, name, id, maximumFlow, preOptimisationFlow, preOptimisationFlow);
                preRao.addMonitoredBranchResult(monitoredBranchResult);
            });
        });

        // 2. do for each contingency
        Map<Contingency, SensitivityComputationResults>  mapSensi = sensiSaResults.getResultMap();
        List<ContingencyResult> contingencyResultsRao = new ArrayList<>();
        for (Contingency contingency : mapSensi.keySet()) {
            String idContSensi = contingency.getId();
            String nameContSensi = contingency.getName();
            ContingencyResult contingencyResult = new ContingencyResult(idContSensi, nameContSensi);

            SensitivityComputationResults sensitivityComputationResults = mapSensi.get(contingency);

            crac.getCnecs().forEach(cnec -> {
                // cnec.getCriticalNetworkElement().getId()
                sensitivityComputationResults.getSensitivityValues().forEach(sensitivityValue -> {
                    String id = sensitivityValue.getFactor().getFunction().getId();
                    String name = sensitivityValue.getFactor().getFunction().getName();
                    double maximumFlow = Integer.MAX_VALUE; //todo ? how to get max from Crac?
                    double preOptimisationFlow = sensitivityValue.getFunctionReference();

                    MonitoredBranchResult monitoredBranchResult = new MonitoredBranchResult(id, name, id, maximumFlow, preOptimisationFlow, preOptimisationFlow);
                    contingencyResult.addMonitoredBranchResult(monitoredBranchResult);
                });
            });

            contingencyResultsRao.add(contingencyResult);
        }

        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS, preRao, contingencyResultsRao);
        return CompletableFuture.completedFuture(raoComputationResult);
    }
}
