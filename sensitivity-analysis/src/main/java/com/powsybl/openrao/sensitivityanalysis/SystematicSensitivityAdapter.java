/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.action.Action;
import com.powsybl.action.AreaInterchangeTargetAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.HvdcAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.RatioTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
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

    /** Bundles the run parameters with their associated instant-order map to avoid output parameters. */
    private record RunConfig(SensitivityAnalysisRunParameters params, Map<SensitivityState, Integer> instantOrderByState) {
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
                copy(sensitivityComputationParameters));
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
        // For contingencies with auto RA but no curative RA, SystematicSensitivityResult::getCnecStateResult
        // retrieves sensi for the curative state from the auto state to account for auto RAs.
        Set<State> statesWithRa = appliedRemedialActions.getStatesWithRa(network);
        Set<State> statesWithoutRa = cnecSensitivityProvider.getFlowCnecs().stream().map(Cnec::getState).collect(Collectors.toSet());
        statesWithoutRa.removeAll(statesWithRa);

        TECHNICAL_LOGS.debug("{} state(s) without RA", statesWithoutRa.size());
        SystematicSensitivityResult result = runWithoutRemedialActions(network, cnecSensitivityProvider,
            sensitivityComputationParameters, sensitivityProvider, outageInstant, statesWithoutRa);
        if (result.getStatus() == SystematicSensitivityResult.SensitivityComputationStatus.FAILURE) {
            return result;
        }

        TECHNICAL_LOGS.debug("{} state(s) with RA", statesWithRa.size());
        cnecSensitivityProvider.disableFactorsForBaseCaseSituation();
        runWithRemedialActions(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
            sensitivityProvider, statesWithRa, appliedRemedialActions);
        cnecSensitivityProvider.enableFactorsForBaseCaseSituation();

        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");
        return result.postTreatIntensities().postTreatHvdcs(network, cnecSensitivityProvider.getHvdcs());
    }

    private static SystematicSensitivityResult runWithoutRemedialActions(Network network,
                                                                         CnecSensitivityProvider cnecSensitivityProvider,
                                                                         SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                         String sensitivityProvider,
                                                                         Instant outageInstant,
                                                                         Set<State> statesWithoutRa) {
        List<Contingency> contingenciesWithoutRa = statesWithoutRa.stream()
            .flatMap(state -> state.getContingency().stream())
            .distinct()
            .toList();
        List<SensitivityFactor> factors = cnecSensitivityProvider.getBasecaseFactors(network);
        factors.addAll(cnecSensitivityProvider.getContingencyFactors(network, contingenciesWithoutRa));
        if (factors.isEmpty()) {
            // Nothing to compute for the states without applied RA (e.g. when the provider only holds CNECs of a single
            // contingency state that has applied RA). Running an empty sensitivity would yield an (empty) FAILURE
            // result and wrongly abort the whole run before the with-RA part.
            return new SystematicSensitivityResult();
        }
        try {
            SensitivityAnalysisResult sensiResult = SensitivityAnalysis.find(sensitivityProvider).run(network,
                network.getVariantManager().getWorkingVariantId(),
                factors,
                contingenciesWithoutRa,
                cnecSensitivityProvider.getVariableSets(),
                copy(sensitivityComputationParameters));
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
                                               AppliedRemedialActions appliedRemedialActions) {
        // Two paths:
        // - fast path: if all the applied remedial actions can be simulated by the sensitivity provider as operator strategies,
        //   a single sensitivity analysis is run for all the states with RA;
        // - fallback: otherwise, the remedial actions are applied on a network variant and one sensitivity analysis is run
        //   per state with RA.
        Optional<Map<State, List<Action>>> actionsPerState = convertToOperatorStrategyActions(statesWithRa, appliedRemedialActions,
            network, sensitivityComputationParameters.getLoadFlowParameters().isDc());
        if (actionsPerState.isPresent()) {
            TECHNICAL_LOGS.debug("Applied remedial actions are simulated as operator strategies in a single sensitivity analysis");
            runWithRemedialActionsAsOperatorStrategies(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, statesWithRa, actionsPerState.get());
        } else {
            TECHNICAL_LOGS.debug("Some applied remedial actions cannot be simulated as operator strategies: running one sensitivity analysis per state with RA");
            runWithRemedialActionsAppliedOnNetwork(result, network, cnecSensitivityProvider, sensitivityComputationParameters,
                sensitivityProvider, statesWithRa, appliedRemedialActions);
        }
    }

    /**
     * Converts the remedial actions applied on each state into powsybl {@link Action}s, provided that ALL of them can be
     * simulated as operator strategies by the sensitivity provider (OpenLoadFlow). Returns an empty optional as soon as a
     * remedial action cannot be converted or yields an unsupported action.
     */
    static Optional<Map<State, List<Action>>> convertToOperatorStrategyActions(Set<State> statesWithRa,
                                                                               AppliedRemedialActions appliedRemedialActions,
                                                                               Network network,
                                                                               boolean dc) {
        Map<State, List<Action>> actionsPerState = new HashMap<>();
        for (State state : statesWithRa) {
            List<Action> actions;
            try {
                actions = appliedRemedialActions.toActions(state, network);
            } catch (OpenRaoException | UnsupportedOperationException e) {
                TECHNICAL_LOGS.debug("Remedial actions applied on state {} cannot be converted to actions: {}", state.getId(), e.getMessage());
                return Optional.empty();
            }
            Optional<Action> unsupportedAction = actions.stream().filter(action -> !isSupportedByOperatorStrategies(action, dc)).findFirst();
            if (unsupportedAction.isPresent()) {
                TECHNICAL_LOGS.debug("Action {} of type {} applied on state {} is not supported by operator strategies in {} mode",
                    unsupportedAction.get().getId(), unsupportedAction.get().getType(), state.getId(), dc ? "DC" : "AC");
                return Optional.empty();
            }
            actionsPerState.put(state, actions);
        }
        return Optional.of(actionsPerState);
    }

    /**
     * Tells whether the given action can be simulated as an operator strategy action by OpenLoadFlow sensitivity analysis:
     * <ul>
     *     <li>in DC mode, the Woodbury engine only supports PST tap, line/switch connection, generator and load actions;</li>
     *     <li>in AC mode, ratio tap changer, shunt compensator, area interchange and HVDC actions (active power setpoint
     *     change and/or AC emulation disabling) are supported too.</li>
     * </ul>
     */
    static boolean isSupportedByOperatorStrategies(Action action, boolean dc) {
        if (action instanceof PhaseTapChangerTapPositionAction || action instanceof TerminalsConnectionAction || action instanceof SwitchAction
            || action instanceof GeneratorAction || action instanceof LoadAction) {
            return true;
        }
        if (dc) {
            return false;
        }
        if (action instanceof HvdcAction hvdcAction) {
            boolean enablesAcEmulation = hvdcAction.isAcEmulationEnabled().orElse(false);
            boolean disablesAcEmulation = hvdcAction.isAcEmulationEnabled().map(enabled -> !enabled).orElse(false);
            // enabling AC emulation is not supported, and an action neither disabling it nor changing the setpoint has no effect
            return !enablesAcEmulation && (disablesAcEmulation || hvdcAction.getActivePowerSetpoint().isPresent());
        }
        return action instanceof RatioTapChangerTapPositionAction || action instanceof ShuntCompensatorPositionAction || action instanceof AreaInterchangeTargetAction;
    }

    private static void runWithRemedialActionsAsOperatorStrategies(SystematicSensitivityResult result,
                                                                   Network network,
                                                                   CnecSensitivityProvider cnecSensitivityProvider,
                                                                   SensitivityAnalysisParameters sensitivityComputationParameters,
                                                                   String sensitivityProvider,
                                                                   Set<State> statesWithRa,
                                                                   Map<State, List<Action>> actionsPerState) {
        RunConfig config = configureWithRemedialActions(cnecSensitivityProvider.getVariableSets(),
            sensitivityComputationParameters, statesWithRa, actionsPerState);
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

    /**
     * Fallback path: the remedial actions of each state are applied on a temporary network variant, and one sensitivity
     * analysis is run per state with RA.
     */
    private static void runWithRemedialActionsAppliedOnNetwork(SystematicSensitivityResult result,
                                                               Network network,
                                                               CnecSensitivityProvider cnecSensitivityProvider,
                                                               SensitivityAnalysisParameters sensitivityComputationParameters,
                                                               String sensitivityProvider,
                                                               Set<State> statesWithRa,
                                                               AppliedRemedialActions appliedRemedialActions) {
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
                        contingencyList,
                        cnecSensitivityProvider.getVariableSets(),
                        copy(sensitivityComputationParameters));
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
                                                          Map<State, List<Action>> actionsPerState) {
        List<Contingency> contingencies = new ArrayList<>();
        Set<Action> actions = new LinkedHashSet<>();
        List<OperatorStrategy> operatorStrategies = new ArrayList<>();
        Map<SensitivityState, Integer> instantOrderByState = new HashMap<>();

        // Group states by contingency: a contingency with several curative perimeters (multi-curative) yields
        // several states sharing the same contingency, which must be declared only ONCE to the sensitivity provider
        // (a duplicated contingency id makes the whole sensitivity analysis fail). Each curative perimeter still gets
        // its own operator strategy, and its actions accumulate from one curative instant to the next within the
        // contingency.
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
            // accumulate the actions applied on the contingency from one instant to the next (auto then curative)
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
                .setVariableSets(variableSets),
            instantOrderByState);
    }

    /**
     * Deep-copies the given {@link SensitivityAnalysisParameters} (including its extensions) through JSON serialization, in
     * the same way as {@code LoadFlowParameters.copy()} in powsybl-core. The {@link SensitivityAnalysisParameters} instance
     * is typically shared between sensitivity analyses (e.g. MARMOT timestamps) that may run concurrently. Each analysis sets
     * its own operator strategies calculation mode, which the provider may read asynchronously during the run; mutating the
     * shared instance would race on that field and make the provider read another analysis' mode, producing empty/incoherent
     * results. Working on a copy keeps each analysis isolated.
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
}
