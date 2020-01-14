/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_range_action_rao;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;

import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.ra_optimisation.MonitoredBranchResult;
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

        Map<Cnec, Double> cnecMarginMap = analysisResult.getCnecFlowMap();

        List<MonitoredBranchResult> resultList = new ArrayList<>();
        for (Map.Entry<Cnec, Double> entry : cnecMarginMap.entrySet()) {
            Cnec cnec = entry.getKey();
            double refereceFlow = entry.getValue();
            double margin = 0;
            try {
                margin = cnec.getThreshold().getMaxThreshold().orElse(0.0) - refereceFlow;
            } catch (SynchronizationException e) {
                LOGGER.error("Cannot comput margin for cnec {}. {}", cnec.getId(), e.getMessage());
            }
            LOGGER.info("Margin for cnec {} is {}", cnec.getId(), (int) margin);

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

        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS,
                null, null); //todo change RaoComputationResult with state?
        raoComputationResult.addExtension(LinearRangeActionRaoResult.class, resultExtension);
        LOGGER.info("LinearRangeActionRaoResult: mininum margin = {}, security status: {}", (int) resultExtension.getMinMargin(), resultExtension.getSecurityStatus());
        // 4. return
        return CompletableFuture.completedFuture(raoComputationResult);
    }

}
