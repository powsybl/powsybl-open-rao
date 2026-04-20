/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.pstregulation;

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
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
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

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.parameters.extensions.MultithreadingParameters.getAvailableCPUs;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PstRegulation {
    private static final String INITIAL_SCENARIO = "InitialScenario";

    private PstRegulation() {
    }

    public static RaoResult regulatePsts(Network network,
                                         Crac crac,
                                         RaoResult raoResult,
                                         RaoParameters raoParameters) {
        Map<String, String> pstsToRegulate = SearchTreeRaoPstRegulationParameters.getPstsToRegulate(raoParameters);
        if (pstsToRegulate.isEmpty()) {
            return raoResult;
        }

        Set<PstRangeAction> rangeActionsToRegulate = getPstRangeActionsForRegulation(pstsToRegulate.keySet(), crac);
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
        BUSINESS_LOGS.info(
            "{} contingency scenario(s) to regulate: {}",
            contingencies.size(),
            String.join(", ", contingencies.stream().map(contingency -> contingency.getName().orElse(contingency.getId())).sorted().toList()));
        BUSINESS_LOGS.info(
            "{} PST(s) to regulate: {}",
            rangeActionsToRegulate.size(),
            String.join(", ", rangeActionsToRegulate.stream().map(PstRangeAction::getName).sorted().toList()));

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
                    () -> regulatePstsForContingencyScenario(pstRegulationInput, crac, rangeActionsToRegulate, raoResult, loadFlowParameters, networkPool)
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
            // FIXME [VB] All the above looks similar to what is done in CastorPstRegulation on main branch
            //  The difference here is that we return a RaoResult by calling the method below, whereas on the main branch,
            //  the regulatePsts() method returns pstRegulationResults and the merging & build of RaoResult is made in 2 separate methods called in CastorFullOptimization
            return mergePstRegulationResultsWithRaoResult(pstRegulationResults, raoResult, network, crac, raoParameters);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            BUSINESS_WARNS.warn("An error occurred during PST regulation, pre-regulation RAO result will be kept. Error was: {}", e.getMessage());
            return raoResult;
        } finally {
            loadFlowParameters.setPhaseShifterRegulationOn(initialPhaseShifterRegulationOnValue);
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
    private static PstRegulationResult regulatePstsForContingencyScenario(PstRegulationInput pstRegulationInput,
                                                                          Crac crac,
                                                                          Set<PstRangeAction> rangeActionsToRegulate,
                                                                          RaoResult raoResult,
                                                                          LoadFlowParameters loadFlowParameters,
                                                                          AbstractNetworkPool networkPool) throws InterruptedException {
        Network networkClone = networkPool.getAvailableNetwork();
        Contingency contingency = pstRegulationInput.curativeState().getContingency().orElseThrow();
        simulateContingencyAndApplyCurativeActions(contingency, networkClone, crac, raoResult);
        Map<PstRangeAction, Integer> initialTapPerPst = getInitialTapPerPst(rangeActionsToRegulate, networkClone);
        Map<PstRangeAction, Integer> regulatedTapPerPst = PstRegulator.regulatePsts(pstRegulationInput.elementaryPstRegulationInputs(), networkClone, loadFlowParameters);
        logPstRegulationResultsForContingencyScenario(contingency, initialTapPerPst, regulatedTapPerPst, pstRegulationInput.limitingElement());
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

    private static void logPstRegulationResultsForContingencyScenario(Contingency contingency,
                                                                      Map<PstRangeAction, Integer> initialTapPerPst,
                                                                      Map<PstRangeAction, Integer> regulatedTapPerPst,
                                                                      FlowCnec mostLimitingElement) {
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
            BUSINESS_LOGS.info("FlowCNEC '{}' of contingency scenario '{}' is overloaded and is the most limiting element, PST regulation has been triggered: {}", mostLimitingElement.getId(), contingency.getName().orElse(contingency.getId()), allShiftedPstsDetails);
        }
    }

    // FIXME [VB] What causes problems is located in this merging method.
    //  On the main branch, this methods takes PostPerimeterResult postPraResult, Map<State, PostPerimeterResult> postContingencyResults,
    //  PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis and FlowResult initialFlowResult.
    //  Here, the method takes a RaoResult and RaoParameters.
    private static RaoResult mergePstRegulationResultsWithRaoResult(Set<PstRegulationResult> pstRegulationResults,
                                                                    RaoResult raoResult,
                                                                    Network network,
                                                                    Crac crac,
                                                                    RaoParameters raoParameters) {
        Map<State, PstRegulationResult> resultsPerState = pstRegulationResults.stream()
            .collect(Collectors.toMap(
                pstRegulationResult -> crac.getState(pstRegulationResult.contingency(), crac.getLastInstant()),
                Function.identity()
        ));

        ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(
            RaoInput.build(network, crac).build(), raoParameters
        );
        final PrePerimeterSensitivityAnalysis initialPrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true
        );
        PrePerimeterResult initialFlowResult = initialPrePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network);

        // create a new network variant from initial variant for performing the results merging
        String variantName = "PSTRegulationResultsMerging";
        // FIXME Maybe that's an odd question, but why do we set working variant to initial scenario before cloning if
        //  we jump to the new variant right after?
        //  => Seems that we don't really need to do so
        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(INITIAL_SCENARIO, variantName);
        network.getVariantManager().setWorkingVariant(variantName);

        // apply PRAs
        State preventiveState = crac.getPreventiveState();
        applyOptimalRemedialActionsForState(network, raoResult, preventiveState);
        // FIXME [VB] Why is "postPraResult" a PrePerimeterResult here, whereas it is a PostPerimeterResult on the main branch?
        //  => Bad naming of this variable
        PrePerimeterResult postPraResult = initialPrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
            network, initialFlowResult, Set.of(), new AppliedRemedialActions()
        );

        // FIXME [VB] Is this preventiveResult supposed to be equivalent to postPraResult.optimizationResult() of the main branch?
        //  => YES
        OptimizationResult preventiveResult = new OptimizationResultImpl(
            postPraResult, postPraResult, postPraResult,
            new NetworkActionsResultImpl(Map.of(
                preventiveState, raoResult.getActivatedNetworkActionsDuringState(preventiveState)
            )),
            new RangeActionActivationResultImpl(
                new RangeActionSetpointResultImpl(raoResult.getOptimizedSetPointsOnState(preventiveState))
            )
        );
        // FIXME [VB] Is this postPreventivePerimeterResult supposed to be equivalent to postPraResult of the main branch?
        //  => YES
        PostPerimeterResult postPreventivePerimeterResult =
            new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, postPraResult, Set.of(), preventiveResult, new AppliedRemedialActions());

        // FIXME [VB] Is this postRegulationPostContingencyResults supposed to be equivalent to postContingencyResults of the main branch?
        //  =>YES
        Map<State, PostPerimeterResult> postRegulationPostContingencyResults = new HashMap<>();

        List<Instant> postOutageInstants = crac.getSortedInstants().stream()
            .filter(instant -> instant.isAuto() || instant.isCurative())
            .toList();

        for (Contingency contingency : crac.getContingencies()) {
            AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();

            network.getVariantManager().cloneVariant(variantName, contingency.getId());
            network.getVariantManager().setWorkingVariant(contingency.getId());

            PrePerimeterResult prePerimeterResult = postPreventivePerimeterResult.prePerimeterResultForAllFollowingStates();

            for (Instant instant : postOutageInstants) {
                State state = crac.getState(contingency, instant);
                // FIXME [VB] What happens in this second for-loop should correspond to what is done on lines 494-500 & 532-543 of
                //  original code in CastorFullOptimization
                if (state != null) {
                    final RangeActionActivationResultImpl rangeActionActivationResult = new RangeActionActivationResultImpl(prePerimeterResult);
                    appliedRemedialActions.addAppliedNetworkActions(state, raoResult.getActivatedNetworkActionsDuringState(state));
                    // FIXME [VB] Does raoResult.getOptimizedSetPointsOnState(state) return all PRA+ARA+CRA?
                    //  On main branch, we must use postPraResult for PRA and postPerimeterResult (from postContingencyResults) for ARA+CRA
                    appliedRemedialActions.addAppliedRangeActions(state, raoResult.getOptimizedSetPointsOnState(state));
                    raoResult.getOptimizedSetPointsOnState(state).forEach(
                        (rangeAction, setpoint) -> rangeActionActivationResult.putResult(rangeAction, state, setpoint)
                    );

                    if (resultsPerState.containsKey(state)) {
                        // FIXME [VB] This code seems to correspond to the code on lines 506-515 of the main branch
                        PstRegulationResult pstRegulationResult = resultsPerState.get(state);
                        pstRegulationResult.regulatedTapPerPst().forEach(
                            (pstRangeAction, regulatedTap) -> {
                                appliedRemedialActions.addAppliedRangeAction(state, pstRangeAction, pstRangeAction.convertTapToAngle(regulatedTap));
                                rangeActionActivationResult.putResult(pstRangeAction, state, pstRangeAction.convertTapToAngle(regulatedTap));
                            }
                        );
                    }

                    appliedRemedialActions.getAppliedNetworkActions(state).forEach(
                        networkAction -> networkAction.apply(network)
                    );
                    // FIXME [VB] Here all remedialActions will be applied on network.
                    //  However it seems that we don't apply regulatedPst on network in the main branch's code.
                    appliedRemedialActions.getAppliedRangeActions(state).forEach(
                        (rangeAction, setPoint) -> rangeAction.apply(network, setPoint)
                    );
                    // FIXME [VB] The appliedRemedialActions variable is never used later in the code...
                    //  As we are in a loop, it will possibly be filled with more data in the next iterations, which means that
                    //  there will be more actions applied on the network variants (or different actions if some existing ones are overridden?)

                    // FIXME [VB] Is this postInstantPerimeterResult supposed to be an equivalent to postCraSensitivityAnalysisOutput of the main branch?
                    //  But postCraSensitivityAnalysisOutput is outside of a loop and here we are inside a loop... Does it make a difference?
                    //  Furthermore, why do we use an empty AppliedRemedialActions object instead of using the appliedRemedialActions instance that has been populated above?
                    PrePerimeterSensitivityAnalysis statePrePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                        crac, crac.getFlowCnecs(state), crac.getRangeActions(), raoParameters, toolProvider, true
                    );

                    PrePerimeterResult postInstantPerimeterResult = statePrePerimeterSensitivityAnalysis.runBasedOnInitialResults(
                        network, initialFlowResult, Collections.emptySet(), new AppliedRemedialActions()
                    );

                    OptimizationResult newOptimizationResult = new OptimizationResultImpl(
                        postInstantPerimeterResult,
                        postInstantPerimeterResult,
                        postInstantPerimeterResult,
                        new NetworkActionsResultImpl(Map.of(state, raoResult.getActivatedNetworkActionsDuringState(state))),
                        rangeActionActivationResult
                    );
                    final Set<FlowCnec> statePostPerimeterFlowCnecs = crac.getFlowCnecs().stream()
                        .filter(cnec -> !cnec.getState().getInstant().comesBefore(instant))
                        .filter(cnec -> cnec.getState().getContingency().orElseThrow().equals(contingency))
                        .collect(Collectors.toSet());

                    PostPerimeterResult postPerimeterStateResult =
                        new PostPerimeterSensitivityAnalysis(crac, statePostPerimeterFlowCnecs, crac.getRangeActions(), raoParameters, toolProvider, true)
                            .runBasedOnInitialPreviousAndOptimizationResults(network, initialFlowResult, prePerimeterResult, Set.of(), newOptimizationResult, new AppliedRemedialActions());
                    postRegulationPostContingencyResults.put(state, postPerimeterStateResult);

                    prePerimeterResult = postInstantPerimeterResult;
                }
            }
        }

        StateTree stateTree = new StateTree(crac);
        return new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialFlowResult,
            postPreventivePerimeterResult,
            postRegulationPostContingencyResults,
            crac,
            raoParameters);
    }
}
