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
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
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

        // 1. pre
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId);
        if (loadFlowResult.isOk()) {
            buildFlowFromNetwork(network, crac, cnecFlowMap, null);
        }
        List<TwoWindingsTransformer> twoWindingsTransformers = getPstInRangeActions(network, crac.getRangeActions());
        SensitivityComputationResults preSensi = runSensitivityComputation(network, crac, twoWindingsTransformers);
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

                            LoadFlowResult currentloadFlowResult = LoadFlowService.runLoadFlow(network, workingVariant);
                            if (currentloadFlowResult.isOk()) {
                                buildFlowFromNetwork(network, crac, cnecFlowMap, contingency);
                            }

                            SensitivityComputationResults sensiResults = runSensitivityComputation(network, crac, twoWindingsTransformers);
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

        return new SystematicSensitivityAnalysisResult(stateSensiMap, cnecFlowMap);
    }

    private static void buildFlowFromNetwork(Network network, Crac crac, Map<Cnec, Double> cnecFlowMap, Contingency contingency) {
        Set<State> states = new HashSet<>();
        if (contingency == null) {
            states.add(crac.getPreventiveState());
        } else {
            states.addAll(crac.getStates(contingency));
        }

        states.forEach(state -> crac.getCnecs(state).forEach(cnec -> {
            cnecFlowMap.put(cnec, cnec.getP(network));
        }));
    }

    private static List<TwoWindingsTransformer> getPstInRangeActions(Network network, Set<RangeAction> rangeActions) {
        List<TwoWindingsTransformer> psts = new ArrayList<>();
        for (RangeAction rangeAction : rangeActions) {
            Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
            for (NetworkElement networkElement : networkElements) {
                if (isPst(network, networkElement)) {
                    psts.add(network.getTwoWindingsTransformer(networkElement.getId()));
                } else {
                    LOGGER.warn("In SystematicSensitivityAnalysisService getPstInRangeActions: not supported type of range action");
                }
            }
        }
        return psts;
    }

    private static SensitivityComputationResults runSensitivityComputation(
            Network network,
            Crac crac,
            List<TwoWindingsTransformer> psts) {
        SensitivityFactorsProvider factorsProvider = net -> {
            List<SensitivityFactor> factors = new ArrayList<>();
            crac.getCnecs().forEach(cnec -> {
                String monitoredBranchId = cnec.getId();
                String monitoredBranchName = cnec.getName();
                String branchId = cnec.getNetworkElement().getId();
                BranchFlow branchFlow = new BranchFlow(monitoredBranchId, monitoredBranchName, branchId);
                psts.forEach(twt -> {
                    String twtId = twt.getId();
                    factors.add(new BranchFlowPerPSTAngle(branchFlow,
                            new PhaseTapChangerAngle(twtId, twtId, twtId)));
                });
            });
            return factors;
        };

        if (factorsProvider.getFactors(network).isEmpty()) {
            return new SensitivityComputationResults(false, Collections.emptyMap(), "", new ArrayList<>());
        }
        try {
            return SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            return null;
        }

    }

    private static boolean isPst(Network network, NetworkElement networkElement) {
        return network.getTwoWindingsTransformer(networkElement.getId()) != null;
    }

}
