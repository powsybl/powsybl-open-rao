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
        return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant, Collections.emptySet(), null);
    }

    static SensitivityAnalysisRunParameters configureSensiAnalysisWithoutRemedialActions(List<Contingency> contingencies,
                                                                                         List<SensitivityVariableSet> variableSets,
                                                                                         SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                                         Instant outageInstant,
                                                                                         Set<NetworkAction> networkActions,
                                                                                         AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                                                         Map<SensitivityState, Integer> instantOrderByState,
                                                                                         Network network) {
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
        // maximal set of possible actions is made from all network actions (even if not referenced in the operator strategy
        // to be able to efficiently fast restart sensitivity calculation)
        Set<Action> actions = new LinkedHashSet<>(networkActions.stream().flatMap(na -> na.getElementaryActions().stream()).toList());
        Set<String> simulatedActionIds = new LinkedHashSet<>();
        String operatorStrategyId = null;
        // To fix on OLF side, when a preventive action is applied it is not reapplied when calculating
        // curative actions after contingency.
        // As a workaround, we re-apply the preventive actions as curative actions with all contingencies.
        if (preventiveAppliedRemedialActions != null) {
            // only add preventive range actions, all network actions are already added
            var preventiveRangeActions = preventiveAppliedRemedialActions.getRangeActions().entrySet().stream().flatMap(e -> e.getKey().toActions(e.getValue(), network).stream()).toList();
            actions.addAll(preventiveRangeActions);
            simulatedActionIds.addAll(preventiveAppliedRemedialActions.getNetworkActions().stream().flatMap(e -> e.getElementaryActions().stream().map(Action::getId)).toList());
            simulatedActionIds.addAll(preventiveRangeActions.stream().map(Action::getId).toList());
            operatorStrategyId = "OS";
            // associate to N state and all contingencies
            ContingencyContext contingencyContext = contingencies.isEmpty() ? ContingencyContext.none() : ContingencyContext.all();
            operatorStrategies.add(new OperatorStrategy(operatorStrategyId, contingencyContext, new TrueCondition(), new ArrayList<>(simulatedActionIds)));
        }
        sensitivityComputationParameters.setOperatorStrategiesCalculationMode(operatorStrategyId != null
                ? SensitivityOperatorStrategiesCalculationMode.CONTINGENCIES_AND_OPERATOR_STRATEGIES
                : SensitivityOperatorStrategiesCalculationMode.NONE);
        instantOrderByState.put(new SensitivityState(null, operatorStrategyId), outageInstant.getOrder());
        for (Contingency contingency : contingencies) {
            instantOrderByState.put(new SensitivityState(contingency.getId(), operatorStrategyId), outageInstant.getOrder());
        }
        return new SensitivityAnalysisRunParameters()
                .setParameters(sensitivityComputationParameters)
                .setContingencies(contingencies)
                .setOperatorStrategies(operatorStrategies)
                .setActions(new ArrayList<>(actions))
                .setVariableSets(variableSets);
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      Set<NetworkAction> networkActions,
                                                      AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result;
        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();
        try {
            List<Contingency> contingencies = cnecSensitivityProvider.getContingencies(network);
            List<SensitivityFactor> factors = cnecSensitivityProvider.getAllFactors(network);
            SensitivityAnalysisRunParameters runParameters = configureSensiAnalysisWithoutRemedialActions(
                    contingencies,
                    cnecSensitivityProvider.getVariableSets(),
                    sensitivityComputationParameters,
                    outageInstant,
                    networkActions,
                    preventiveAppliedRemedialActions,
                    instantOrderByState,
                    network);
            result = SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    factors,
                    runParameters);
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return new SystematicSensitivityResult().completeData(result, instantOrderByState)
                .postTreatIntensities()
                .postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    static SensitivityAnalysisRunParameters configureSensiAnalysisWithRemedialActions(List<SensitivityVariableSet> variableSets,
                                                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                                      Set<State> statesWithRa,
                                                                                      Set<NetworkAction> networkActions,
                                                                                      AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                                                      AppliedRemedialActions appliedRemedialActions,
                                                                                      Map<SensitivityState, Integer> instantOrderByState,
                                                                                      Network network) {
        List<Contingency> contingencies = new ArrayList<>();

        // maximal set of possible actions is made from all network actions (even if not referenced in the operator strategy
        // to be able to efficiently fast restart sensitivity calculation)
        Set<Action> actions = new LinkedHashSet<>(networkActions.stream().flatMap(na -> na.getElementaryActions().stream()).toList());
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
        Set<String> simulatedActionIds = new LinkedHashSet<>();
        // we concat preventive actions and remedial actions
        // - preventive actions are applied to all contingencies
        if (preventiveAppliedRemedialActions != null) {
            // only add preventive range actions, all network actions are already added
            var preventiveRangeActions = preventiveAppliedRemedialActions.getRangeActions().entrySet().stream().flatMap(e -> e.getKey().toActions(e.getValue(), network).stream()).toList();
            actions.addAll(preventiveRangeActions);
            simulatedActionIds.addAll(preventiveAppliedRemedialActions.getNetworkActions().stream().flatMap(e -> e.getElementaryActions().stream().map(Action::getId)).toList());
            simulatedActionIds.addAll(preventiveRangeActions.stream().map(Action::getId).toList());
        }
        // - remedial actions are applied to the contingency of the state with RA
        for (State state : statesWithRa) {
            Contingency contingency = state.getContingency().orElseThrow(() ->
                    new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.")
            );
            contingencies.add(contingency);

            List<Action> curativeActionsForState = appliedRemedialActions.toActions(state, network);
            actions.addAll(curativeActionsForState);
            String operatorStrategyId = "OS-" + contingency.getId();
            simulatedActionIds.addAll(curativeActionsForState.stream().map(Action::getId).toList());
            operatorStrategies.add(new OperatorStrategy(operatorStrategyId,
                    ContingencyContext.specificContingency(contingency.getId()),
                    new TrueCondition(),
                    new ArrayList<>(simulatedActionIds)));

            instantOrderByState.put(new SensitivityState(contingency.getId(), operatorStrategyId), state.getInstant().getOrder());
        }
        sensitivityComputationParameters.setOperatorStrategiesCalculationMode(SensitivityOperatorStrategiesCalculationMode.ONLY_OPERATOR_STRATEGIES);
        return new SensitivityAnalysisRunParameters()
                .setParameters(sensitivityComputationParameters)
                .setContingencies(contingencies)
                .setOperatorStrategies(operatorStrategies)
                .setActions(new ArrayList<>(actions))
                .setVariableSets(variableSets);
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                      AppliedRemedialActions appliedRemedialActions,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      Set<NetworkAction> networkActions) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant,
                    networkActions, preventiveAppliedRemedialActions);
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
        TECHNICAL_LOGS.debug("{} state(s) without RA ", statesWithoutRa.size());

        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .flatMap(state -> state.getContingency().stream())
            .distinct()
            .toList();

        List<SensitivityFactor> allFactorsWithoutRa = cnecSensitivityProvider.getBasecaseFactors(network);
        allFactorsWithoutRa.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));

        SystematicSensitivityResult result = new SystematicSensitivityResult();
        try {
            Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();
            SensitivityAnalysisRunParameters runParameters = configureSensiAnalysisWithoutRemedialActions(
                    contingenciesWithoutRa,
                    cnecSensitivityProvider.getVariableSets(),
                    sensitivityComputationParameters,
                    outageInstant,
                    networkActions,
                    preventiveAppliedRemedialActions,
                    instantOrderByState,
                    network);

            var sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                    network.getVariantManager().getWorkingVariantId(),
                    allFactorsWithoutRa,
                    runParameters);
            result.completeData(sensiResult, instantOrderByState);
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis without RA failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }

        // systematic analyses for states with RA
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("{} state(s) with RA {}", statesWithRa.size());

        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();
        SensitivityAnalysisRunParameters runParameters = configureSensiAnalysisWithRemedialActions(
                cnecSensitivityProvider.getVariableSets(),
                sensitivityComputationParameters,
                statesWithRa, networkActions, preventiveAppliedRemedialActions, appliedRemedialActions,
                instantOrderByState,
                network);

        var factors = cnecSensitivityProvider.getContingencyFactors(network, runParameters.getContingencies());
        try {
            var sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                                                                                network.getVariantManager().getWorkingVariantId(),
                                                                                factors,
                                                                                runParameters);
            result.completeData(sensiResult, instantOrderByState);
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis with RA failed: %s", e.getMessage()));
            SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                factors,
                instantOrderByState.keySet().stream()
                    .filter(state -> state.contingencyId() != null)
                    .map(state -> new SensitivityAnalysisResult.SensitivityStateStatus(state, SensitivityAnalysisResult.Status.FAILURE))
                    .toList(),
                runParameters.getContingencies().stream().map(Contingency::getId).toList(),
                runParameters.getOperatorStrategies().stream().map(OperatorStrategy::getId).toList(),
                List.of()
            );
            result.completeData(failedResult, instantOrderByState);
        }

        // enable preventive factors for next iterations
        cnecSensitivityProvider.enableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");

        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }
}
