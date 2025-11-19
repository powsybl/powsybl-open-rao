/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.*;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.result.impl.*;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.FAILURE;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoObjectiveFunctionParameters.getCurativeMinObjImprovement;
import static com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters.*;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CastorSecondPreventive {
    private final Crac crac;
    private final RaoParameters raoParameters;
    private final Network network;
    private final StateTree stateTree;
    private final ToolProvider toolProvider;
    private final java.time.Instant targetEndInstant;

    private static final String SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT = "SecondPreventiveScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    public CastorSecondPreventive(Crac crac,
                                  RaoParameters raoParameters,
                                  Network network,
                                  StateTree stateTree,
                                  ToolProvider toolProvider,
                                  java.time.Instant targetEndInstant) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.network = network;
        this.stateTree = stateTree;
        this.toolProvider = toolProvider;
        this.targetEndInstant = targetEndInstant;
    }

    /**
     * This function decides if a 2nd preventive RAO should be run. It checks the user parameter first, then takes the
     * decision depending on the curative RAO results and the curative RAO stop criterion.
     */
    boolean shouldRunSecondPreventiveRao(OptimizationResult firstPreventiveResult, Collection<PostPerimeterResult> curativeRaoResults, RaoResult postFirstRaoResult, long estimatedPreventiveRaoTimeInSeconds) {
        Instant lastCurativeInstant = crac.getLastInstant();
        if (getSecondPreventiveExecutionCondition(raoParameters).equals(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(java.time.Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (getSecondPreventiveExecutionCondition(raoParameters).equals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE)
            && postFirstRaoResult.getCost(lastCurativeInstant) <= postFirstRaoResult.getCost(null)) {
            BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
            // it is not necessary to compare initial & post-preventive costs since the preventive RAO cannot increase its own cost
            // only compare initial cost with the curative costs
            return false;
        }
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType = raoParameters.getObjectiveFunctionParameters().getType();
        if (objectiveFunctionType.equals(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW)) {
            if (firstPreventiveResult.getCost() > 0) {
                // in case of curative optimization even if preventive unsecure (see parameter enforce-curative-security)
                // we do not want to run a second preventive that would not be able to fix the situation, to save time
                BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
                return false;
            }
            // Run 2nd preventive RAO if one perimeter of the curative optimization is unsecure
            return isAnyResultUnsecure(curativeRaoResults);
        } else {  // MIN OBJECTIVE
            // Run 2nd preventive RAO if the final result has a worse cost than the preventive perimeter
            return isFinalCostWorseThanPreventive(getCurativeMinObjImprovement(raoParameters), firstPreventiveResult, postFirstRaoResult, lastCurativeInstant);
        }
    }

    /**
     * Returns true if any result has a positive functional cost
     */
    private static boolean isAnyResultUnsecure(Collection<PostPerimeterResult> results) {
        return results.stream().anyMatch(postPerimeterResult -> postPerimeterResult.getOptimizationResult().getFunctionalCost() >= 0 || postPerimeterResult.getOptimizationResult().getVirtualCost() > 1e-6);
    }

    /**
     * Returns true if final cost (after PRAO + ARAO + CRAO) is worse than the cost at the end of the preventive perimeter
     */
    private static boolean isFinalCostWorseThanPreventive(double curativeMinObjImprovement, OptimizationResult preventiveResult, RaoResult postFirstRaoResult, Instant curativeInstant) {
        return postFirstRaoResult.getCost(curativeInstant) > preventiveResult.getCost() - curativeMinObjImprovement;
    }

    RaoResult runSecondPreventiveAndAutoRao(CastorContingencyScenarios castorContingencyScenarios,
                                            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                            PrePerimeterResult initialOutput,
                                            PostPerimeterResult firstPreventiveResult,
                                            Map<State, PostPerimeterResult> postContingencyResults) {
        // Run 2nd preventive RAO
        SecondPreventiveRaoResult secondPreventiveRaoResult;
        try {
            secondPreventiveRaoResult = runSecondPreventiveRao(prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult.getOptimizationResult(), postContingencyResults);
            if (secondPreventiveRaoResult.postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                return new FailedRaoResultImpl("Post-PRA sensitivity analysis failed during 2nd preventive RAO");
            }
        } catch (OpenRaoException e) {
            BUSINESS_LOGS.error(e.getMessage());
            return new FailedRaoResultImpl(String.format("RAO failed during second preventive : %s", e.getMessage()));
        }

        // Run 2nd automaton simulation and update results
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        Map<State, PostPerimeterResult> newPostContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(network, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true);
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        // Always re-run curative sensitivity analysis (re-run is necessary in several specific cases)
        // -- Gather all post contingency remedial actions
        // ---- Curative remedial actions :
        // ------ appliedCras from secondPreventiveRaoResult
        AppliedRemedialActions appliedArasAndCras = secondPreventiveRaoResult.appliedArasAndCras().copyCurative();
        // ------ + curative range actions optimized during second preventive
        for (Map.Entry<State, PostPerimeterResult> entry : postContingencyResults.entrySet()) {
            State state = entry.getKey();
            if (!state.getInstant().isCurative()) {
                continue;
            }
            secondPreventiveRaoResult.perimeterResult().getActivatedRangeActions(state)
                .forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(state, rangeAction, secondPreventiveRaoResult.perimeterResult.getOptimizedSetpoint(rangeAction, state)));
        }
        // ---- Auto remedial actions : computed during second auto, saved in newPostContingencyResults
        // ---- only RAs from perimeters that haven't failed are included in appliedArasAndCras
        // ---- this check is only performed here because SkippedOptimizationResultImpl with appliedRas can only be generated for AUTO instant
        newPostContingencyResults.entrySet().stream().filter(entry ->
                !(entry.getValue().getOptimizationResult() instanceof SkippedOptimizationResultImpl) && entry.getKey().getInstant().isAuto())
            .forEach(entry -> {
                appliedArasAndCras.addAppliedNetworkActions(entry.getKey(), entry.getValue().getOptimizationResult().getActivatedNetworkActions());
                entry.getValue().getOptimizationResult().getActivatedRangeActions(entry.getKey()).forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(entry.getKey(), rangeAction, entry.getValue().getOptimizationResult().getOptimizedSetpoint(rangeAction, entry.getKey())));
            });
        // Run curative sensitivity analysis with appliedArasAndCras
        // TODO: this is too slow, we can replace it with load-flow computations or security analysis since we don't need sensitivity values
        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, Collections.emptySet(), appliedArasAndCras);
        if (postCraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
            return new FailedRaoResultImpl("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        }
        for (Map.Entry<State, PostPerimeterResult> entry : postContingencyResults.entrySet()) {
            State state = entry.getKey();
            if (!state.getInstant().isCurative()) {
                continue;
            }
            // Specific case : curative state was previously skipped because it led to a sensitivity analysis failure.
            // Curative state is still a SkippedOptimizationResultImpl, but its computation status must be updated
            if (entry.getValue().getOptimizationResult() instanceof SkippedOptimizationResultImpl) {
                OptimizationResult skippedResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), postCraSensitivityAnalysisOutput.getSensitivityStatus(entry.getKey()), getSensitivityFailureOvercost(raoParameters));
                PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedResult, skippedResult, null, skippedResult);
                newPostContingencyResults.put(state, new PostPerimeterResult(skippedResult, prePerimeterResult));
            } else {
                newPostContingencyResults.put(state, new PostPerimeterResult(
                    new CurativeWithSecondPraoResult(state, entry.getValue().getOptimizationResult(), secondPreventiveRaoResult.perimeterResult(), postCraSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType().costOptimization()),
                    postCraSensitivityAnalysisOutput
                ));
            }
        }
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, postCraSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        RaoLogger.checkIfMostLimitingElementIsFictional(BUSINESS_LOGS, postCraSensitivityAnalysisOutput);

        PostPerimeterResult postPraResult = new PostPerimeterResult(
            secondPreventiveRaoResult.perimeterResult,
            secondPreventiveRaoResult.postPraSensitivityAnalysisOutput
        );

        return new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialOutput,
            firstPreventiveResult,
            postPraResult,
            newPostContingencyResults,
            crac,
            raoParameters);
    }

    private record SecondPreventiveRaoResult(OptimizationResult perimeterResult,
                                             PrePerimeterResult postPraSensitivityAnalysisOutput,
                                             AppliedRemedialActions appliedArasAndCras) {
    }

    /**
     * Main function to run 2nd preventive RAO
     * Using 1st preventive and curative results, it ets up network and range action contexts, then calls the optimizer
     * It finally merges the three results into one RaoResult object
     */
    private SecondPreventiveRaoResult runSecondPreventiveRao(PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                             PrePerimeterResult initialOutput,
                                                             OptimizationResult firstPreventiveResult,
                                                             Map<State, PostPerimeterResult> postContingencyResults) {
        // Go back to the initial state of the network, saved in the SECOND_PREVENTIVE_STATE variant
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);

        // Get the applied network actions for every contingency perimeter
        AppliedRemedialActions appliedArasAndCras = new AppliedRemedialActions();
        if (crac.hasAutoInstant()) {
            addAppliedNetworkActionsPostContingency(crac.getInstants(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }
        addAppliedNetworkActionsPostContingency(crac.getInstants(InstantKind.CURATIVE), appliedArasAndCras, postContingencyResults);
        // Get the applied range actions for every auto contingency perimeter
        if (crac.hasAutoInstant()) {
            addAppliedRangeActionsPostContingency(crac.getInstants(InstantKind.AUTO), appliedArasAndCras, postContingencyResults);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        // If any sensitivity computation fails, fail and fall back to 1st preventive result
        // TODO: can we / do we want to improve this behavior by excluding the failed contingencies?
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedArasAndCras);
        if (sensiWithPostContingencyRemedialActions.getSensitivityStatus() == FAILURE) {
            throw new OpenRaoException("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            new RemedialActionActivationResultImpl(new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, crac.getRangeActions())), new NetworkActionsResultImpl(getAllAppliedNetworkAraAndCra(appliedArasAndCras))),
            sensiWithPostContingencyRemedialActions,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        String newVariant = RandomizedString.getRandomizedString("SecondPreventive", network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT, newVariant, true);
        network.getVariantManager().setWorkingVariant(newVariant);
        OptimizationResult secondPreventiveResult = optimizeSecondPreventivePerimeter(initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult, appliedArasAndCras)
            .join().getOptimizationResult(crac.getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access after PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        if (postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        }
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, appliedArasAndCras);
    }

    void addAppliedNetworkActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, PostPerimeterResult> postContingencyResults) {
        instants.forEach(instant ->
            postContingencyResults.forEach((state, postPerimeterResult) -> {
                if (state.getInstant().equals(instant)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, postPerimeterResult.getOptimizationResult().getActivatedNetworkActions());
                }
            })
        );
    }

    void addAppliedRangeActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, PostPerimeterResult> postContingencyResults) {
        // Add all range actions that were activated.
        // Curative/ preventive duplicates are handled via exclusion from 2nd preventive
        instants.forEach(instant ->
            postContingencyResults.forEach((state, postPerimeterResult) -> {
                if (state.getInstant().equals(instant)) {
                    postPerimeterResult.getOptimizationResult().getActivatedRangeActions(state).forEach(rangeAction ->
                        appliedRemedialActions.addAppliedRangeAction(state, rangeAction, postPerimeterResult.getOptimizationResult().getOptimizedSetpoint(rangeAction, state)));
                }
            })
        );
    }

    private Map<State, Set<NetworkAction>> getAllAppliedNetworkAraAndCra(AppliedRemedialActions appliedArasAndCras) {
        Map<State, Set<NetworkAction>> appliedNetworkActions = new HashMap<>();
        crac.getStates().stream().filter(state -> state.getInstant().isAuto() || state.getInstant().isCurative())
            .forEach(state -> appliedNetworkActions.put(state, appliedArasAndCras.getAppliedNetworkActions(state)));
        return appliedNetworkActions;
    }

    private CompletableFuture<OneStateOnlyRaoResultImpl> optimizeSecondPreventivePerimeter(PrePerimeterResult initialOutput,
                                                                                           PrePerimeterResult prePerimeterResult,
                                                                                           OptimizationResult firstPreventiveResult,
                                                                                           AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter;
        State preventiveState = crac.getPreventiveState();

        optPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, prePerimeterResult);

        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters();
            searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        SearchTreeParameters searchTreeParameters = searchTreeParametersBuilder.build();

        if (getSecondPreventiveHintFromFirstPreventiveRao(raoParameters)) {
            // Set the optimal set of network actions decided in 1st preventive RAO as a hint for 2nd preventive RAO
            searchTreeParameters.getNetworkActionParameters().addNetworkActionCombination(new NetworkActionCombination(firstPreventiveResult.getActivatedNetworkActions(), true));
        }

        Set<State> statesToOptimize = new HashSet<>(optPerimeter.getMonitoredStates());
        statesToOptimize.add(optPerimeter.getMainOptimizationState());

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) // no remedial action applied
            .withObjectiveFunction(ObjectiveFunction.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, new HashSet<>(), raoParameters, statesToOptimize))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

        // apply PRAs
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        // network actions need to be applied BEFORE range actions because to apply HVDC range actions we need to apply AC emulation deactivation actions beforehand
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        result.getActivatedRangeActions(preventiveState).forEach(rangeAction -> rangeAction.apply(network, result.getOptimizedSetpoint(rangeAction, preventiveState)));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(preventiveState, prePerimeterResult, result, optPerimeter.getFlowCnecs()));
    }
}
