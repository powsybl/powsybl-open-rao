/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.strategy.OperatorStrategy;
import com.powsybl.contingency.strategy.condition.TrueCondition;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.sensitivity.*;

import java.util.*;
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
        return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant, null);
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      AppliedRemedialActions appliedRemedialActionsBefore) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result;
        try {
            List<OperatorStrategy> operatorStrategies = new ArrayList<>();
            List<Action> actions = new ArrayList<>();
            if (appliedRemedialActionsBefore != null && !appliedRemedialActionsBefore.isEmpty(network)) {
                System.out.println("PROUT");
                for (State state : appliedRemedialActionsBefore.getStatesWithRa(network)) {
                    actions.addAll(appliedRemedialActionsBefore.getAppliedNetworkActions(state).stream().flatMap(a -> a.getElementaryActions().stream()).toList());
                }
                operatorStrategies.add(new OperatorStrategy("TOTO", ContingencyContext.none(), new TrueCondition(),
                        actions.stream().map(Action::getId).toList()));
                System.out.println("PROUT2 " + actions.size());
                sensitivityComputationParameters.setOperatorStrategiesCalculationMode(SensitivityOperatorStrategiesCalculationMode.CONTINGENCIES_AND_OPERATOR_STRATEGIES);
            }
            SensitivityAnalysisRunParameters runParameters = new SensitivityAnalysisRunParameters()
                    .setParameters(sensitivityComputationParameters)
                    .setContingencies(cnecSensitivityProvider.getContingencies(network))
                    .setOperatorStrategies(operatorStrategies)
                    .setActions(actions)
                    .setVariableSets(cnecSensitivityProvider.getVariableSets());
            result = SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    cnecSensitivityProvider.getAllFactors(network),
                    runParameters);
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result, outageInstant.getOrder()).postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions appliedRemedialActionsBefore,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant,
                    appliedRemedialActionsBefore);
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
            .flatMap(state -> state.getContingency().stream())
            .distinct()
            .toList();

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        try {
            SensitivityAnalysisRunParameters runParameters = new SensitivityAnalysisRunParameters()
                    .setParameters(sensitivityComputationParameters)
                    .setContingencies(contingenciesWithoutRa)
                    .setVariableSets(cnecSensitivityProvider.getVariableSets());
            result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                allFactorsWithoutRa,
                runParameters), outageInstant.getOrder());
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

//            if (optContingency.isEmpty()) {
//                throw new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.");
//            }

            TECHNICAL_LOGS.debug("... ({}/{}) state with RA {}", counterForLogs, statesWithRa.size() + 1, state.getId());

            //TODO: We can save a bit of time by unapplying previous remedial actions here if we find a clean way to do it
            network.getVariantManager().cloneVariant(workingVariantId, variantForState, true);
            shouldRemoveVariant = true;
            network.getVariantManager().setWorkingVariant(variantForState);

            appliedRemedialActions.applyOnNetwork(state, network);

            List<Contingency> contingencyList = optContingency.stream().toList();

            try {
                SensitivityAnalysisRunParameters runParameters = new SensitivityAnalysisRunParameters()
                        .setParameters(sensitivityComputationParameters)
                        .setContingencies(contingencyList)
                        .setVariableSets(cnecSensitivityProvider.getVariableSets());
                result.completeData(SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                    runParameters), state.getInstant().getOrder());
            } catch (PowsyblException | OpenRaoException | CompletionException e) {
                TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed for state %s : %s", state.getId(), e.getMessage()));
                SensitivityState sensitivityState = optContingency.map(c -> SensitivityState.postContingency(c.getId())).orElseGet(() -> SensitivityState.PRE_CONTINGENCY);
                SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                    cnecSensitivityProvider.getContingencyFactors(network, contingencyList),
                    List.of(new SensitivityAnalysisResult.SensitivityStateStatus(sensitivityState, SensitivityAnalysisResult.Status.FAILURE)),
                    contingencyList.stream().map(Contingency::getId).toList(),
                    List.of(),
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
