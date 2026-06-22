/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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

    /** Bundles the run parameters with their associated instant-order map to avoid output parameters. */
    private record RunConfig(SensitivityAnalysisRunParameters params, Map<SensitivityState, Integer> instantOrderByState) {
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant) {
        return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant, Collections.emptySet(), null);
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      Set<NetworkAction> networkActions,
                                                      AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        Set<State> allStates = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        SystematicSensitivityResult result = runWithoutRemedialActions(network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, outageInstant, networkActions, preventiveAppliedRemedialActions, allStates);
        if (result.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FAILURE) {
            return result;
        }
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
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
        // For contingencies with auto RA but no curative RA, SystematicSensitivityResult::getCnecStateResult
        // retrieves sensi for the curative state from the auto state to account for auto RAs.
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa(network);
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        TECHNICAL_LOGS.debug("{} state(s) without RA", statesWithoutRa.size());
        SystematicSensitivityResult result = runWithoutRemedialActions(network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, outageInstant, networkActions, preventiveAppliedRemedialActions, statesWithoutRa);
        if (result.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FAILURE) {
            return result;
        }

        TECHNICAL_LOGS.debug("{} state(s) with RA", statesWithRa.size());
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        runWithRemedialActions(result, network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider,
                statesWithRa, networkActions, preventiveAppliedRemedialActions, appliedRemedialActions);
        cnecSensitivityProvider.enableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    private static SystematicSensitivityResult runWithoutRemedialActions(Network network,
                                                                         CnecSensitivityProvider cnecSensitivityProvider,
                                                                         SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                         String sensitivityProvider,
                                                                         Instant outageInstant,
                                                                         Set<NetworkAction> networkActions,
                                                                         AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                                         Set<State> statesWithoutRa) {
        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
                .flatMap(state -> state.getContingency().stream())
                .distinct()
                .toList();
        List<SensitivityFactor> factors = cnecSensitivityProvider.getBasecaseFactors(network);
        factors.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        try {
            RunConfig config = configureWithoutRemedialActions(contingenciesWithoutRa, cnecSensitivityProvider.getVariableSets(),
                    sensitivityComputationParameters, outageInstant, networkActions, preventiveAppliedRemedialActions, network);
            SensitivityAnalysisResult sensiResult = runAnalysis(network, factors, config.params(), sensitivityProvider);
            return new SystematicSensitivityResult().completeData(sensiResult, config.instantOrderByState());
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis without RA failed: %s", e.getMessage()));
            return new SystematicSensitivityResult(SystematicSensitivityResult.SensitivityComputationStatus.FAILURE);
        }
    }

    private static void runWithRemedialActions(SystematicSensitivityResult result,
                                               Network network,
                                               CnecSensitivityProvider cnecSensitivityProvider,
                                               SensitivityAnalysisParameters sensitivityComputationParameters,
                                               String sensitivityProvider,
                                               Set<State> statesWithRa,
                                               Set<NetworkAction> networkActions,
                                               AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                               AppliedRemedialActions appliedRemedialActions) {
        RunConfig config = configureWithRemedialActions(cnecSensitivityProvider.getVariableSets(), sensitivityComputationParameters,
                statesWithRa, networkActions, preventiveAppliedRemedialActions, appliedRemedialActions, network);
        List<SensitivityFactor> factors = cnecSensitivityProvider.getContingencyFactors(network, config.params().getContingencies());
        try {
            SensitivityAnalysisResult sensiResult = runAnalysis(network, factors, config.params(), sensitivityProvider);
            result.completeData(sensiResult, config.instantOrderByState());
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis with RA failed: %s", e.getMessage()));
            SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                    factors,
                    config.instantOrderByState().keySet().stream()
                            .filter(state -> state.contingencyId() != null)
                            .map(state -> new SensitivityAnalysisResult.SensitivityStateStatus(state, SensitivityAnalysisResult.Status.FAILURE))
                            .toList(),
                    config.params().getContingencies().stream().map(Contingency::getId).toList(),
                    config.params().getOperatorStrategies().stream().map(OperatorStrategy::getId).toList(),
                    List.of()
            );
            result.completeData(failedResult, config.instantOrderByState());
        }
    }

    private static RunConfig configureWithoutRemedialActions(List<Contingency> contingencies,
                                                             List<SensitivityVariableSet> variableSets,
                                                             SensitivityAnalysisParameters sensitivityComputationParameters,
                                                             Instant outageInstant,
                                                             Set<NetworkAction> networkActions,
                                                             AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                             Network network) {
        Set<Action> actions = new LinkedHashSet<>(networkActions.stream().flatMap(na -> na.getElementaryActions().stream()).toList());
        Set<String> simulatedActionIds = new LinkedHashSet<>();
        String operatorStrategyId = null;
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();

        if (preventiveAppliedRemedialActions != null) {
            // OLF workaround: preventive actions are not re-applied when computing curative sensitivity after contingency,
            // so we re-inject them as an operator strategy covering all contingencies.
            addPreventiveActions(actions, simulatedActionIds, preventiveAppliedRemedialActions, network);
            operatorStrategyId = "OS";
            ContingencyContext contingencyContext = contingencies.isEmpty() ? ContingencyContext.none() : ContingencyContext.all();
            operatorStrategies.add(new OperatorStrategy(operatorStrategyId, contingencyContext, new TrueCondition(), new ArrayList<>(simulatedActionIds)));
        }

        SensitivityAnalysisParameters runParameters = copy(sensitivityComputationParameters)
                .setOperatorStrategiesCalculationMode(operatorStrategyId != null
                        ? SensitivityOperatorStrategiesCalculationMode.CONTINGENCIES_AND_OPERATOR_STRATEGIES
                        : SensitivityOperatorStrategiesCalculationMode.NONE);

        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();
        instantOrderByState.put(new SensitivityState(null, operatorStrategyId), outageInstant.getOrder());
        for (Contingency contingency : contingencies) {
            instantOrderByState.put(new SensitivityState(contingency.getId(), operatorStrategyId), outageInstant.getOrder());
        }

        return new RunConfig(
                new SensitivityAnalysisRunParameters()
                        .setParameters(runParameters)
                        .setContingencies(contingencies)
                        .setOperatorStrategies(operatorStrategies)
                        .setActions(new ArrayList<>(actions))
                        .setVariableSets(variableSets),
                instantOrderByState);
    }

    private static RunConfig configureWithRemedialActions(List<SensitivityVariableSet> variableSets,
                                                          SensitivityAnalysisParameters sensitivityComputationParameters,
                                                          Set<State> statesWithRa,
                                                          Set<NetworkAction> networkActions,
                                                          AppliedRemedialActions.AppliedRemedialActionsPerState preventiveAppliedRemedialActions,
                                                          AppliedRemedialActions appliedRemedialActions,
                                                          Network network) {
        List<Contingency> contingencies = new ArrayList<>();
        // maximal action pool: all network actions + curative actions per state, used for fast-restart sensitivity
        Set<Action> actions = new LinkedHashSet<>(networkActions.stream().flatMap(na -> na.getElementaryActions().stream()).toList());
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
        Set<String> simulatedActionIds = new LinkedHashSet<>();
        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();

        if (preventiveAppliedRemedialActions != null) {
            addPreventiveActions(actions, simulatedActionIds, preventiveAppliedRemedialActions, network);
        }

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

        SensitivityAnalysisParameters runParameters = copy(sensitivityComputationParameters)
                .setOperatorStrategiesCalculationMode(SensitivityOperatorStrategiesCalculationMode.ONLY_OPERATOR_STRATEGIES);

        return new RunConfig(
                new SensitivityAnalysisRunParameters()
                        .setParameters(runParameters)
                        .setContingencies(contingencies)
                        .setOperatorStrategies(operatorStrategies)
                        .setActions(new ArrayList<>(actions))
                        .setVariableSets(variableSets),
                instantOrderByState);
    }

    /**
     * Deep-copies the given {@link SensitivityAnalysisParameters} (including its extensions) through JSON serialization, in
     * the same way as {@code LoadFlowParameters.copy()} in powsybl-core. The {@link SensitivityAnalysisParameters} instance
     * is typically shared between sensitivity analyses (e.g. MARMOT timestamps) that may run concurrently. Each analysis sets
     * its own operator strategies calculation mode, which OLF reads asynchronously during the run; mutating the shared
     * instance would race on that field and make OLF read another analysis' mode, producing empty/incoherent results. Working
     * on a copy keeps each analysis isolated.
     */
    private static SensitivityAnalysisParameters copy(SensitivityAnalysisParameters parameters) {
        ObjectMapper objectMapper = JsonSensitivityAnalysisParameters.createObjectMapper();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            objectMapper.writeValue(outputStream, parameters);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                return JsonSensitivityAnalysisParameters.read(inputStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void addPreventiveActions(Set<Action> actions, Set<String> simulatedActionIds,
                                             AppliedRemedialActions.AppliedRemedialActionsPerState preventiveApplied,
                                             Network network) {
        // only range actions need to be added explicitly; network actions are already in the global pool
        var preventiveRangeActions = preventiveApplied.getRangeActions().entrySet().stream()
                .flatMap(e -> e.getKey().toActions(e.getValue(), network).stream()).toList();
        actions.addAll(preventiveRangeActions);
        simulatedActionIds.addAll(preventiveApplied.getNetworkActions().stream()
                .flatMap(e -> e.getElementaryActions().stream().map(Action::getId)).toList());
        simulatedActionIds.addAll(preventiveRangeActions.stream().map(Action::getId).toList());
    }

    private static SensitivityAnalysisResult runAnalysis(Network network, List<SensitivityFactor> factors,
                                                         SensitivityAnalysisRunParameters params, String provider) {
        return SensitivityAnalysis.find(provider).run(network, network.getVariantManager().getWorkingVariantId(), factors, params);
    }
}
