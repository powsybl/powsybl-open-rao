/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.opentelemetry.OpenTelemetryReporter;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.util.AbstractNetworkPool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class CastorPstRegulation {
    private CastorPstRegulation() {
    }

    public static Set<PstRegulationResult> regulatePsts(Map<String, String> pstsToRegulate, Map<State, PostPerimeterResult> postContingencyResults, Network network, Crac crac, RaoParameters raoParameters, RaoResult raoResult) {
        // filter out non-curative PSTs
        // currently, only PSTs with a usage rule for a given state are regulated
        Set<PstRangeAction> rangeActionsToRegulate = getPstRangeActionsForRegulation(
            pstsToRegulate.keySet(), crac);
        if (rangeActionsToRegulate.isEmpty()) {
            return Set.of();
        }

        Set<PstRegulationInput> statesToRegulate = getStatesToRegulate(crac, postContingencyResults,
            getFlowUnit(raoParameters), rangeActionsToRegulate,
            SearchTreeRaoPstRegulationParameters.getPstsToRegulate(raoParameters), network);
        if (statesToRegulate.isEmpty()) {
            return Set.of();
        }

        Set<Contingency> contingencies = statesToRegulate.stream()
            .map(PstRegulationInput::curativeState).map(State::getContingency)
            .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        BUSINESS_LOGS.info("{} contingency scenario(s) to regulate: {}", contingencies.size(),
            String.join(", ", contingencies.stream()
                .map(contingency -> contingency.getName().orElse(contingency.getId())).sorted()
                .toList()));
        BUSINESS_LOGS.info("{} PST(s) to regulate: {}", rangeActionsToRegulate.size(),
            String.join(", ",
                rangeActionsToRegulate.stream().map(PstRangeAction::getName).sorted().toList()));

        // update loadflow parameters
        LoadFlowParameters loadFlowParameters = getLoadFlowParameters(raoParameters);
        boolean initialPhaseShifterRegulationOnValue = loadFlowParameters.isPhaseShifterRegulationOn();
        updateLoadFlowParametersForPstRegulation(loadFlowParameters);

        // apply optimal preventive remedial actions
        applyOptimalRemedialActionsForState(network, raoResult, crac.getPreventiveState());

        // regulate PSTs for each curative scenario in parallel
        return OpenTelemetryReporter.withSpan("rao.regulatePstsParallel", cx -> {
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network,
            network.getVariantManager().getWorkingVariantId(),
            getNumberOfThreads(crac, raoParameters), true)) {
            List<ForkJoinTask<PstRegulationResult>> tasks = statesToRegulate.stream()
                .map(pstRegulationInput ->
                    networkPool.submit(
                        () -> regulatePstsForContingencyScenario(pstRegulationInput, crac,
                            rangeActionsToRegulate, raoResult, loadFlowParameters, networkPool))
                ).toList();
            Set<PstRegulationResult> pstRegulationResults = new HashSet<>();
            for (ForkJoinTask<PstRegulationResult> task : tasks) {
                try {
                    pstRegulationResults.add(task.get());
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(1000, TimeUnit.SECONDS);
            return pstRegulationResults;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            BUSINESS_WARNS.warn(
                "An error occurred during PST regulation, pre-regulation RAO result will be kept.");
            return Set.of();
        } finally {
            loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
        }
        });
    }

    /**
     * Determines the states for which PST regulation must be applied. There are two conditions:
     * <ol>
     *     <li>the state must be unsecure;</li>
     *     <li>the limiting elements must be in series with a regulated PST.</li>
     * </ol>
     * For all such states, the associated PST regulation input is included in a set that is returned.
     */
    private static Set<PstRegulationInput> getStatesToRegulate(Crac crac, Map<State, PostPerimeterResult> postContingencyResults, Unit unit, Set<PstRangeAction> rangeActionsToRegulate, Map<String, String> linesInSeriesWithPst, Network network) {
        Instant lastInstant = crac.getLastInstant();
        return lastInstant.isCurative() ?
            crac.getStates(lastInstant).stream()
                .filter(postContingencyResults::containsKey)
                .filter(curativeState -> !(postContingencyResults.get(curativeState).optimizationResult() instanceof SkippedOptimizationResultImpl))
                .map(curativeState -> getPstRegulationInput(curativeState, crac, postContingencyResults.get(curativeState), unit, rangeActionsToRegulate, linesInSeriesWithPst, network))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet())
            : Set.of();
    }

    private static Optional<PstRegulationInput> getPstRegulationInput(State curativeState, Crac crac, PostPerimeterResult postPerimeterResult, Unit unit, Set<PstRangeAction> rangeActionsToRegulate, Map<String, String> linesInSeriesWithPst, Network network) {
        Optional<FlowCnec> limitingElement = getMostLimitingElementProtectedByPst(curativeState, crac, postPerimeterResult, unit, new HashSet<>(linesInSeriesWithPst.values()));
        if (limitingElement.isPresent()) {
            Set<ElementaryPstRegulationInput> elementaryPstRegulationInputs = rangeActionsToRegulate.stream()
                .filter(pstRangeAction -> linesInSeriesWithPst.containsKey(pstRangeAction.getNetworkElement().getId()))
                .map(pstRangeAction -> ElementaryPstRegulationInput.of(pstRangeAction, linesInSeriesWithPst.get(pstRangeAction.getNetworkElement().getId()), curativeState, crac, network))
                .collect(Collectors.toSet());
            return Optional.of(new PstRegulationInput(curativeState, limitingElement.get(), elementaryPstRegulationInputs));
        }
        return Optional.empty();
    }

    /**
     * If the most limiting element of a curative state is overloaded and is in series with a PST, it is returned.
     * If not, an empty optional value is returned instead.
     */
    private static Optional<FlowCnec> getMostLimitingElementProtectedByPst(State curativeState, Crac crac, PostPerimeterResult postPerimeterResult, Unit unit, Set<String> linesInSeriesWithPst) {
        Map<FlowCnec, Double> marginPerCnec = crac.getFlowCnecs(curativeState).stream().collect(Collectors.toMap(Function.identity(), flowCnec -> postPerimeterResult.optimizationResult().getMargin(flowCnec, unit)));
        List<Map.Entry<FlowCnec, Double>> sortedNegativeMargins = marginPerCnec.entrySet().stream()
            .filter(entry -> entry.getValue() < 0)
            .sorted(Map.Entry.comparingByValue()).toList();
        if (sortedNegativeMargins.isEmpty()) {
            return Optional.empty();
        }
        double minMargin = sortedNegativeMargins.get(0).getValue();
        Set<FlowCnec> limitingElements = new HashSet<>();
        sortedNegativeMargins.stream().filter(entry -> entry.getValue() == minMargin).forEach(entry -> limitingElements.add(entry.getKey()));
        return limitingElements.stream()
            .filter(flowCnec -> linesInSeriesWithPst.contains(flowCnec.getNetworkElement().getId()))
            .min(Comparator.comparing(Identifiable::getId));
    }

    private static LoadFlowParameters getLoadFlowParameters(RaoParameters raoParameters) {
        return raoParameters.hasExtension(OpenRaoSearchTreeParameters.class) ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters() : new LoadFlowParameters();
    }

    private static void updateLoadFlowParametersForPstRegulation(LoadFlowParameters loadFlowParameters) {
        loadFlowParameters.setPhaseShifterRegulationOn(true);
        if (loadFlowParameters.getExtension(OpenLoadFlowParameters.class) == null) {
            loadFlowParameters.addExtension(OpenLoadFlowParameters.class, new OpenLoadFlowParameters());
        }
        loadFlowParameters.getExtension(OpenLoadFlowParameters.class).setMaxOuterLoopIterations(1000);
    }

    private static void applyOptimalRemedialActionsForState(Network networkClone, RaoResult raoResult, State state) {
        // network actions need to be applied BEFORE range actions because to apply HVDC range actions we need to apply AC emulation deactivation network actions beforehand
        raoResult.getActivatedNetworkActionsDuringState(state).forEach(networkAction -> networkAction.apply(networkClone));
        raoResult.getActivatedRangeActionsDuringState(state).forEach(rangeAction -> rangeAction.apply(networkClone, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
    }

    private static Set<PstRangeAction> getPstRangeActionsForRegulation(Set<String> pstsToRegulate, Crac crac) {
        Map<String, PstRangeAction> rangeActionPerPst = getRangeActionPerPst(pstsToRegulate, crac);
        Set<PstRangeAction> rangeActionsToRegulate = new HashSet<>();
        for (String pstId : pstsToRegulate) {
            if (rangeActionPerPst.containsKey(pstId)) {
                rangeActionsToRegulate.add(rangeActionPerPst.get(pstId));
            } else {
                BUSINESS_LOGS.info("PST {} cannot be regulated as no curative PST range action was defined for it.", pstId);
            }
        }
        return rangeActionsToRegulate;
    }

    private static Map<String, PstRangeAction> getRangeActionPerPst(Set<String> pstsToRegulate, Crac crac) {
        // filter out crac's last instant range actions, as results reporting would be less relevant
        return crac.getPstRangeActions().stream()
            .filter(pstRangeAction -> pstRangeAction.getUsageRules().stream().anyMatch(usageRule -> usageRule.getInstant() == crac.getLastInstant()))
            .filter(pstRangeAction -> pstsToRegulate.contains(pstRangeAction.getNetworkElement().getId()))
            .collect(Collectors.toMap(pstRangeAction -> pstRangeAction.getNetworkElement().getId(), Function.identity()));
    }

    private static int getNumberOfThreads(Crac crac, RaoParameters raoParameters) {
        return Math.min(getAvailableCPUs(raoParameters), crac.getContingencies().size());
    }

    /**
     * Performs PST regulation for a curative state. The taps are changed during the loadflow iterations.
     */
    private static PstRegulationResult regulatePstsForContingencyScenario(PstRegulationInput pstRegulationInput, Crac crac, Set<PstRangeAction> rangeActionsToRegulate, RaoResult raoResult, LoadFlowParameters loadFlowParameters, AbstractNetworkPool networkPool) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork();
        Contingency contingency = pstRegulationInput.curativeState().getContingency().orElseThrow();
        simulateContingencyAndApplyCurativeActions(contingency, networkClone, crac, raoResult);
        Map<PstRangeAction, Integer> initialTapPerPst = getInitialTapPerPst(rangeActionsToRegulate, networkClone);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(pstRegulationInput.elementaryPstRegulationInputs(), networkClone, loadFlowParameters);
        logPstRegulationResultsForContingencyScenario(contingency, initialTapPerPst, regulatedTapPerPst, pstRegulationInput.limitingElement());
        networkPool.releaseUsedNetwork(networkClone);
        return new PstRegulationResult(contingency, regulatedTapPerPst);
    }

    private static void simulateContingencyAndApplyCurativeActions(Contingency contingency, Network networkClone, Crac crac, RaoResult raoResult) {
        // simulate contingency
        contingency.toModification().apply(networkClone);

        // apply optimal automatons and curative remedial actions
        crac.getStates(contingency).stream()
            .filter(state -> !state.getInstant().isOutage())
            .forEach(state -> applyOptimalRemedialActionsForState(networkClone, raoResult, state));
    }

    private static Map<PstRangeAction, Integer> getInitialTapPerPst(Set<PstRangeAction> rangeActionsToRegulate, Network networkClone) {
        return rangeActionsToRegulate.stream().collect(Collectors.toMap(Function.identity(), pstRangeAction -> networkClone.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger().getTapPosition()));
    }

    private static void logPstRegulationResultsForContingencyScenario(Contingency contingency,
                                                                      Map<PstRangeAction, Integer> initialTapPerPst,
                                                                      Map<PstRangeAction, Integer> regulatedTapPerPst,
                                                                      FlowCnec mostLimitingElement) {
        List<PstRangeAction> sortedPstRangeActions = initialTapPerPst.keySet().stream().sorted(Comparator.comparing(PstRangeAction::getId)).toList();
        List<String> shiftDetails = new ArrayList<>();
        sortedPstRangeActions.forEach(
            pstRangeAction -> {
                int initialTap = initialTapPerPst.get(pstRangeAction);
                int regulatedTap = regulatedTapPerPst.get(pstRangeAction);
                if (initialTap != regulatedTap) {
                    shiftDetails.add("%s (%s -> %s)".formatted(pstRangeAction.getName(), initialTap, regulatedTap));
                }
            }
        );
        String allShiftedPstsDetails = shiftDetails.isEmpty() ? "no PST shifted" : String.join(", ", shiftDetails);
        if (!shiftDetails.isEmpty()) {
            BUSINESS_LOGS.info("FlowCNEC '{}' of contingency scenario '{}' is overloaded and is the most limiting element, PST regulation has been triggered: {}", mostLimitingElement.getId(), contingency.getName().orElse(contingency.getId()), allShiftedPstsDetails);
        }
    }
}
