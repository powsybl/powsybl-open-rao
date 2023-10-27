/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactor;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;
import static com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisUtil.convertCracContingencyToPowsybl;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
final class SystematicSensitivityAdapter {
    private SystematicSensitivityAdapter() {
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant instantOutage) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result;
        try {
            result = SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                cnecSensitivityProvider.getAllFactors(network),
                cnecSensitivityProvider.getContingencies(network),
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters);
        } catch (Exception e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result, instantOutage.getOrder()).postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    private static int getOrderInstantOutage(CnecSensitivityProvider cnecSensitivityProvider) {
        for (FlowCnec flowCnec : cnecSensitivityProvider.getFlowCnecs()) {
            Instant instant = flowCnec.getState().getInstant();
            while (instant != null) {
                System.out.println(instant);
                if (instant.getInstantKind() == InstantKind.OUTAGE) {
                    return instant.getOrder();
                }
                instant = instant.getPreviousInstant();
            }
        }
        throw new FaraoException("Outage instant was not found in the CNECs");
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant instantOutage) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, instantOutage);
        }

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [start]");
        // Information : for contingencies with auto RA but no curative RA, SystematicSensitivityResult::getCnecStateResult will
        // retrieve sensi information for curative state from auto state to take into account auto RAs.
        // (When auto AND curative RAs are applied, they will both be included in statesWithRa and both sensis
        // are computed.)
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa(network);
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        // systematic analysis for states without RA
        TECHNICAL_LOGS.debug("... (1/{}) {} state(s) without RA ", statesWithRa.size() + 1, statesWithoutRa.size());

        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .filter(state -> state.getContingency().isPresent())
            .map(state -> convertCracContingencyToPowsybl(state.getContingency().get(), network))
            .distinct()
            .toList();

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        OptionalInt instantOrderClosestToPreventiveWithoutRa = statesWithoutRa.stream().map(State::getInstant)
            .filter(instant -> instant.getInstantKind() != InstantKind.PREVENTIVE)
            .mapToInt(Instant::getOrder)
            .min();
        if (instantOrderClosestToPreventiveWithoutRa.isPresent()) {
            result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                allFactorsWithoutRa,
                contingenciesWithoutRa,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters), instantOrderClosestToPreventiveWithoutRa.getAsInt());
        }

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int counterForLogs = 2;
        for (State state : statesWithRa) {

            Optional<com.farao_community.farao.data.crac_api.Contingency> optContingency = state.getContingency();

            if (optContingency.isEmpty()) {
                throw new FaraoException("Sensitivity analysis with applied RA does not handle preventive RA.");
            }

            TECHNICAL_LOGS.debug("... ({}/{}) state with RA {}", counterForLogs, statesWithRa.size() + 1, state.getId());

            String variantForState = RandomizedString.getRandomizedString();
            network.getVariantManager().cloneVariant(workingVariantId, variantForState);
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = Collections.singletonList(convertCracContingencyToPowsybl(optContingency.get(), network));

            result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                contingencyList,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters), state.getInstant().getOrder());
            network.getVariantManager().removeVariant(variantForState);
            counterForLogs++;
        }

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }
}
