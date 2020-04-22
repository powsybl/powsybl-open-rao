/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class SystematicSensitivityAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisService.class);

    private SystematicSensitivityAnalysisService() {
    }

    public static SystematicSensitivityAnalysisResult runAnalysis(Network network,
                                                                  Crac crac,
                                                                  ComputationManager computationManager,
                                                                  SensitivityComputationParameters sensitivityComputationParameters) {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();

        Map<State, SensitivityComputationResults> stateSensiMap = new HashMap<>();
        Map<Cnec, Double> cnecFlowMap = new HashMap<>();
        Map<Cnec, Double> cnecIntensityMap = new HashMap<>();

        // 1. pre
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId, sensitivityComputationParameters.getLoadFlowParameters());
        if (loadFlowResult.isOk()) {
            buildFlowFromNetwork(network, crac, cnecFlowMap, cnecIntensityMap, null);
        }
        SensitivityComputationResults preSensi = runSensitivityComputation(network, crac, sensitivityComputationParameters);
        stateSensiMap.put(crac.getPreventiveState(), preSensi);

        // 2. analysis for each contingency
        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId)) {
            variantsPool.submit(() -> crac.getContingencies().stream()
                    .filter(c -> crac.getStates(c).stream().mapToLong(s -> crac.getCnecs(s).size()).sum() > 0)
                    .forEach(contingency -> {
                        try {
                            String workingVariant = variantsPool.getAvailableVariant();
                            network.getVariantManager().setWorkingVariant(workingVariant);
                            contingency.apply(network, computationManager);

                            LoadFlowResult currentloadFlowResult = LoadFlowService.runLoadFlow(network, workingVariant, sensitivityComputationParameters.getLoadFlowParameters());
                            if (currentloadFlowResult.isOk()) {
                                buildFlowFromNetwork(network, crac, cnecFlowMap, cnecIntensityMap, contingency);
                            }

                            SensitivityComputationResults sensiResults = runSensitivityComputation(network, crac, sensitivityComputationParameters);
                            crac.getStates(contingency).forEach(state -> {
                                if (!stateSensiMap.containsKey(state)) {
                                    stateSensiMap.put(state, sensiResults);
                                }
                            });

                            variantsPool.releaseUsedVariant(workingVariant);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    })).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new FaraoException(e);
        }
        network.getVariantManager().setWorkingVariant(initialVariantId);

        return new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap, cnecIntensityMap);
    }

    private static void buildFlowFromNetwork(Network network,
                                             Crac crac,
                                             Map<Cnec, Double> cnecFlowMap,
                                             Map<Cnec, Double> cnecIntensityMap,
                                             Contingency contingency) {
        Set<State> states = new HashSet<>();
        if (contingency == null) {
            states.add(crac.getPreventiveState());
        } else {
            states.addAll(crac.getStates(contingency));
        }

        states.forEach(state -> crac.getCnecs(state).forEach(cnec -> {
            cnecFlowMap.put(cnec, cnec.getP(network));
            cnecIntensityMap.put(cnec, cnec.getI(network));
        }));
    }

    private static SensitivityComputationResults runSensitivityComputation(
            Network network,
            Crac crac,
            SensitivityComputationParameters sensitivityComputationParameters) {
        SensitivityFactorsProvider factorsProvider = new CracFactorsProvider(crac);

        if (factorsProvider.getFactors(network).isEmpty()) {
            return new SensitivityComputationResults(false, Collections.emptyMap(), "", new ArrayList<>());
        }
        try {
            return SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider, sensitivityComputationParameters);
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

}
