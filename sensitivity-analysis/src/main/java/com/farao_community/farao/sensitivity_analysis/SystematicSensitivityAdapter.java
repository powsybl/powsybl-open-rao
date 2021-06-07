/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.DefaultComputationManagerConfig;
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
        LOGGER.debug("Sensitivity analysis without applied RA [start]");
        SensitivityAnalysisResult result = SensitivityAnalysis.run(network, cnecSensitivityProvider, cnecSensitivityProvider.getContingencies(network), sensitivityComputationParameters);
        LOGGER.debug("Sensitivity analysis without applied RA [end]");
        return new SystematicSensitivityResult(result);
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      AppliedRemedialActions appliedRemedialActions) {

        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty()) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters);
        }

        // systematic analysis for states without RA
        LOGGER.debug("Sensitivity analysis with applied RA [start]");
        LOGGER.debug("- states without RA");

        Set<State> statesWithoutRa = getStatesWithoutRa(appliedRemedialActions, cnecSensitivityProvider);
        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .filter(state -> state.getContingency().isPresent())
            .map(state -> convertCracContingencyToPowsybl(state.getContingency().get(), network))
            .collect(Collectors.toList());

        //todo : better handling of the cnecSensitivityProvider to avoid the computation of useless sensis
        SensitivityAnalysisResult resultWithoutRa = SensitivityAnalysis.run(network, cnecSensitivityProvider, contingenciesWithoutRa, sensitivityComputationParameters);
        SystematicSensitivityResult result = new SystematicSensitivityResult(resultWithoutRa);

        // systematic analyses for states with RA
        Map<State, SensitivityAnalysisResult> resultWithRaMap = new HashMap<>();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int i = 1;
        for (State state : appliedRemedialActions.getStatesWithRa()) {

            LOGGER.debug("- curative state {} with RA [{}/{}]", i, state.getContingency().get().getId(), appliedRemedialActions.getStatesWithRa().size());

            String variantForState = RandomizedString.getRandomizedString();
            network.getVariantManager().cloneVariant(workingVariantId, variantForState);
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.apply(state, network);

            List<Contingency> contingency = Collections.singletonList(convertCracContingencyToPowsybl(state.getContingency().get(), network));

            SensitivityAnalysisResult resultWithRa = SensitivityAnalysis.run(network, variantForState, cnecSensitivityProvider, contingency, sensitivityComputationParameters);
            resultWithRaMap.put(state, resultWithRa);

            result.fillData(resultWithRa);
            network.getVariantManager().removeVariant(variantForState);
            i++;
        }

        // merge results
        result.postTreatIntensities();

        network.getVariantManager().setWorkingVariant(workingVariantId);

        return result;
    }

    private static Set<State> getStatesWithoutRa(AppliedRemedialActions appliedRemedialActions, CnecSensitivityProvider cnecSensitivityProvider) {
        Set<State> states = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        states.removeAll(appliedRemedialActions.getStatesWithRa());
        return states;
    }
}
