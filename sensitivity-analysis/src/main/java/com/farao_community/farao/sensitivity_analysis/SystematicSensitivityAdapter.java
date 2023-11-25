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
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.sensitivity_analysis.SensitivityAnalysisUtil.convertCracContingencyToPowsybl;
import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

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
                                                      String sensitivityProvider) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        try {

            List<SensitivityFactor> factors = cnecSensitivityProvider.getAllFactors(network);
            SystematicSensitivityResult result = new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
            List<Contingency> contingencies = cnecSensitivityProvider.getContingencies(network);

            SensitivityFactorReader factorReader = new SensitivityFactorModelReader(factors, network);
            Set<String> hvdcsToInvert = cnecSensitivityProvider.getHvdcs().keySet().stream()
                .filter(networkElementId -> network.getHvdcLine(networkElementId).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER).collect(Collectors.toSet());
            SensitivityResultWriter resultWriter = SystematicSensitivityResult.getSensitivityResultWriter(factors, result, Instant.OUTAGE, contingencies, hvdcsToInvert);

            SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                factorReader,
                resultWriter,
                contingencies,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters,
                LocalComputationManager.getDefault(),
                Reporter.NO_OP
            );

            TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
            return result.postTreatIntensitiesAndStatus();
        } catch (Exception e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider);
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
            .collect(Collectors.toList());

        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));

        SystematicSensitivityResult result = new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        List<Contingency> contingencies = cnecSensitivityProvider.getContingencies(network);

        SensitivityFactorReader factorReader = new SensitivityFactorModelReader(allFactorsWithoutRa, network);
        Set<String> hvdcsToInvert = cnecSensitivityProvider.getHvdcs().keySet().stream()
            .filter(networkElementId -> network.getHvdcLine(networkElementId).getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER).collect(Collectors.toSet());
        SensitivityResultWriter resultWriter = SystematicSensitivityResult.getSensitivityResultWriter(allFactorsWithoutRa, result, Instant.OUTAGE, contingencies, hvdcsToInvert);

        SensitivityAnalysis.find(sensitivityProvider).run(network,
            network.getVariantManager().getWorkingVariantId(),
            factorReader,
            resultWriter,
            contingencies,
            cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters,
            LocalComputationManager.getDefault(),
            Reporter.NO_OP
        );

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
            List<SensitivityFactor> contingencyFactors = cnecSensitivityProvider.getContingencyFactors(network, contingencyList);

            SensitivityFactorReader contingencyFactorsReader = new SensitivityFactorModelReader(contingencyFactors, network);
            SensitivityResultWriter contingencyResultWriter = SystematicSensitivityResult.getSensitivityResultWriter(contingencyFactors, result, state.getInstant(), contingencyList, hvdcsToInvert);

            SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                contingencyFactorsReader,
                contingencyResultWriter,
                contingencyList,
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters,
                LocalComputationManager.getDefault(),
                Reporter.NO_OP
            );
            network.getVariantManager().removeVariant(variantForState);
            counterForLogs++;
        }

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");

        network.getVariantManager().setWorkingVariant(workingVariantId);
        return result.postTreatIntensitiesAndStatus();
    }
}
