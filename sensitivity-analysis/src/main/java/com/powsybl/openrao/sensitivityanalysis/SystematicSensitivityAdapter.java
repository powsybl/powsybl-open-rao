/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

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
                                                      Instant outageInstant) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result;
        try {
            result = SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                cnecSensitivityProvider.getAllFactors(network),
                cnecSensitivityProvider.getContingencies(network),
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters);
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result, outageInstant.getOrder()).postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant);
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
            .map(state -> state.getContingency().get())
            .distinct()
            .toList();

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        try {
            result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                allFactorsWithoutRa,
                contingenciesWithoutRa,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters), outageInstant.getOrder());
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        int counterForLogs = 2;

        String variantForState = RandomizedString.getRandomizedString();
        boolean shouldRemoveVariant = false;
        for (State state : statesWithRa) {

            Optional<Contingency> optContingency = state.getContingency();

            if (optContingency.isEmpty()) {
                throw new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.");
            }

            TECHNICAL_LOGS.debug("... ({}/{}) state with RA {}", counterForLogs, statesWithRa.size() + 1, state.getId());

            //TODO: We can save a bit of time by unapplying previous remedial actions here if we find a clean way to do it
            network.getVariantManager().cloneVariant(workingVariantId, variantForState, true);
            shouldRemoveVariant = true;
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = Collections.singletonList(optContingency.get());

            try {
                result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                    contingencyList,
                    cnecSensitivityProvider.getVariableSets(),
                    sensitivityComputationParameters), state.getInstant().getOrder());
            } catch (PowsyblException | OpenRaoException | CompletionException e) {
                TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed for state %s : %s", state.getId(), e.getMessage()));
                SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                    cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                    List.of(new SensitivityAnalysisResult.SensitivityContingencyStatus(optContingency.get().getId(), SensitivityAnalysisResult.Status.FAILURE)),
                    List.of()
                );
                result.completeData(failedResult, state.getInstant().getOrder());
            }
            counterForLogs++;
        }

        if (shouldRemoveVariant) {
            network.getVariantManager().removeVariant(variantForState);
        }

        // enable preventive factors for next iterations
        cnecSensitivityProvider.enableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }
}
