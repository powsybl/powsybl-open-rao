/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
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
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.reports.MostLimitingElementsReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.CurativeWithSecondPraoResult;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PrePerimeterSensitivityResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionSetpointResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.SkippedOptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.FAILURE;
import static com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters.getSensitivityFailureOvercost;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoObjectiveFunctionParameters.getCurativeMinObjImprovement;
import static com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters.getSecondPreventiveExecutionCondition;
import static com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters.getSecondPreventiveHintFromFirstPreventiveRao;
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
    private final ReportNode secondPreventiveReportNode;

    private static final String SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT = "SecondPreventiveScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;

    public CastorSecondPreventive(final Crac crac,
                                  final RaoParameters raoParameters,
                                  final Network network,
                                  final StateTree stateTree,
                                  final ToolProvider toolProvider,
                                  final java.time.Instant targetEndInstant,
                                  final ReportNode secondPreventiveReportNode) {
        this.crac = crac;
        this.raoParameters = raoParameters;
        this.network = network;
        this.stateTree = stateTree;
        this.toolProvider = toolProvider;
        this.targetEndInstant = targetEndInstant;
        this.secondPreventiveReportNode = secondPreventiveReportNode;
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
            CastorReports.reportNotEnoughTimeToRunSecondPreventiveRao(secondPreventiveReportNode, targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
            return false;
        }
        if (getSecondPreventiveExecutionCondition(raoParameters).equals(SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE)
            && postFirstRaoResult.getCost(lastCurativeInstant) <= postFirstRaoResult.getCost(null)) {
            CastorReports.reportCostNotIncreasedDuringRao(secondPreventiveReportNode);
            // it is not necessary to compare initial & post-preventive costs since the preventive RAO cannot increase its own cost
            // only compare initial cost with the curative costs
            return false;
        }
        ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType = raoParameters.getObjectiveFunctionParameters().getType();
        if (objectiveFunctionType.equals(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW)) {
            if (firstPreventiveResult.getCost() > 0) {
                // in case of curative optimization even if preventive unsecure (see parameter enforce-curative-security)
                // we do not want to run a second preventive that would not be able to fix the situation, to save time
                CastorReports.reportSecondPreventiveCancelledToSaveComputationTime(secondPreventiveReportNode);
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
        return results.stream().anyMatch(postPerimeterResult -> postPerimeterResult.optimizationResult().getFunctionalCost() >= 0 || postPerimeterResult.optimizationResult().getVirtualCost() > 1e-6);
    }

    /**
     * Returns true if final cost (after PRAO + ARAO + CRAO) is worse than the cost at the end of the preventive perimeter
     */
    private static boolean isFinalCostWorseThanPreventive(double curativeMinObjImprovement, OptimizationResult preventiveResult, RaoResult postFirstRaoResult, Instant curativeInstant) {
        return postFirstRaoResult.getCost(curativeInstant) > preventiveResult.getCost() - curativeMinObjImprovement;
    }

    SecondPreventiveRaoResultsHolder runSecondPreventiveAndAutoRao(CastorContingencyScenarios castorContingencyScenarios,
                                                                   PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                                   PrePerimeterResult initialOutput,
                                                                   PostPerimeterResult firstPreventiveResult,
                                                                   Map<State, PostPerimeterResult> postContingencyResults) {
        // Run 2nd preventive RAO
        SecondPreventiveRaoResult secondPreventiveRaoResult;
        try {
            secondPreventiveRaoResult = runSecondPreventiveRao(prePerimeterSensitivityAnalysis, initialOutput, firstPreventiveResult.optimizationResult(), postContingencyResults);
            if (secondPreventiveRaoResult.postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                return SecondPreventiveRaoResultsHolder.failed("Post-PRA sensitivity analysis failed during 2nd preventive RAO");
            }
        } catch (OpenRaoException e) {
            CommonReports.reportExceptionMessage(secondPreventiveReportNode, e.getMessage());
            return SecondPreventiveRaoResultsHolder.failed(String.format("RAO failed during second preventive : %s", e.getMessage()));
        }

        // Run 2nd automaton simulation and update results
        final ReportNode secondAutomatonSimulationReportNode = CastorReports.reportSecondAutomatonSimulation(secondPreventiveReportNode);
        Map<State, PostPerimeterResult> newPostContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(network, secondPreventiveRaoResult.postPraSensitivityAnalysisOutput, true, secondAutomatonSimulationReportNode);
        CastorReports.reportSecondAutomatonSimulationEnd(secondAutomatonSimulationReportNode);

        CastorReports.reportMergingFirstSecondPreventiveAndPostContingencyRaoResults(secondPreventiveReportNode);
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
                !(entry.getValue().optimizationResult() instanceof SkippedOptimizationResultImpl) && entry.getKey().getInstant().isAuto())
            .forEach(entry -> {
                appliedArasAndCras.addAppliedNetworkActions(entry.getKey(), entry.getValue().optimizationResult().getActivatedNetworkActions());
                entry.getValue().optimizationResult().getActivatedRangeActions(entry.getKey()).forEach(rangeAction -> appliedArasAndCras.addAppliedRangeAction(entry.getKey(), rangeAction, entry.getValue().optimizationResult().getOptimizedSetpoint(rangeAction, entry.getKey())));
            });
        // Run curative sensitivity analysis with appliedArasAndCras
        // TODO: this is too slow, we can replace it with load-flow computations or security analysis since we don't need sensitivity values
        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, Collections.emptySet(), appliedArasAndCras, secondPreventiveReportNode);
        if (postCraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorReports.reportSystematicSensitivityAnalysisAfterCraAfterSecondPreventiveFailed(secondPreventiveReportNode);
            return SecondPreventiveRaoResultsHolder.failed("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        }
        for (Map.Entry<State, PostPerimeterResult> entry : postContingencyResults.entrySet()) {
            State state = entry.getKey();
            if (!state.getInstant().isCurative()) {
                continue;
            }
            // Specific case : curative state was previously skipped because it led to a sensitivity analysis failure.
            // Curative state is still a SkippedOptimizationResultImpl, but its computation status must be updated
            if (entry.getValue().optimizationResult() instanceof SkippedOptimizationResultImpl) {
                OptimizationResult skippedResult = new SkippedOptimizationResultImpl(state, new HashSet<>(), new HashSet<>(), postCraSensitivityAnalysisOutput.getSensitivityStatus(entry.getKey()), getSensitivityFailureOvercost(raoParameters));
                PrePerimeterResult prePerimeterResult = new PrePerimeterSensitivityResultImpl(skippedResult, skippedResult, null, skippedResult);
                newPostContingencyResults.put(state, new PostPerimeterResult(skippedResult, prePerimeterResult));
            } else {
                newPostContingencyResults.put(state, new PostPerimeterResult(
                    new CurativeWithSecondPraoResult(state, entry.getValue().optimizationResult(), secondPreventiveRaoResult.perimeterResult(), postCraSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType().costOptimization()),
                    postCraSensitivityAnalysisOutput
                ));
            }
        }
        MostLimitingElementsReports.reportBusinessMostLimitingElements(secondPreventiveReportNode, postCraSensitivityAnalysisOutput, postCraSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
        CastorReports.reportIfMostLimitingElementIsFictional(secondPreventiveReportNode, postCraSensitivityAnalysisOutput);

        return new SecondPreventiveRaoResultsHolder(secondPreventiveRaoResult, newPostContingencyResults, false, null);
    }

    record SecondPreventiveRaoResult(OptimizationResult perimeterResult,
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
        PrePerimeterResult sensiWithPostContingencyRemedialActions = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), appliedArasAndCras, secondPreventiveReportNode);
        if (sensiWithPostContingencyRemedialActions.getSensitivityStatus() == FAILURE) {
            throw new OpenRaoException("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        }
        CastorReports.reportCastorSystematicSensitivityAnalysisAfterCraResults(secondPreventiveReportNode,
            prePerimeterSensitivityAnalysis.getObjectiveFunction(),
            new RemedialActionActivationResultImpl(new RangeActionActivationResultImpl(RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, crac.getRangeActions())), new NetworkActionsResultImpl(getAllAppliedNetworkAraAndCra(appliedArasAndCras))),
            sensiWithPostContingencyRemedialActions,
            raoParameters,
            NUMBER_LOGGED_ELEMENTS_DURING_RAO);

        // Run second preventive RAO
        CastorReports.reportSecondPreventivePerimeterOptimizationStart(secondPreventiveReportNode);
        String newVariant = RandomizedString.getRandomizedString("SecondPreventive", network.getVariantManager().getVariantIds(), 10);
        network.getVariantManager().cloneVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT, newVariant, true);
        network.getVariantManager().setWorkingVariant(newVariant);
        OptimizationResult secondPreventiveResult = optimizeSecondPreventivePerimeter(initialOutput, sensiWithPostContingencyRemedialActions, firstPreventiveResult, appliedArasAndCras).join();
        // Re-run sensitivity computation based on PRAs without CRAs, to access after PRA results
        PrePerimeterResult postPraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialOutput, stateTree.getOperatorsNotSharingCras(), null, secondPreventiveReportNode);
        if (postPraSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
            CastorReports.reportSystematicSensitivityAnalysisAfterPraAfterSecondPreventiveFailed(secondPreventiveReportNode);
        }
        CastorReports.reportSecondPreventivePerimeterOptimizationEnd(secondPreventiveReportNode);
        return new SecondPreventiveRaoResult(secondPreventiveResult, postPraSensitivityAnalysisOutput, appliedArasAndCras);
    }

    void addAppliedNetworkActionsPostContingency(Set<Instant> instants, AppliedRemedialActions appliedRemedialActions, Map<State, PostPerimeterResult> postContingencyResults) {
        instants.forEach(instant ->
            postContingencyResults.forEach((state, postPerimeterResult) -> {
                if (state.getInstant().equals(instant)) {
                    appliedRemedialActions.addAppliedNetworkActions(state, postPerimeterResult.optimizationResult().getActivatedNetworkActions());
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
                    postPerimeterResult.optimizationResult().getActivatedRangeActions(state).forEach(rangeAction ->
                        appliedRemedialActions.addAppliedRangeAction(state, rangeAction, postPerimeterResult.optimizationResult().getOptimizedSetpoint(rangeAction, state)));
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

    private CompletableFuture<OptimizationResult> optimizeSecondPreventivePerimeter(PrePerimeterResult initialOutput,
                                                                                    PrePerimeterResult prePerimeterResult,
                                                                                    OptimizationResult firstPreventiveResult,
                                                                                    AppliedRemedialActions appliedCras) {

        OptimizationPerimeter optPerimeter;
        State preventiveState = crac.getPreventiveState();

        optPerimeter = GlobalOptimizationPerimeter.build(crac, network, raoParameters, prePerimeterResult, secondPreventiveReportNode);

        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(secondPreventiveReportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForSecondPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters(secondPreventiveReportNode);
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

        OptimizationResult result = new SearchTree(searchTreeInput, searchTreeParameters, true, secondPreventiveReportNode).run().join();

        // apply PRAs
        network.getVariantManager().setWorkingVariant(SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        // network actions need to be applied BEFORE range actions because to apply HVDC range actions we need to apply AC emulation deactivation network actions beforehand
        result.getActivatedNetworkActions().forEach(networkAction -> networkAction.apply(network));
        result.getActivatedRangeActions(preventiveState).forEach(rangeAction -> rangeAction.apply(network, result.getOptimizedSetpoint(rangeAction, preventiveState)));

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Helper record class used to expose the elementary results (second preventive RAO result and post-contingency
     * results per state) coming from a run of a global second-preventive RAO.
     */
    public record SecondPreventiveRaoResultsHolder(SecondPreventiveRaoResult secondPreventiveRaoResult,
                                                   Map<State, PostPerimeterResult> postContingencyResults,
                                                   boolean hasFailed, String errorMessage) {
        public static SecondPreventiveRaoResultsHolder failed(String errorMessage) {
            return new SecondPreventiveRaoResultsHolder(null, null, true, errorMessage);
        }
    }
}
