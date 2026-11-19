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
import com.powsybl.computation.ComputationManager;
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
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.json.JsonSensitivityAnalysisParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    private record RunConfig(SensitivityAnalysisRunParameters params, Map<SensitivityState, Integer> instantOrderByState) {
    }

    static SystematicSensitivityResult runSensitivity(Network network,
                                                      CnecSensitivityProvider cnecSensitivityProvider,
                                                      SensitivityAnalysisParameters sensitivityComputationParameters,
                                                      String sensitivityProvider,
                                                      Instant outageInstant,
                                                      ComputationManager computationManager) {
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        SensitivityAnalysisResult result;
        try {
            final List<SensitivityFactor> allFactors = cnecSensitivityProvider.getAllFactors(network); // This must be done before getting variable sets
            final SensitivityAnalysisRunParameters sensitivityAnalysisRunParameters = new SensitivityAnalysisRunParameters()
                .setContingencies(cnecSensitivityProvider.getContingencies(network))
                .setVariableSets(cnecSensitivityProvider.getVariableSets())
                .setParameters(copy(sensitivityComputationParameters))
                .setComputationManager(computationManager);
            result = SensitivityAnalysis.find(sensitivityProvider).run(
                network,
                network.getVariantManager().getWorkingVariantId(),
                allFactors,
                sensitivityAnalysisRunParameters
            );
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
                                                      Instant outageInstant,
                                                      ComputationManager computationManager) {
        if (appliedRemedialActions == null || appliedRemedialActions.isEmpty(network)) {
            return runSensitivity(network, cnecSensitivityProvider, sensitivityComputationParameters, sensitivityProvider, outageInstant, computationManager);
        }

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [start]");
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa(network);
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        TECHNICAL_LOGS.debug("{} state(s) without RA", statesWithoutRa.size());
        SystematicSensitivityResult result = runWithoutRemedialActions(network, cnecSensitivityProvider,
            sensitivityComputationParameters, sensitivityProvider, outageInstant, statesWithoutRa, computationManager);
        if (result.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FAILURE) {
            return result;
        }

        TECHNICAL_LOGS.debug("{} state(s) with RA", statesWithRa.size());
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        runWithRemedialActions(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
            sensitivityProvider, statesWithRa, appliedRemedialActions, computationManager);
        cnecSensitivityProvider.enableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    private static SystematicSensitivityResult runWithoutRemedialActions(Network network,
                                                                         CnecSensitivityProvider cnecSensitivityProvider,
                                                                         SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                         String sensitivityProvider,
                                                                         Instant outageInstant,
                                                                         Set<State> statesWithoutRa,
                                                                         ComputationManager computationManager) {
        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .flatMap(state -> state.getContingency().stream())
            .distinct()
            .toList();
        List<SensitivityFactor> factors = cnecSensitivityProvider.getBasecaseFactors(network);
        factors.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        if (factors.isEmpty()) {
            // an empty run would be reported as a FAILURE
            return new SystematicSensitivityResult();
        }
        try {
            SensitivityAnalysisResult sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                factors,
                new SensitivityAnalysisRunParameters()
                    .setContingencies(contingenciesWithoutRa)
                    .setVariableSets(cnecSensitivityProvider.getVariableSets())
                    .setParameters(copy(sensitivityComputationParameters))
                    .setComputationManager(computationManager));
            return new SystematicSensitivityResult().completeData(sensiResult, outageInstant.getOrder());
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
                                               AppliedRemedialActions appliedRemedialActions,
                                               ComputationManager computationManager) {
        // operator strategies of a contingency accumulate the actions of its successive states, so a contingency is
        // simulated with operator strategies only if the actions of all its states with RA are supported
        OperatorStrategySupport.Mode mode = OperatorStrategySupport.Mode.of(sensitivityComputationParameters);
        Map<State, List<Action>> actionsPerState = new HashMap<>();
        Set<Optional<Contingency>> unsupportedContingencies = new HashSet<>();
        for (State state : statesWithRa) {
            convertToOperatorStrategyActions(state, appliedRemedialActions, network, mode).ifPresentOrElse(
                actions -> actionsPerState.put(state, actions),
                () -> unsupportedContingencies.add(state.getContingency()));
        }
        Map<Boolean, Set<State>> statesBySupport = statesWithRa.stream()
            .collect(Collectors.partitioningBy(state -> !unsupportedContingencies.contains(state.getContingency()), Collectors.toSet()));
        Set<State> statesAsOperatorStrategies = statesBySupport.get(true);
        Set<State> statesAppliedOnNetwork = statesBySupport.get(false);

        if (!statesAsOperatorStrategies.isEmpty()) {
            TECHNICAL_LOGS.debug("{} state(s) with RA simulated as operator strategies in a single sensitivity analysis", statesAsOperatorStrategies.size());
            runWithRemedialActionsAsOperatorStrategies(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, statesAsOperatorStrategies, actionsPerState, computationManager);
        }
        if (!statesAppliedOnNetwork.isEmpty()) {
            TECHNICAL_LOGS.debug("{} state(s) with RA not supported by operator strategies: running one sensitivity analysis per state", statesAppliedOnNetwork.size());
            runWithRemedialActionsAppliedOnNetwork(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, statesAppliedOnNetwork, appliedRemedialActions, computationManager);
        }
    }

    /**
     * Empty if a remedial action applied on the state is not supported by operator strategies.
     */
    static Optional<List<Action>> convertToOperatorStrategyActions(State state,
                                                                   AppliedRemedialActions appliedRemedialActions,
                                                                   Network network,
                                                                   OperatorStrategySupport.Mode mode) {
        List<Action> actions;
        try {
            actions = appliedRemedialActions.toActions(state, network);
        } catch (OpenRaoException | UnsupportedOperationException e) {
            TECHNICAL_LOGS.debug("Remedial actions applied on state {} cannot be converted to actions: {}", state.getId(), e.getMessage());
            return Optional.empty();
        }
        Optional<Action> unsupportedAction = actions.stream().filter(action -> !OperatorStrategySupport.isSupported(action, network, mode)).findFirst();
        if (unsupportedAction.isPresent()) {
            TECHNICAL_LOGS.debug("Action {} of type {} applied on state {} is not supported by operator strategies in {} mode",
                unsupportedAction.get().getId(), unsupportedAction.get().getType(), state.getId(), mode);
            return Optional.empty();
        }
        return Optional.of(actions);
    }

    private static void runWithRemedialActionsAsOperatorStrategies(SystematicSensitivityResult result,
                                                                   Network network,
                                                                   CnecSensitivityProvider cnecSensitivityProvider,
                                                                   SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                   String sensitivityProvider,
                                                                   Set<State> statesWithRa,
                                                                   Map<State, List<Action>> actionsPerState,
                                                                   ComputationManager computationManager) {
        RunConfig config = configureWithRemedialActions(cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters, statesWithRa, actionsPerState, computationManager);
        List<SensitivityFactor> factors = cnecSensitivityProvider.getContingencyFactors(network, config.params().getContingencies());
        try {
            SensitivityAnalysisResult sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(), factors, config.params());
            result.completeData(sensiResult, config.instantOrderByState());
        } catch (PowsyblException | OpenRaoException | CompletionException e) {
            TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis with RA failed: %s", e.getMessage()));
            SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                factors,
                config.instantOrderByState().keySet().stream()
                    .map(state -> new SensitivityAnalysisResult.SensitivityStateStatus(state, SensitivityAnalysisResult.Status.FAILURE))
                    .toList(),
                config.params().getContingencies().stream().map(Contingency::getId).toList(),
                config.params().getOperatorStrategies().stream().map(OperatorStrategy::getId).toList(),
                List.of()
            );
            result.completeData(failedResult, config.instantOrderByState());
        }
    }

    private static void runWithRemedialActionsAppliedOnNetwork(SystematicSensitivityResult result,
                                                               Network network,
                                                               CnecSensitivityProvider cnecSensitivityProvider,
                                                               SensitivityAnalysisParameters sensitivityComputationParameters,
                                                               String sensitivityProvider,
                                                               Set<State> statesWithRa,
                                                               AppliedRemedialActions appliedRemedialActions,
                                                               ComputationManager computationManager) {
        String workingVariantId = network.getVariantManager().getWorkingVariantId();
        String variantForState = RandomizedString.getRandomizedString();
        int counterForLogs = 1;
        try {
            for (State state : statesWithRa) {
                Contingency contingency = state.getContingency().orElseThrow(() ->
                    new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.")
                );
                TECHNICAL_LOGS.debug("... ({}/{}) state with RA {}", counterForLogs, statesWithRa.size(), state.getId());

                network.getVariantManager().cloneVariant(workingVariantId, variantForState, true);
                network.getVariantManager().setWorkingVariant(variantForState);
                appliedRemedialActions.applyOnNetwork(state, network);

                List<Contingency> contingencyList = Collections.singletonList(contingency);
                List<SensitivityFactor> factors = cnecSensitivityProvider.getContingencyFactors(network, contingencyList);
                try {
                    SensitivityAnalysisResult sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                        variantForState,
                        factors,
                        new SensitivityAnalysisRunParameters()
                            .setContingencies(contingencyList)
                            .setVariableSets(cnecSensitivityProvider.getVariableSets())
                            .setParameters(copy(sensitivityComputationParameters))
                            .setComputationManager(computationManager));
                    result.completeData(sensiResult, state.getInstant().getOrder());
                } catch (PowsyblException | OpenRaoException | CompletionException e) {
                    TECHNICAL_LOGS.error(String.format("Systematic sensitivity analysis failed for state %s : %s", state.getId(), e.getMessage()));
                    SensitivityAnalysisResult failedResult = new SensitivityAnalysisResult(
                        factors,
                        List.of(new SensitivityAnalysisResult.SensitivityStateStatus(SensitivityState.postContingency(contingency.getId()), SensitivityAnalysisResult.Status.FAILURE)),
                        contingencyList.stream().map(Contingency::getId).toList(),
                        List.of(),
                        List.of()
                    );
                    result.completeData(failedResult, state.getInstant().getOrder());
                }
                counterForLogs++;
            }
        } finally {
            network.getVariantManager().setWorkingVariant(workingVariantId);
            if (network.getVariantManager().getVariantIds().contains(variantForState)) {
                network.getVariantManager().removeVariant(variantForState);
            }
        }
    }

    private static RunConfig configureWithRemedialActions(List<SensitivityVariableSet> variableSets,
                                                          SensitivityAnalysisParameters sensitivityComputationParameters,
                                                          Set<State> statesWithRa,
                                                          Map<State, List<Action>> actionsPerState,
                                                          ComputationManager computationManager) {
        List<Contingency> contingencies = new ArrayList<>();
        Set<Action> actions = new LinkedHashSet<>();
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();

        // multi-curative: one contingency, one operator strategy per state, with the actions of the previous instants
        Map<Contingency, List<State>> statesByContingency = new LinkedHashMap<>();
        for (State state : statesWithRa) {
            Contingency contingency = state.getContingency().orElseThrow(() ->
                new OpenRaoException("Sensitivity analysis with applied RA does not handle preventive RA.")
            );
            statesByContingency.computeIfAbsent(contingency, c -> new ArrayList<>()).add(state);
        }

        for (Map.Entry<Contingency, List<State>> entry : statesByContingency.entrySet()) {
            Contingency contingency = entry.getKey();
            contingencies.add(contingency);
            Set<String> contingencyActionIds = new LinkedHashSet<>();
            List<State> orderedStates = entry.getValue().stream()
                .sorted(Comparator.comparingInt(state -> state.getInstant().getOrder()))
                .toList();
            for (State state : orderedStates) {
                List<Action> actionsForState = actionsPerState.getOrDefault(state, Collections.emptyList());
                actions.addAll(actionsForState);
                contingencyActionIds.addAll(actionsForState.stream().map(Action::getId).toList());
                String operatorStrategyId = "OS-" + contingency.getId() + "-" + state.getInstant().getOrder();
                operatorStrategies.add(new OperatorStrategy(operatorStrategyId,
                    ContingencyContext.specificContingency(contingency.getId()),
                    new TrueCondition(),
                    new ArrayList<>(contingencyActionIds)));
                instantOrderByState.put(new SensitivityState(contingency.getId(), operatorStrategyId), state.getInstant().getOrder());
            }
        }

        SensitivityAnalysisParameters runParameters = copy(sensitivityComputationParameters)
            .setOperatorStrategiesCalculationMode(SensitivityOperatorStrategiesCalculationMode.ONLY_OPERATOR_STRATEGIES);

        return new RunConfig(
            new SensitivityAnalysisRunParameters()
                .setParameters(runParameters)
                .setContingencies(contingencies)
                .setOperatorStrategies(operatorStrategies)
                .setActions(new ArrayList<>(actions))
                .setVariableSets(variableSets)
                .setComputationManager(computationManager),
            instantOrderByState);
    }

    /**
     * Deep copy (extensions included): the parameters are shared by concurrent analyses, each one setting its own
     * operator strategies calculation mode.
     */
    static SensitivityAnalysisParameters copy(SensitivityAnalysisParameters parameters) {
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
}
