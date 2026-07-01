/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.pstregulation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PostPerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.PrePerimeterSensitivityAnalysis;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.pstregulation.reports.PstRegulationReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulation {
    private static final String PST_REGULATION_VARIANT = "PSTRegulation";

    private PstRegulation() {
    }

    public static RaoResult regulatePsts(final Network network,
                                         final Crac crac,
                                         final RaoResult raoResult,
                                         final RaoParameters raoParameters,
                                         final ReportNode reportNode) {
        final ReportNode pstRegulationReportNode = PstRegulationReports.reportPstRegulation(reportNode);
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        Set<String> initialVariants = new HashSet<>(network.getVariantManager().getVariantIds());

        network.getVariantManager().cloneVariant(initialVariantId, PST_REGULATION_VARIANT);
        network.getVariantManager().setWorkingVariant(PST_REGULATION_VARIANT);

        Map<String, String> pstsToRegulate = SearchTreeRaoPstRegulationParameters.getPstsToRegulate(raoParameters);
        if (pstsToRegulate.isEmpty()) {
            return raoResult;
        }

        Set<PstRangeAction> rangeActionsToRegulate = getPstRangeActionsForRegulation(pstsToRegulate.keySet(), crac, pstRegulationReportNode);
        if (rangeActionsToRegulate.isEmpty()) {
            return raoResult;
        }

        Set<PstRegulationInput> statesToRegulate = getStatesToRegulate(crac, raoResult, getFlowUnit(raoParameters), rangeActionsToRegulate, SearchTreeRaoPstRegulationParameters.getPstsToRegulate(raoParameters), network);
        if (statesToRegulate.isEmpty()) {
            return raoResult;
        }

        Set<Contingency> contingencies = statesToRegulate.stream()
            .map(PstRegulationInput::curativeState)
            .map(State::getContingency)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        PstRegulationReports.reportContingencyScenariosToRegulate(pstRegulationReportNode, contingencies);
        PstRegulationReports.reportPstsToRegulate(pstRegulationReportNode, rangeActionsToRegulate);

        // update loadflow parameters
        LoadFlowParameters loadFlowParameters = getLoadFlowParameters(raoParameters);
        boolean initialPhaseShifterRegulationOnValue = loadFlowParameters.isPhaseShifterRegulationOn();
        updateLoadFlowParametersForPstRegulation(loadFlowParameters);

        // apply optimal preventive remedial actions
        applyOptimalRemedialActionsForState(network, raoResult, crac.getPreventiveState());

        // regulate PSTs for each curative scenario in parallel
        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), getNumberOfThreads(crac, raoParameters), true)) {
            List<ForkJoinTask<PstRegulationResult>> tasks = statesToRegulate.stream()
                .map(pstRegulationInput -> networkPool.submit(
                    () -> regulatePstsForContingencyScenario(pstRegulationInput, crac, rangeActionsToRegulate, raoResult, loadFlowParameters, networkPool, pstRegulationReportNode)
                ))
                .toList();
            Set<PstRegulationResult> pstRegulationResults = new HashSet<>();
            for (ForkJoinTask<PstRegulationResult> task : tasks) {
                try {
                    pstRegulationResults.add(task.get());
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(1000, TimeUnit.SECONDS);
            network.getVariantManager().setWorkingVariant(initialVariantId);
            return mergePstRegulationResultsWithRaoResult(pstRegulationResults, raoResult, network, crac, raoParameters, pstRegulationReportNode);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            PstRegulationReports.reportErrorDuringPstRegulation(pstRegulationReportNode, e.getMessage());
            return raoResult;
        } finally {
            loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
            network.getVariantManager().setWorkingVariant(initialVariantId);
            Set<String> variantsToRemove = network.getVariantManager().getVariantIds()
                .stream()
                .filter(variantId -> !initialVariants.contains(variantId))
                .collect(Collectors.toSet());
            variantsToRemove.forEach(network.getVariantManager()::removeVariant);
            PstRegulationReports.reportPstRegulationEnd();
        }
    }

    private static Unit getFlowUnit(RaoParameters raoParameters) {
        OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (searchTreeParameters != null) {
            return searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters().isDc()
                ? Unit.MEGAWATT
                : Unit.AMPERE;
        }
        return Unit.AMPERE;
    }

    /**
     * Determines the states for which PST regulation must be applied. There are two conditions:
     * <ol>
     *     <li>the state must be unsecure;</li>
     *     <li>the limiting elements must be in series with a regulated PST.</li>
     * </ol>
     * For all such states, the associated PST regulation input is included in a set that is returned.
     */
    private static Set<PstRegulationInput> getStatesToRegulate(Crac crac,
                                                               RaoResult raoResult,
                                                               Unit unit,
                                                               Set<PstRangeAction> rangeActionsToRegulate,
                                                               Map<String, String> linesInSeriesWithPst,
                                                               Network network) {
        Instant lastInstant = crac.getLastInstant();
        return lastInstant.isCurative()
            ? crac.getStates(lastInstant).stream()
                .filter(state -> raoResult.getComputationStatus(state) != ComputationStatus.FAILURE)
                .map(curativeState -> getPstRegulationInput(curativeState, crac, raoResult, unit, rangeActionsToRegulate, linesInSeriesWithPst, network))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet())
            : Set.of();
    }

    private static Optional<PstRegulationInput> getPstRegulationInput(State curativeState,
                                                                      Crac crac,
                                                                      RaoResult raoResult,
                                                                      Unit unit,
                                                                      Set<PstRangeAction> rangeActionsToRegulate,
                                                                      Map<String, String> linesInSeriesWithPst,
                                                                      Network network) {
        Optional<FlowCnec> limitingElement = getMostLimitingElementProtectedByPst(
            curativeState, crac, raoResult, unit, new HashSet<>(linesInSeriesWithPst.values())
        );
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
    private static Optional<FlowCnec> getMostLimitingElementProtectedByPst(State curativeState,
                                                                           Crac crac,
                                                                           RaoResult raoResult,
                                                                           Unit unit,
                                                                           Set<String> linesInSeriesWithPst) {
        Map<FlowCnec, Double> marginPerCnec = crac.getFlowCnecs(curativeState).stream()
            .collect(Collectors.toMap(
                Function.identity(),
                flowCnec -> raoResult.getMargin(curativeState.getInstant(), flowCnec, unit)
            ));
        List<Map.Entry<FlowCnec, Double>> sortedNegativeMargins = marginPerCnec.entrySet().stream()
            .filter(entry -> entry.getValue() < 0)
            .sorted(Map.Entry.comparingByValue())
            .toList();
        if (sortedNegativeMargins.isEmpty()) {
            return Optional.empty();
        }
        double minMargin = sortedNegativeMargins.getFirst().getValue();
        Set<FlowCnec> limitingElements = new HashSet<>();
        sortedNegativeMargins.stream()
            .filter(entry -> entry.getValue() == minMargin)
            .forEach(entry -> limitingElements.add(entry.getKey()));
        return limitingElements.stream()
            .filter(flowCnec -> linesInSeriesWithPst.contains(flowCnec.getNetworkElement().getId()))
            .min(Comparator.comparing(Identifiable::getId));
    }

    private static LoadFlowParameters getLoadFlowParameters(RaoParameters raoParameters) {
        return raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
            ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters().getLoadFlowParameters()
            : new LoadFlowParameters();
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
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(networkAction -> networkAction.apply(networkClone));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(rangeAction -> rangeAction.apply(
                networkClone, raoResult.getOptimizedSetPointOnState(state, rangeAction)
            ));
    }

    private static Set<PstRangeAction> getPstRangeActionsForRegulation(final Set<String> pstsToRegulate,
                                                                       final Crac crac,
                                                                       final ReportNode reportNode) {
        Map<String, PstRangeAction> rangeActionPerPst = getRangeActionPerPst(pstsToRegulate, crac);
        Set<PstRangeAction> rangeActionsToRegulate = new HashSet<>();
        for (String pstId : pstsToRegulate) {
            if (rangeActionPerPst.containsKey(pstId)) {
                rangeActionsToRegulate.add(rangeActionPerPst.get(pstId));
            } else {
                PstRegulationReports.reportPstCannotBeRegulated(reportNode, pstId);
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
    private static PstRegulationResult regulatePstsForContingencyScenario(final PstRegulationInput pstRegulationInput,
                                                                          final Crac crac,
                                                                          final Set<PstRangeAction> rangeActionsToRegulate,
                                                                          final RaoResult raoResult,
                                                                          final LoadFlowParameters loadFlowParameters,
                                                                          final AbstractNetworkPool networkPool,
                                                                          final ReportNode reportNode) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork();
        Contingency contingency = pstRegulationInput.curativeState().getContingency().orElseThrow();
        simulateContingencyAndApplyCurativeActions(contingency, networkClone, crac, raoResult);
        Map<PstRangeAction, Integer> initialTapPerPst = getInitialTapPerPst(rangeActionsToRegulate, networkClone);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(pstRegulationInput.elementaryPstRegulationInputs(), networkClone, loadFlowParameters, reportNode);
        logPstRegulationResultsForContingencyScenario(contingency, initialTapPerPst, regulatedTapPerPst, pstRegulationInput.limitingElement(), reportNode);
        networkPool.releaseUsedNetwork(networkClone);
        return new PstRegulationResult(contingency, regulatedTapPerPst);
    }

    private static void simulateContingencyAndApplyCurativeActions(Contingency contingency,
                                                                   Network networkClone,
                                                                   Crac crac,
                                                                   RaoResult raoResult) {
        // simulate contingency
        contingency.toModification().apply(networkClone);

        // apply optimal automatons and curative remedial actions
        crac.getStates(contingency).stream()
            .filter(state -> !state.getInstant().isOutage())
            .forEach(state -> applyOptimalRemedialActionsForState(networkClone, raoResult, state));
    }

    private static Map<PstRangeAction, Integer> getInitialTapPerPst(Set<PstRangeAction> rangeActionsToRegulate,
                                                                    Network networkClone) {
        return rangeActionsToRegulate.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                pstRangeAction -> networkClone.getTwoWindingsTransformer(pstRangeAction.getNetworkElement().getId()).getPhaseTapChanger().getTapPosition()
            ));
    }

    private static void logPstRegulationResultsForContingencyScenario(final Contingency contingency,
                                                                      final Map<PstRangeAction, Integer> initialTapPerPst,
                                                                      final Map<PstRangeAction, Integer> regulatedTapPerPst,
                                                                      final FlowCnec mostLimitingElement,
                                                                      final ReportNode reportNode) {
        List<PstRangeAction> sortedPstRangeActions = initialTapPerPst.keySet().stream()
            .sorted(Comparator.comparing(PstRangeAction::getId))
            .toList();
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
            PstRegulationReports.reportPstRegulationTriggeredDueToOverloadedFlowCnec(
                reportNode, mostLimitingElement.getId(), contingency.getName().orElse(contingency.getId()), allShiftedPstsDetails
            );
        }
    }

    private static RaoResult mergePstRegulationResultsWithRaoResult(final Set<PstRegulationResult> pstRegulationResults,
                                                                    final RaoResult raoResult,
                                                                    final Network network,
                                                                    final Crac crac,
                                                                    final RaoParameters raoParameters,
                                                                    final ReportNode reportNode) {
        final Map<State, PstRegulationResult> resultsPerState = pstRegulationResults.stream()
            .collect(Collectors.toMap(
                pstRegulationResult -> crac.getState(pstRegulationResult.contingency(), crac.getLastInstant()),
                Function.identity()
        ));

        final ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(
            RaoInput.build(network, crac).build(), raoParameters
        );
        final PrePerimeterSensitivityAnalysis initialPrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        );
        final PrePerimeterResult initialFlowResult = initialPrePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);

        // create a new network variant from initial variant for performing the results merging
        final String variantName = "PSTRegulationResultsMerging";
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), variantName);
        network.getVariantManager().setWorkingVariant(variantName);

        // apply PRAs
        final State preventiveState = crac.getPreventiveState();
        applyOptimalRemedialActionsForState(network, raoResult, preventiveState);

        // this result is only used as a data holder for flows: it does not contain the proper objective function value in costly
        final PrePerimeterResult preventivePrePerimeterResult = initialPrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
            network, initialFlowResult, Set.of(), new AppliedRemedialActions(), reportNode
        );

        RangeActionActivationResultImpl preventiveRangeActionActivationResult = new RangeActionActivationResultImpl(initialFlowResult);
        raoResult.getActivatedRangeActionsDuringState(preventiveState).forEach(rangeAction -> preventiveRangeActionActivationResult.putResult(rangeAction, preventiveState, raoResult.getOptimizedSetPointOnState(preventiveState, rangeAction)));

        final OptimizationResult preventiveResult = new OptimizationResultImpl(
            preventivePrePerimeterResult, preventivePrePerimeterResult, preventivePrePerimeterResult,
            new NetworkActionsResultImpl(Map.of(
                preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)
            )),
            preventiveRangeActionActivationResult
        );

        final PostPerimeterResult preventivePostPerimeterResult =
            new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, preventivePrePerimeterResult, Set.of(), preventiveResult, new AppliedRemedialActions(), reportNode);

        final Map<State, PostPerimeterResult> postRegulationPostContingencyResults = new HashMap<>();

        final List<Instant> postOutageInstants = crac.getSortedInstants().stream()
            .filter(instant -> instant.isAuto() || instant.isCurative())
            .toList();

        for (final Contingency contingency : crac.getContingencies()) {
            final AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

            network.getVariantManager().cloneVariant(variantName, contingency.getId());
            network.getVariantManager().setWorkingVariant(contingency.getId());

            PrePerimeterResult contingencyPrePerimeterResult = preventivePostPerimeterResult.prePerimeterResultForAllFollowingStates();

            for (final Instant instant : postOutageInstants) {
                final State state = crac.getState(contingency, instant);
                if (state != null) {
                    final RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(contingencyPrePerimeterResult);
                    appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                    raoResult.getActivatedRangeActionsDuringState(state).forEach(
                        rangeAction -> {
                            final double optimizedSetPointOnState = raoResult.getOptimizedSetPointOnState(state, rangeAction);
                            appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizedSetPointOnState);
                            rangeActionActivationResult.putResult(rangeAction, state, optimizedSetPointOnState);
                        }
                    );

                    if (resultsPerState.containsKey(state)) {
                        final PstRegulationResult pstRegulationResult = resultsPerState.get(state);
                        pstRegulationResult.regulatedTapPerPst().forEach(
                            (pstRangeAction, regulatedTap) -> {
                                final double angleSetpoint = pstRangeAction.convertTapToAngle(regulatedTap);
                                appliedRemedialActions.addAppliedRangeAction(state, pstRangeAction, angleSetpoint);
                                rangeActionActivationResult.putResult(pstRangeAction, state, angleSetpoint);
                            }
                        );
                    }

                    final PrePerimeterSensitivityAnalysis statePrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                        crac, crac.getFlowCnecs(state), crac.getRangeActions(), raoParameters, toolProvider, true
                    );

                    final PrePerimeterResult statePrePerimeterResult = statePrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
                        network, initialFlowResult, Collections.emptySet(), appliedRemedialActions, reportNode
                    );

                    final OptimizationResult stateOptimizationResult = new OptimizationResultImpl(
                        statePrePerimeterResult,
                        statePrePerimeterResult,
                        statePrePerimeterResult,
                        new NetworkActionsResultImpl(Map.of(state, raoResult.getActivatedNetworkActionsDuringState(state))),
                        rangeActionActivationResult
                    );
                    final Set<FlowCnec> statePostPerimeterFlowCnecs = crac.getFlowCnecs().stream()
                        .filter(cnec -> !cnec.getState().getInstant().comesBefore(instant))
                        .filter(cnec -> cnec.getState().getContingency().orElseThrow().equals(contingency))
                        .collect(Collectors.toSet());

                    final PostPerimeterResult statePostPerimeterResult =
                        new PostPerimeterSensitivityAnalysis(crac, statePostPerimeterFlowCnecs, crac.getRangeActions(), raoParameters, toolProvider, true)
                            .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, contingencyPrePerimeterResult, Set.of(), stateOptimizationResult, appliedRemedialActions, reportNode);
                    postRegulationPostContingencyResults.put(state, statePostPerimeterResult);

                    contingencyPrePerimeterResult = statePrePerimeterResult;
                }
            }
        }

        final StateTree stateTree = new StateTree(crac, reportNode);
        final PreventiveAndCurativesRaoResultImpl postRegulationRaoResult = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialFlowResult,
            preventivePostPerimeterResult,
            postRegulationPostContingencyResults,
            crac,
            raoParameters,
            reportNode
        );
        final String executionDetails = String.format("%s %s", raoResult.getExecutionDetails(), "and went through PST regulation");
        postRegulationRaoResult.setExecutionDetails(executionDetails);

        return postRegulationRaoResult;
    }
}
