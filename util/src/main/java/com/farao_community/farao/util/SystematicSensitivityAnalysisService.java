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
import com.powsybl.contingency.BranchContingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
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
                                                                  ComputationManager computationManager) {
        String initialVariantId = network.getVariantManager().getWorkingVariantId();

        LOGGER.info("Running pre contingency sensitivity computation");
        Map<String, Double> preReferenceFlow = new HashMap<>();
        LoadFlowResult loadFlowResult = LoadFlowService.runLoadFlow(network, initialVariantId); //todo, how to run load flow ...
        if (loadFlowResult.isOk()) {
            buildReferenceFlowFromNetwork(network, crac, preReferenceFlow);
        }

        //prepare range actions: pst ( and hvdc in the future )
        List<TwoWindingsTransformer> twoWindingsTransformers = getPstInRangeActions(network, crac.getRangeActions());
        //contingency = null : pre-contingency sensitivity computation analysis
        SensitivityComputationResults precontingencyResult = runSensitivityComputation(network, crac, twoWindingsTransformers);

        //get result per contingency
        LOGGER.info("Calculating reference flow and sensitivity result per contingency");
        Map<Contingency, Map<String, Double> > contingencyReferenceFlowsMap = new HashMap<>();
        Map<Contingency, SensitivityComputationResults> contingencySensitivityComputationResultsMap = new HashMap<>();

        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId)) {
            variantsPool.submit(() -> crac.getContingencies().forEach(contingency -> {
                try {
                    LOGGER.info("Running post contingency sensitivity computation for contingency '{}'", contingency.getId());
                    String workingVariant = variantsPool.getAvailableVariant();
                    network.getVariantManager().setWorkingVariant(workingVariant);
                    applyContingencyInCrac(network, computationManager, contingency);

                    //run load flow
                    Map<String, Double> contingencyReferenceFlow = new HashMap<>();
                    LoadFlowResult currentloadFlowResult = LoadFlowService.runLoadFlow(network, workingVariant); //todo, how to run load flow ...
                    if (currentloadFlowResult.isOk()) {
                        buildReferenceFlowFromNetwork(network, crac, contingencyReferenceFlow);
                    }
                    contingencyReferenceFlowsMap.put(contingency, contingencyReferenceFlow);

                    //run sensi
                    SensitivityComputationResults sensiResults = runSensitivityComputation(network, crac, twoWindingsTransformers);
                    contingencySensitivityComputationResultsMap.put(contingency, sensiResults);

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

        //return SystematicSensitivityAnalysisResult
        return new SystematicSensitivityAnalysisResult(precontingencyResult, preReferenceFlow, contingencySensitivityComputationResultsMap, contingencyReferenceFlowsMap);
    }

    private static void buildReferenceFlowFromNetwork(Network network, Crac crac, Map<String, Double> referenceFlow) {
        Set<Cnec> cnecs = crac.getCnecs();
        for (Cnec cnec : cnecs) {
            double margin = 0.0;
            double referenceflowFromNetwork = 0.0;
            //get from network
            String cnecnetworkelementid = cnec.getCriticalNetworkElement().getId();
            Branch branch = network.getBranch(cnecnetworkelementid);
            if (branch == null) {
                LOGGER.warn("Cannot found branch in network for cnec: {} during building reference flow from network.", cnecnetworkelementid);
            } else {
                try {
                    margin = cnec.computeMargin(network);
                } catch (SynchronizationException e) {
                    e.printStackTrace();
                }

                referenceflowFromNetwork = network.getBranch(cnecnetworkelementid).getTerminal1().getP();
                if (Double.isNaN(referenceflowFromNetwork)) {
                    referenceflowFromNetwork = 0.0;
                    LOGGER.warn("Reference flow is set to 0.0 from NaN in network for cnec: {} during building reference flow from network", cnecnetworkelementid);
                }
                referenceFlow.put(cnecnetworkelementid, referenceflowFromNetwork);
                LOGGER.info("Building reference flow from network for cnec {} with value {}", cnecnetworkelementid, referenceflowFromNetwork);
            }
        }
    }

    private static void applyContingencyInCrac(Network network, ComputationManager computationManager, Contingency contingency) {
        contingency.getNetworkElements().forEach(contingencyElement -> applyContingencyElementInCrac(network, computationManager, contingencyElement));
    }

    private static void applyContingencyElementInCrac(Network network, ComputationManager computationManager, NetworkElement contingencyElement) {
        Identifiable element = network.getIdentifiable(contingencyElement.getId());
        if (element instanceof Branch) {
            BranchContingency contingency = new BranchContingency(contingencyElement.getId());
            contingency.toTask().modify(network, computationManager);
        } else {
            throw new FaraoException("Unable to apply contingency element " + contingencyElement.getId());
        }
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
                String branchId = cnec.getCriticalNetworkElement().getId();
                BranchFlow branchFlow = new BranchFlow(monitoredBranchId, monitoredBranchName, branchId);
                psts.forEach(twt -> {
                    String twtId = twt.getId();
                    factors.add(new BranchFlowPerPSTAngle(branchFlow,
                            new PhaseTapChangerAngle(twtId, twtId, twtId)));
                });
            });
            return factors;
        };

        return SensitivityComputationService.runSensitivity(network, network.getVariantManager().getWorkingVariantId(), factorsProvider);
    }

    private static boolean isPst(Network network, NetworkElement networkElement) {
        return network.getTwoWindingsTransformer(networkElement.getId()) != null;
    }

}
