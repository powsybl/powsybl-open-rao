/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisUtil.convertCracContingencyToPowsybl;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
final class SystematicSensitivityAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAdapter.class);

    private SystematicSensitivityAdapter() {
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters) {
        LOGGER.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result = SensitivityAnalysis.run(network, cnecSensitivityProvider, cnecSensitivityProvider.getContingencies(network), sensitivityComputationParameters);
        LOGGER.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result).postTreatIntensities();
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters) {

        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty()) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters);
        }

        LOGGER.debug("Systematic sensitivity analysis with applied RA [start]");

        Set<State> statesWithoutRa = getStatesWithoutRa(appliedRemedialActions, cnecSensitivityProvider);
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa();

        // systematic analysis for states without RA
        LOGGER.debug("... (1/{}) {} state(s) without RA ", statesWithRa.size() + 1, statesWithoutRa.size());

        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .filter(state -> state.getContingency().isPresent())
            .map(state -> convertCracContingencyToPowsybl(state.getContingency().get(), network))
            .collect(Collectors.toList());

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        result.completeData(SensitivityAnalysis.run(network, cnecSensitivityProvider, contingenciesWithoutRa, sensitivityComputationParameters));

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int counterForLogs = 2;
        for (State state : appliedRemedialActions.getStatesWithRa()) {

            if (state.getContingency().isEmpty()) {
                throw new FaraoException("Sensitivity analysis with applied RA does not handled preventive RA.");
            }

            com.farao_community.farao.data.crac_api.Contingency contingency = state.getContingency().get();

            LOGGER.debug("... ({}/{}) curative state {}", counterForLogs, appliedRemedialActions.getStatesWithRa().size() + 1, contingency.getId());

            String variantForState = RandomizedString.getRandomizedString();
            network.getVariantManager().cloneVariant(workingVariantId, variantForState);
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = Collections.singletonList(convertCracContingencyToPowsybl(contingency, network));

            result.completeData(SensitivityAnalysis.run(network, variantForState, cnecSensitivityProvider, contingencyList, sensitivityComputationParameters));
            network.getVariantManager().removeVariant(variantForState);
            counterForLogs++;
        }

        LOGGER.debug("Systematic sensitivity analysis with applied RA [end]");

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensities();
    }

    private static Set<State> getStatesWithoutRa(AppliedRemedialActions appliedRemedialActions, CnecSensitivityProvider cnecSensitivityProvider) {
        Set<State> states = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        states.removeAll(appliedRemedialActions.getStatesWithRa());
        return states;
    }
}
