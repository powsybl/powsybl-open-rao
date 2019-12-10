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
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public final class SensitivitySecurityAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivitySecurityAnalysisService.class);

    private SensitivitySecurityAnalysisService() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static SensitivitySecurityAnalysisResult runSensitivity(Network network,
                                                                   Crac crac,
                                                                   ComputationManager computationManager) {
        //prepare range actions: pst ( and hvdc in the future )
        List<TwoWindingsTransformer> twoWindingsTransformers = getPstInRangeActions(network, crac.getRangeActions());

        //contingency = null : pre-contingency sensitivity computation analysis
        LOGGER.info("Running pre contingency sensitivity computation");
        SensitivityComputationResults precontingencyResult = runSensitivityComputation(network, crac, twoWindingsTransformers);

        //get result per contingency
        LOGGER.info("Calculating sensitivity result per contingency");
        Map<Contingency, SensitivityComputationResults> contingencySensitivityComputationResultsMap = new HashMap<>();

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId)) {
            variantsPool.submit(() -> crac.getContingencies().forEach(contingency -> {
                try {
                    LOGGER.info("Running post contingency sensitivity computation for contingency '{}'", contingency.getId());
                    String workingVariant = variantsPool.getAvailableVariant();
                    network.getVariantManager().setWorkingVariant(workingVariant);
                    applyCracContingency(network, computationManager, contingency);

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

        //return SensitivitySecurityAnalysisResult
        return new SensitivitySecurityAnalysisResult(precontingencyResult, contingencySensitivityComputationResultsMap);
    }

    public static void applyCracContingency(Network network, ComputationManager computationManager, Contingency contingency) {
        contingency.getNetworkElements().forEach(contingencyElement -> applyCracContingencyElement(network, computationManager, contingencyElement));
    }

    public static void applyCracContingencyElement(Network network, ComputationManager computationManager, NetworkElement contingencyElement) {
        Identifiable element = network.getIdentifiable(contingencyElement.getId());
        if (element instanceof Branch) {
            BranchContingency contingency = new BranchContingency(contingencyElement.getId());
            contingency.toTask().modify(network, computationManager);
        } else {
            throw new FaraoException("Unable to apply contingency element " + contingencyElement.getId());
        }
    }

    public static List<TwoWindingsTransformer> getPstInRangeActions(Network network, List<RangeAction> rangeActions) {
        List<TwoWindingsTransformer> psts = new ArrayList<>();
        for (RangeAction rangeAction : rangeActions) {
            List<NetworkElement> networkElements = rangeAction.getNetworkElements();
//            LOGGER.info("In RangeAction: " + rangeAction.getId());
            for (NetworkElement networkElement : networkElements) {
                if (isPst(network, networkElement)) {
//                    LOGGER.info("adding " + networkElement.getId());
                    psts.add(network.getTwoWindingsTransformer(networkElement.getId()));
                } else {
                    LOGGER.warn("not support type of range action");
                }
            }
        }
        return psts;
    }

    public static SensitivityComputationResults runSensitivityComputation(
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

    public static boolean isPst(Network network, NetworkElement networkElement) {
        return network.getTwoWindingsTransformer(networkElement.getId()) != null;
    }

}
