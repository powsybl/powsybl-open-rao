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
        // TODO: replace find() with find(providerName)
        SensitivityAnalysisResult result = SensitivityAnalysis.find().run(network,
            cnecSensitivityProvider.getAllFactors(network),
            cnecSensitivityProvider.getContingencies(network),
            cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters);
        LOGGER.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result, false).postTreatIntensities();
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters) {

        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty()) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters);
        }

        LOGGER.debug("Systematic sensitivity analysis with applied RA [start]");

        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa();
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        // systematic analysis for states without RA
        LOGGER.debug("... (1/{}) {} state(s) without RA ", statesWithRa.size() + 1, statesWithoutRa.size());

        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .filter(state -> state.getContingency().isPresent())
            .map(state -> convertCracContingencyToPowsybl(state.getContingency().get(), network))
            .collect(Collectors.toList());

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        result.completeData(SensitivityAnalysis.find().run(network,
            allFactorsWithoutRa,
            contingenciesWithoutRa,
            cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters), false);

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int counterForLogs = 2;
        for (State state : appliedRemedialActions.getStatesWithRa()) {

            Optional<com.farao_community.farao.data.crac_api.Contingency> optContingency = state.getContingency();

            if (optContingency.isEmpty()) {
                throw new FaraoException("Sensitivity analysis with applied RA does not handled preventive RA.");
            }

            LOGGER.debug("... ({}/{}) curative state {}", counterForLogs, appliedRemedialActions.getStatesWithRa().size() + 1, optContingency.get().getId());

            String variantForState = RandomizedString.getRandomizedString();
            network.getVariantManager().cloneVariant(workingVariantId, variantForState);
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = Collections.singletonList(convertCracContingencyToPowsybl(optContingency.get(), network));

            result.completeData(SensitivityAnalysis.find().run(network,
                cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                contingencyList,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters), true);
            network.getVariantManager().removeVariant(variantForState);
            counterForLogs++;
        }

        LOGGER.debug("Systematic sensitivity analysis with applied RA [end]");

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensities();
    }
}
