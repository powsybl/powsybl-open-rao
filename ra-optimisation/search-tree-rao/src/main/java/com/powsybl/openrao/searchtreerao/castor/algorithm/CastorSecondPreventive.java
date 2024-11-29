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
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.StandardRangeAction;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
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
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.data.cracapi.range.RangeType.RELATIVE_TO_PREVIOUS_INSTANT;
import static com.powsybl.openrao.data.raoresultapi.ComputationStatus.FAILURE;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine De-Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
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
    boolean shouldRunSecondPreventiveRao(OptimizationResult firstPreventiveResult, Collection<OptimizationResult> curativeRaoResults, RaoResult postFirstRaoResult, long estimatedPreventiveRaoTimeInSeconds) {
        Instant lastCurativeInstant = crac.getLastInstant();
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.DISABLED)) {
            return false;
        }
        if (!Objects.isNull(targetEndInstant) && ChronoUnit.SECONDS.between(java.time.Instant.now(), targetEndInstant) < estimatedPreventiveRaoTimeInSeconds) {
            BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (raoParameters.getSecondPreventiveRaoParameters().getExecutionCondition().equals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE)
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
            return isFinalCostWorseThanPreventive(raoParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement(), firstPreventiveResult, postFirstRaoResult, lastCurativeInstant);
        }
    }

    /**
     * Returns true if any result has a positive functional cost
     */
    private static boolean isAnyResultUnsecure(Collection<OptimizationResult> results) {
        return results.stream().anyMatch(optimizationResult -> optimizationResult.getFunctionalCost() >= 0 || optimizationResult.getVirtualCost() > 1e-6);
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
                                            OptimizationResult firstPreventiveResult,
                                            Map<State, OptimizationResult> postContingencyResults) {
        // Run 2nd preventive RAO
        SecondPreventiveRaoResult secondPreventiveRaoResult;
        try {
            secondPreventiveRaoResult = runSecondPreventiveRao(prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult, postContingencyResults);
            if (secondPreventiveRaoResult.postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                return new FailedRaoResultImpl("Post-PRA sensitivity analysis failed during 2nd preventive RAO");
            }
        } catch (OpenRaoException e) {
            BUSINESS_LOGS.error(e.getMessage());
            return new FailedRaoResultImpl(e.getMessage());
        }

        // Run 2nd automaton simulation and update results
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        Map<State, OptimizationResult> newPostContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(network, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true);
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        // Always re-run curative sensitivity analysis (re-run is necessary in several specific cases)
        // -- Gather all post contingency remedial actions
        // ---- Curative remedial actions :
        // ------ appliedCras from secondPreventiveRaoResult
        AppliedRemedialActions appliedArasAndCras = secondPreventiveRaoResult.appliedArasAndCras().copyCurative();
        // ------ + curative range actions optimized during second preventive with global optimization
        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
                State state = entry.getKey();
                if (!state.getInstant().isCurative()) {
                    continue;
                }
                secondPreventiveRaoResult.perimeterResult().getActivatedRangeActions(state)
                    .forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(state, rangeAction, secondPreventiveRaoResult.perimeterResult.getOptimizedSetpoint(rangeAction, state)));
            }
        }
        // ---- Auto remedial actions : computed during second auto, saved in newPostContingencyResults
        // ---- only RAs from perimeters that haven't failed are included in appliedArasAndCras
        // ---- this check is only performed here because SkippedOptimizationResultImpl with appliedRas can only be generated for AUTO instant
        newPostContingencyResults.entrySet().stream().filter(entry ->
                !(entry.getValue() instanceof SkippedOptimizationResultImpl) && entry.getKey().getInstant().isAuto())
            .forEach(entry -> {
                appliedArasAndCras.addAppliedNetworkActions(entry.getKey(), entry.getValue().getActivatedNetworkActions());
                entry.getValue().getActivatedRangeActions(entry.getKey()).forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(entry.getKey(), rangeAction, entry.getValue().getOptimizedSetpoint(rangeAction, entry.getKey())));
            });
        // Run curative sensitivity analysis with appliedArasAndCras
        // TODO: this is too slow, we can replace it with load-flow computations or security analysis since we don't need sensitivity values
        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, Collections.emptySet(), appliedArasAndCras);
        if (postCraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
            return new FailedRaoResultImpl("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        }
        for (Map.Entry<State, OptimizationResult> entry : postContingencyResults.entrySet()) {
            State state = entry.getKey();
            if (!state.getInstant().isCurative()) {
                continue;
            }
            // Specific case : curative state was previously skipped because it led to a sensitivity analysis failure.
            // Curative state is still a SkippedOptimizationResultImpl, but its computation status must be updated
            if (entry.getValue() instanceof SkippedOptimizationResultImpl) {
                newPostContingencyResults.put(state, new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), postCraSensitivityAnalysisOutput.getSensitivityStatus(entry.getKey()), raoParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost()));
            } else {
                newPostContingencyResults.put(state, new CurativeWithSecondPraoResult(state, entry.getValue(), secondPreventiveRaoResult.perimeterResult(), secondPreventiveRaoResult.remedialActionsExcluded(), postCraSensitivityAnalysisOutput));
            }
        }
        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS, postCraSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);

        return new PreventiveAndCurativesRaoResultImpl(stateTree,
            initialOutput,
            firstPreventiveResult,
            secondPreventiveRaoResult.perimeterResult(),
            secondPreventiveRaoResult.remedialActionsExcluded(),
            secondPreventiveRaoResult.postPraSensitivityAnalysisOutput(),
            newPostContingencyResults,
            postCraSensitivityAnalysisOutput,
            crac);
    }

    private record SecondPreventiveRaoResult(OptimizationResult perimeterResult,
                                             PrePerimeterResult postPraSensitivityAnalysisOutput,
                                             Set<RemedialAction<?>> remedialActionsExcluded,
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
                                                             Map<State, OptimizationResult> postContingencyResults) {
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

        // Apply 1st preventive results for range actions that are both preventive and auto or curative. This way we are sure
        // that the optimal setpoints of the curative results stay coherent with their allowed range and close to
        // optimality in their perimeters. These range actions will be excluded from 2nd preventive RAO.
        Set<RemedialAction<?>> remedialActionsExcluded = new HashSet<>();
        if (!raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) { // keep old behaviour
            remedialActionsExcluded = new HashSet<>(getRangeActionsExcludedFromSecondPreventive(firstPreventiveResult, postContingencyResults));
            applyPreventiveResultsForAutoOrCurativeRangeActions(firstPreventiveResult);
            addAppliedRangeActionsPostContingency(crac.getInstants(InstantKind.CURATIVE), appliedArasAndCras, postContingencyResults);
        }

        // Run a first sensitivity computation using initial network and applied CRAs
        // If any sensitivity computation fails, fail and fall back to 1st preventive result
        // TODO: can we / do we want to improve this behavior by excluding the failed contingencies?
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedArasAndCras);
        if (sensiWithPostContingencyRemedialActions.getSensitivityStatus() == FAILURE) {
            throw new OpenRaoException("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        }
        RaoLogger.logSensitivityAnalysisResults("Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ",
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            sensiWithPostContingencyRemedialActions,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        String newVariant = RandomizedString.getRandomizedString("SecondPreventive", network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT, newVariant, true);
        network.getVariantManager().setWorkingVariant(newVariant);
        OptimizationResult secondPreventiveResult = optimizeSecondPreventivePerimeter(initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult, postContingencyResults, appliedArasAndCras)
            .join().getOptimizationResult(crac.getPreventiveState());
        // Re-run sensitivity computation based on PRAs without CRAs, to access after PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, crac, initialOutput, stateTree.getOperatorsNotSharingCras(), null);
        if (postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        }
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, remedialActionsExcluded, appliedArasAndCras);
    }

    void addAppliedNetworkActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        instants.forEach(instant ->
            postContingencyResults.forEach((state, optimizationResult) -> {
                if (state.getInstant().equals(instant)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, optimizationResult.getActivatedNetworkActions());
                }
            })
        );
    }

    void addAppliedRangeActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, OptimizationResult> postContingencyResults) {
        // Add all range actions that were activated.
        // Curative/ preventive duplicates are handled via exclusion from 2nd preventive
        instants.forEach(instant ->
            postContingencyResults.forEach((state, optimizationResult) -> {
                if (state.getInstant().equals(instant)) {
                    optimizationResult.getActivatedRangeActions(state).forEach(rangeAction -> appliedRemedialActions.addAppliedRangeAction(state, rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
                }
            })
        );
    }

    private CompletableFuture<OneStateOnlyRaoResultImpl> optimizeSecondPreventivePerimeter(PrePerimeterResult initialOutput,
                                                                                           PrePerimeterResult prePerimeterResult,
                                                                                           OptimizationResult firstPreventiveResult,
                                                                                           Map<State, OptimizationResult> postContingencyResults,
                                                                                           AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter;
        Instant preventiveInstant = crac.getPreventiveInstant();
        State preventiveState = crac.getPreventiveState();
        Set<RangeAction<?>> excludedRangeActions = getRangeActionsExcludedFromSecondPreventive(firstPreventiveResult, postContingencyResults);

        if (raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions()) {
            optPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, prePerimeterResult);
        } else {
            Set<RangeAction<?>> rangeActionsFor2p = new HashSet<>(crac.getRangeActions());
            excludedRangeActions.forEach(rangeAction -> {
                BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also auto/curative (or its network element has an associated ARA/CRA)", rangeAction.getId());
                rangeActionsFor2p.remove(rangeAction);
            });
            optPerimeter = PreventiveOptimizationPerimeter.buildWithAllCnecs(crac, rangeActionsFor2p, network, raoParameters, prePerimeterResult);
        }

        SearchTreeParameters searchTreeParameters = SearchTreeParameters.create()
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()))
            .build();

        // update RaUsageLimits with already applied RangeActions
        if (!raoParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions() && searchTreeParameters.getRaLimitationParameters().containsKey(preventiveInstant)) {
            Set<RangeAction<?>> activatedPreventiveRangeActions = firstPreventiveResult.getActivatedRangeActions(preventiveState);
            Set<RangeAction<?>> excludedActivatedRangeActions = excludedRangeActions.stream().filter(activatedPreventiveRangeActions::contains).collect(Collectors.toSet());
            searchTreeParameters.setRaLimitationsForSecondPreventive(searchTreeParameters.getRaLimitationParameters().get(preventiveInstant), excludedActivatedRangeActions, preventiveInstant);
        }

        if (raoParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao()) {
            // Set the optimal set of network actions decided in 1st preventive RAO as a hint for 2nd preventive RAO
            searchTreeParameters.getNetworkActionParameters().addNetworkActionCombination(new NetworkActionCombination(firstPreventiveResult.getActivatedNetworkActions(), true));
        }

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialOutput)
            .withPrePerimeterResult(prePerimeterResult)
            .withPreOptimizationAppliedNetworkActions(appliedCras) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.create().build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialOutput, prePerimeterResult, new HashSet<>(), raoParameters))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true).run().join();

        // apply PRAs
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        result.getActivatedRangeActions(preventiveState).forEach(rangeAction -> rangeAction.apply(network, result.getOptimizedSetpoint(rangeAction, preventiveState)));
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));

        return CompletableFuture.completedFuture(new OneStateOnlyRaoResultImpl(preventiveState, prePerimeterResult, result, optPerimeter.getFlowCnecs()));
    }

    /**
     * This method applies range action results on the network, for range actions that are auto or curative
     * It is used for second preventive optimization along with 1st preventive results in order to keep the result
     * of 1st preventive for range actions that are both preventive and auto or curative
     */
    void applyPreventiveResultsForAutoOrCurativeRangeActions(OptimizationResult preventiveResult) {
        preventiveResult.getActivatedRangeActions(crac.getPreventiveState()).stream()
            .filter(crac::isRangeActionAutoOrCurative)
            .forEach(rangeAction -> rangeAction.apply(network, preventiveResult.getOptimizedSetpoint(rangeAction, crac.getPreventiveState())));
    }

    /**
     * Returns the set of range actions that are excluded from the 2nd preventive RAO.
     * The concerned range actions meet certain criterion.
     * 1- The RA has a range limit relative to the previous instant.
     * This way we avoid incoherence between preventive & curative tap positions.
     * 2- For the remaining RAs we are going to remove some for the reason explained below.
     * Let's consider a rangeAction that has the same tap in preventive and in another state.
     * If so, considering it in the second preventive optimization could change its tap for preventive only.
     * Therefore, the RA would no longer have the same taps in preventive and for the given contingency state: It's consider used for the given state.
     * That could lead the RAO to wrongly exceed the RaUsageLimits for the given state.
     * To avoid this, we don't want to optimize these RAs.
     * For the same reason, we are going to check preventive RAs that share the same network elements as auto or curative RAs.
     */
    Set<RangeAction<?>> getRangeActionsExcludedFromSecondPreventive(OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> contingencyResults) {

        // Excludes every non-preventive RA.
        Set<RangeAction<?>> nonPreventiveRangeActions = crac.getRangeActions().stream().filter(ra -> !crac.isRangeActionPreventive(ra)).collect(Collectors.toSet());
        Set<RangeAction<?>> rangeActionsToExclude = new HashSet<>(nonPreventiveRangeActions);

        // Gathers PRAs that are also ARA/CRAs.
        Set<RangeAction<?>> multipleInstantRangeActions = crac.getRangeActions().stream()
            .filter(ra -> crac.isRangeActionPreventive(ra) && crac.isRangeActionAutoOrCurative(ra))
            .collect(Collectors.toSet());

        // Excludes the ones that have a range limit relative to the previous instant.
        multipleInstantRangeActions.stream().filter(CastorSecondPreventive::raHasRelativeToPreviousInstantRange).forEach(rangeActionsToExclude::add);
        rangeActionsToExclude.forEach(multipleInstantRangeActions::remove);

        // We look for PRAs that share the same network element as ARA/CRAs as the same rules apply to them.
        Map<RangeAction<?>, Set<RangeAction<?>>> correspondanceMap = new HashMap<>();
        crac.getRangeActions().stream().filter(ra -> crac.isRangeActionPreventive(ra) && !crac.isRangeActionAutoOrCurative(ra)).forEach(pra -> {
            Set<NetworkElement> praNetworkElements = pra.getNetworkElements();
            for (RangeAction<?> cra : nonPreventiveRangeActions) {
                if (cra.getNetworkElements().equals(praNetworkElements)) {
                    if (raHasRelativeToPreviousInstantRange(cra)) {
                        // Excludes PRAs which share the same network element as an ARA/CRA with a range limit relative to the previous instant.
                        rangeActionsToExclude.add(pra);
                        correspondanceMap.remove(pra);
                        break;
                    } else {
                        // Gathers PRAs with their associated ARA/CRAs inside a map.
                        correspondanceMap.putIfAbsent(pra, new HashSet<>());
                        correspondanceMap.get(pra).add(cra);
                    }
                }
            }
        });

        // If first preventive diverged, we want to remove every range action that is both preventive and auto or curative.
        if (firstPreventiveResult instanceof SkippedOptimizationResultImpl) {
            multipleInstantRangeActions.addAll(correspondanceMap.keySet());
            return multipleInstantRangeActions;
        }

        // Excludes RAs that put crac RaUsageLimits at risk.
        // First, we filter out state that diverged because we know no set-point was chosen for this state.
        Map<State, OptimizationResult> newContingencyResults = new HashMap<>(contingencyResults);
        newContingencyResults.entrySet().removeIf(entry -> entry.getValue() instanceof SkippedOptimizationResultImpl);

        // Then, we build a map that gives for each RA, its tap at each state it's available at.
        State preventiveState = crac.getPreventiveState();
        Map<State, Map<RangeAction<?>, Double>> setPointResults = buildSetPointResultsMap(crac, firstPreventiveResult, newContingencyResults, correspondanceMap, multipleInstantRangeActions, preventiveState);

        // Finally, we filter out RAs that put crac RaUsageLimits at risk.
        rangeActionsToExclude.addAll(getRangeActionsToRemove(crac, preventiveState, setPointResults, newContingencyResults));
        return rangeActionsToExclude;
    }

    /**
     * Creates a map that gives for a given state, each available RA with its tap.
     * The only subtlety being that RAs sharing exactly the same network elements are considered to be only one RA.
     */
    private static Map<State, Map<RangeAction<?>, Double>> buildSetPointResultsMap(Crac crac, OptimizationResult firstPreventiveResult, Map<State, OptimizationResult> contingencyResults, Map<RangeAction<?>, Set<RangeAction<?>>> correspondanceMap, Set<RangeAction<?>> multipleInstantRangeActions, State preventiveState) {
        Map<State, Map<RangeAction<?>, Double>> setPointResults = new HashMap<>(Map.of(preventiveState, new HashMap<>()));
        correspondanceMap.forEach((pra, associatedCras) -> {
            setPointResults.get(preventiveState).put(pra, firstPreventiveResult.getOptimizedSetpoint(pra, preventiveState));
            associatedCras.forEach(cra -> contingencyResults.forEach((state, result) -> {
                if (crac.isRangeActionAvailableInState(cra, state)) {
                    setPointResults.putIfAbsent(state, new HashMap<>());
                    setPointResults.get(state).put(pra, result.getOptimizedSetpoint(cra, state));
                }
            }));
        });
        multipleInstantRangeActions.forEach(ra -> {
            setPointResults.get(preventiveState).put(ra, firstPreventiveResult.getOptimizedSetpoint(ra, preventiveState));
            contingencyResults.forEach((state, result) -> {
                if (crac.isRangeActionAvailableInState(ra, state)) {
                    setPointResults.putIfAbsent(state, new HashMap<>());
                    setPointResults.get(state).put(ra, result.getOptimizedSetpoint(ra, state));
                }
            });
        });
        return setPointResults;
    }

    /**
     * Checks if raUsageLimits are at risk if we choose to re-optimize a range action.
     * Returns True if it's at risk, False otherwise.
     */
    private static boolean shouldRemoveRaDueToUsageLimits(String operator, RaUsageLimits raUsageLimits, Set<RangeAction<?>> activatableRangeActions, Set<NetworkAction> activatedNetworkActions) {
        if (operator == null) {
            return raUsageLimits.getMaxRa() < activatableRangeActions.size() + activatedNetworkActions.size();
        }

        Set<RemedialAction<?>> activatableRemedialActions = new HashSet<>(activatableRangeActions);
        activatableRemedialActions.addAll(activatedNetworkActions);

        long activatableRangeActionsForTheTso = activatableRangeActions.stream().filter(ra -> operator.equals(ra.getOperator())).count();
        long activatableRemedialActionsForTheTso = activatableRemedialActions.stream().filter(ra -> operator.equals(ra.getOperator())).count();
        long activatableTsos = activatableRemedialActions.stream().map(RemedialAction::getOperator).filter(Objects::nonNull).distinct().count();

        int limitingRangeActionValueForTheTso = raUsageLimits.getMaxPstPerTso().getOrDefault(operator, Integer.MAX_VALUE);
        int limitingRemedialActionValueForTheTso = raUsageLimits.getMaxRaPerTso().getOrDefault(operator, Integer.MAX_VALUE);

        return raUsageLimits.getMaxRa() < activatableRangeActions.size() + activatedNetworkActions.size()
            || limitingRangeActionValueForTheTso < activatableRangeActionsForTheTso
            || limitingRemedialActionValueForTheTso < activatableRemedialActionsForTheTso
            || raUsageLimits.getMaxTso() < activatableTsos;
    }

    /**
     * Gathers every range action that should not be considered in the second preventive if those 2 criterion are met :
     * 1- The range action has the same tap in preventive and in a contingency scenario.
     * 2- For the given state, the crac has limiting RaUsageLimits.
     */
    private static Set<RangeAction<?>> getRangeActionsToRemove(Crac crac, State preventiveState, Map<State, Map<RangeAction<?>, Double>> setPointResults, Map<State, OptimizationResult> contingencyResults) {
        Set<RangeAction<?>> rangeActionsToRemove = new HashSet<>();
        setPointResults.forEach((state, spMap) -> {
            if (!state.isPreventive()) {
                Set<RangeAction<?>> activatableRangeActions = crac.getPotentiallyAvailableRangeActions(state);
                Set<NetworkAction> activatedNetworkActions = contingencyResults.get(state).getActivatedNetworkActions();
                spMap.forEach((ra, setPoint) -> {
                    if (setPoint.equals(setPointResults.get(preventiveState).get(ra))
                        && crac.getRaUsageLimitsPerInstant().containsKey(state.getInstant())
                        && shouldRemoveRaDueToUsageLimits(ra.getOperator(), crac.getRaUsageLimits(state.getInstant()), activatableRangeActions, activatedNetworkActions)) {
                        rangeActionsToRemove.add(ra);
                    }
                });
            }
        });
        return rangeActionsToRemove;
    }

    /**
     * Returns True if the rangeAction has a RELATIVE_TO_PREVIOUS_INSTANT range. Else, returns False.
     */
    private static boolean raHasRelativeToPreviousInstantRange(RangeAction<?> rangeAction) {
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            return pstRangeAction.getRanges().stream().anyMatch(tapRange -> tapRange.getRangeType().equals(RELATIVE_TO_PREVIOUS_INSTANT));
        }
        return ((StandardRangeAction<?>) rangeAction).getRanges().stream().anyMatch(standardRange -> standardRange.getRangeType().equals(RELATIVE_TO_PREVIOUS_INSTANT));
    }
}
