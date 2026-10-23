/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.reports.MostLimitingElementsReports;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OptimizationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PreventiveAndCurativesRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RangeActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.UnoptimizedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.SearchTree;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.jspecify.annotations.NonNull;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.getFlowUnit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CastorFullOptimization {
    private static final String INITIAL_SCENARIO = "InitialScenario";
    private static final String PREVENTIVE_SCENARIO = "PreventiveScenario";
    private static final String SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT = "SecondPreventiveScenario";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;
    private static final double EPSILON = 1e-6;

    private final RaoInput raoInput;
    private final Crac crac;
    private final Network network;
    private final RaoParameters raoParameters;
    private final java.time.Instant targetEndInstant;
    private final ReportNode reportNode;

    CastorFullOptimization(final RaoInput raoInput, final RaoParameters raoParameters, final java.time.Instant targetEndInstant, final ReportNode reportNode) {
        this.raoInput = raoInput;
        this.crac = raoInput.getCrac();
        this.network = raoInput.getNetwork();
        this.raoParameters = raoParameters;
        this.targetEndInstant = targetEndInstant;
        this.reportNode = reportNode;
    }

    CompletableFuture<RaoResult> run() {
        final ReportNode optimizationReportNode = CastorReports.reportCastorFullOptimization(reportNode);

        String currentStep = "data initialization";
        String initialVariantName = network.getVariantManager().getWorkingVariantId();

        try {
            RaoRunContext context = createContext();
            if (crac.getFlowCnecs().isEmpty()) {
                PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, context.getToolProvider(), true)
                    .runInitialSensitivityAnalysis(network, optimizationReportNode);
                return CompletableFuture.completedFuture(new UnoptimizedRaoResultImpl(initialResult));
            }
            prepareInitialSetup(context, optimizationReportNode);

            currentStep = "initial sensitivity analysis";
            runInitialSensitivityAnalyses(context, optimizationReportNode);
            if (context.getInitialOutput().getSensitivityStatus() == ComputationStatus.FAILURE) {
                CommonReports.reportInitialSensitivityAnalysisFailed(optimizationReportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
            }
            CastorReports.reportCastorInitialSensitivityAnalysisResults(optimizationReportNode,
                context.getPrePerimeterSensitivityAnalysis().getObjectiveFunction(),
                RemedialActionActivationResultImpl.empty(context.getInitialOutput()),
                context.getInitialOutput(),
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
            // run search tree on preventive perimeter
            currentStep = "first preventive";

            final ReportNode preventivePerimeterOptimReportNode = CastorReports.reportPreventivePerimeterOptimization(optimizationReportNode);
            runFirstPreventiveOptimization(context, preventivePerimeterOptimReportNode);
            CastorReports.reportPreventivePerimeterOptimizationEnd();

            // ----- SENSI POST-PRA -----
            // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs
            currentStep = "post-PRA sensitivity analysis";

            final ReportNode postPraSensiAnalysisReportNode = CastorReports.reportPostPraSensiAnalysis(optimizationReportNode);
            computePostPraSensitivity(context, postPraSensiAnalysisReportNode);
            if (context.getPreCurativeSensitivityAnalysisOutput().getSensitivityStatus() == ComputationStatus.FAILURE) {
                CastorReports.reportSystematicSensitivityAnalysisAfterPraFailed(postPraSensiAnalysisReportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
            }
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterPraResults(optimizationReportNode,
                context.getPrePerimeterSensitivityAnalysis().getObjectiveFunction(),
                new RemedialActionActivationResultImpl(context.getPreventiveResult(), context.getPreventiveResult()),
                context.getPreCurativeSensitivityAnalysisOutput(),
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            if (context.getStateTree().getContingencyScenarios().isEmpty()) {
                // log final result
                MostLimitingElementsReports.reportTechnicalMostLimitingElements(
                    preventivePerimeterOptimReportNode,
                    context.getPreventiveResult(),
                    context.getPreventiveResult(),
                    null,
                    raoParameters.getObjectiveFunctionParameters().getType(),
                    getFlowUnit(raoParameters),
                    10
                );
                CastorReports.reportIfMostLimitingElementIsFictional(preventivePerimeterOptimReportNode, context.getPreventiveResult());
                RaoResult raoResult = new PreventiveAndCurativesRaoResultImpl(context.getStateTree(), context.getInitialOutput(),
                        context.getPostPreventiveResult(), crac, raoParameters, preventivePerimeterOptimReportNode);
                return postCheckResults(raoResult, context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
            }

            RaoResult mergedRaoResults;

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            // optimize contingency scenarios (auto + curative instants)
            currentStep = "contingency scenarios";

            final ReportNode curativePerimeterOptimReportNode = CastorReports.reportCurativePerimeterOptimization(optimizationReportNode);

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter enforce-curative-if-basecase-unsecure)
            if (shouldStopOptimisationIfPreventiveUnsecure(context.getPreventiveResult().getCost())) {
                CastorReports.reportPreventivePerimeterNotSecure(curativePerimeterOptimReportNode);
                mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(
                        context.getStateTree(), context.getInitialOutput(), context.getPostPreventiveResult(), crac, raoParameters, curativePerimeterOptimReportNode);
                // log results
                MostLimitingElementsReports.reportBusinessMostLimitingElements(
                    curativePerimeterOptimReportNode,
                    context.getPreCurativeSensitivityAnalysisOutput(),
                    context.getPreCurativeSensitivityAnalysisOutput(),
                    raoParameters.getObjectiveFunctionParameters().getType(),
                    getFlowUnit(raoParameters),
                    NUMBER_LOGGED_ELEMENTS_END_RAO
                );
                CastorReports.reportIfMostLimitingElementIsFictional(curativePerimeterOptimReportNode, context.getPreCurativeSensitivityAnalysisOutput());
                return postCheckResults(mergedRaoResults, context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
            }

            final ReportNode postContingencyPerimeterOptimReportNode = CastorReports.reportPostContingencyPerimeterOptimization(curativePerimeterOptimReportNode);
            runCurativePerimeterOptimization(context, postContingencyPerimeterOptimReportNode);
            CastorReports.reportPostContingencyPerimeterOptimizationEnd();

            // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
            currentStep = "second preventive optimization";

            runSecondPreventiveOptimization(context, optimizationReportNode);
            // Log final results
            if (context.isLogFinalResultsOutsideOfSecondPreventive()) {
                final ReportNode finalResultsReportNode = CastorReports.reportMergingPreventiveAndPostContingencyRaoResults(optimizationReportNode);
                MostLimitingElementsReports.reportBusinessMostLimitingElements(
                    finalResultsReportNode,
                    context.getStateTree().getBasecaseScenario(),
                    context.getFinalSecondPreventiveResult().optimizationResult(),
                    context.getStateTree().getContingencyScenarios(),
                    context.getFinalPostContingencyResults(),
                    raoParameters.getObjectiveFunctionParameters().getType(),
                    getFlowUnit(raoParameters),
                    NUMBER_LOGGED_ELEMENTS_END_RAO
                );
                CastorReports.reportIfMostLimitingElementIsFictional(
                    finalResultsReportNode,
                    context.getStateTree().getBasecaseScenario(),
                    context.getFinalSecondPreventiveResult().optimizationResult(),
                    context.getStateTree().getContingencyScenarios(),
                    context.getFinalPostContingencyResults(),
                    raoParameters.getObjectiveFunctionParameters().getType(),
                    getFlowUnit(raoParameters)
                );
            }

            return postCheckResults(context.getMergedRaoResults(), context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
        } catch (Exception e) {
            CastorReports.reportExceptionMessageAndStacktrace(optimizationReportNode, e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(String.format("RAO failed during %s : %s", currentStep, e.getMessage())));
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariantName);
        }
    }

    private @NonNull RaoRunContext createContext() {
        return new RaoRunContext(ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters));
    }

    private void prepareInitialSetup(final RaoRunContext context, final ReportNode optimizationReportNode) {
        context.setStateTree(new StateTree(crac, optimizationReportNode));
    }

    /**
     * Compute initial sensitivity on all CNECs (necessary to have initial flows for MNEC and loopflow
     * constraints on CNECs, in preventive and curative perimeters).
     */
    private void runInitialSensitivityAnalyses(final RaoRunContext context, final ReportNode optimizationReportNode) {
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, context.getToolProvider(), true);
        context.setPrePerimeterSensitivityAnalysis(prePerimeterSensitivityAnalysis);

        context.setInitialOutput(prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, optimizationReportNode));
    }

    private void runFirstPreventiveOptimization(final RaoRunContext context, final ReportNode preventivePerimeterOptimReportNode) {
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();

        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

        PrePerimeterResult initialOutput = context.getInitialOutput();
        OptimizationResult preventiveResult;
        if (Objects.isNull(crac.getPreventiveState())) {
            ObjectiveFunction objectiveFunction = ObjectiveFunction.buildForInitialSensitivityComputation(Collections.emptySet(), raoParameters, Collections.emptySet());
            preventiveResult = new OptimizationResultImpl(
                objectiveFunction.evaluate(initialOutput, RemedialActionActivationResultImpl.empty(initialOutput), reportNode),
                initialOutput,
                initialOutput,
                new NetworkActionsResultImpl(Collections.emptyMap()),
                new RangeActionActivationResultImpl(initialOutput)
            );
        } else {
            preventiveResult = optimizePreventivePerimeter(context.getStateTree(), context.getToolProvider(), initialOutput, preventivePerimeterOptimReportNode);
        }
        java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
        context.setPreventiveRaoTime(ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant));

        context.setPreventiveResult(preventiveResult);
    }

    private void computePostPraSensitivity(final RaoRunContext context, final ReportNode postPraSensiAnalysisReportNode) {
        PrePerimeterResult initialOutput = context.getInitialOutput();
        OptimizationResult preventiveResult = context.getPreventiveResult();

        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
        network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        applyRemedialActions(network, preventiveResult, crac.getPreventiveState());

        PostPerimeterResult postPreventiveResult = new PostPerimeterSensitivityAnalysis(
                crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, context.getToolProvider(), true)
            .runBasedOnInitialPreviousAndOptimizationResults(
                    network, initialOutput, initialOutput, Collections.emptySet(),
                    preventiveResult, null, postPraSensiAnalysisReportNode);
        context.setPostPreventiveResult(postPreventiveResult);
        context.setPreCurativeSensitivityAnalysisOutput(postPreventiveResult.prePerimeterResultForAllFollowingStates());
    }

    private void runCurativePerimeterOptimization(final RaoRunContext context, final ReportNode postContingencyPerimeterOptimReportNode) {
        TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, context.getPreventiveResult().getCost());
        CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(
                crac, raoParameters, context.getToolProvider(), context.getStateTree(), curativeTreeParameters, context.getInitialOutput());
        context.setCastorContingencyScenarios(castorContingencyScenarios);

        context.setPostContingencyResults(castorContingencyScenarios.optimizeContingencyScenarios(
            network,
            context.getPreCurativeSensitivityAnalysisOutput(),
            false,
            postContingencyPerimeterOptimReportNode
        ));
    }

    /**
     * Run the second preventive RAO (and, where relevant, re-run auto/curative perimeters) when the first
     * preventive and post-contingency results warrant it, and merge the results. Falls back to the first
     * preventive + post-contingency merged result when second preventive does not improve it.
     */
    private void runSecondPreventiveOptimization(final RaoRunContext context, final ReportNode optimizationReportNode) {
        StateTree stateTree = context.getStateTree();
        PrePerimeterResult initialOutput = context.getInitialOutput();
        PostPerimeterResult postPreventiveResult = context.getPostPreventiveResult();
        Map<State, PostPerimeterResult> postContingencyResults = context.getPostContingencyResults();

        final ReportNode secondPreventivePerimeterOptimReportNode = CastorReports.reportSecondPreventivePerimeterOptimization(optimizationReportNode);
        RaoResult mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(
            stateTree,
            initialOutput,
            postPreventiveResult,
            postContingencyResults,
            crac,
            raoParameters,
            secondPreventivePerimeterOptimReportNode
        );
        context.setLogFinalResultsOutsideOfSecondPreventive(true);
        // Run second preventive when necessary
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(
            crac,
            raoParameters,
            network,
            stateTree,
            context.getToolProvider(),
            targetEndInstant,
            secondPreventivePerimeterOptimReportNode
        );

        // define variables to set with second preventive results only if it improves first
        PostPerimeterResult finalSecondPreventiveResult = postPreventiveResult;
        PostPerimeterResult intermediateSecondPreventiveResult = postPreventiveResult;
        Map<State, PostPerimeterResult> finalPostContingencyResults = new HashMap<>(postContingencyResults);

        if (castorSecondPreventive.shouldRunSecondPreventiveRao(context.getPreventiveResult(), postContingencyResults.values(), mergedRaoResults, context.getPreventiveRaoTime())) {
            CastorSecondPreventive.SecondPreventiveRaoResultsHolder secondPreventiveRaoResultsHolder = castorSecondPreventive.runSecondPreventiveAndAutoRao(
                context.getCastorContingencyScenarios(), context.getPrePerimeterSensitivityAnalysis(), initialOutput, postPreventiveResult, postContingencyResults);
            RaoResult secondPreventiveRaoResults;
            if (secondPreventiveRaoResultsHolder.hasFailed()) {
                secondPreventiveRaoResults = new FailedRaoResultImpl(secondPreventiveRaoResultsHolder.errorMessage());
            } else {
                intermediateSecondPreventiveResult = new PostPerimeterResult(
                    secondPreventiveRaoResultsHolder.secondPreventiveRaoResult().perimeterResult(),
                    secondPreventiveRaoResultsHolder.secondPreventiveRaoResult().postPraSensitivityAnalysisOutput()
                );
                secondPreventiveRaoResults = new PreventiveAndCurativesRaoResultImpl(
                    stateTree,
                    initialOutput,
                    postPreventiveResult,
                    intermediateSecondPreventiveResult,
                    secondPreventiveRaoResultsHolder.postContingencyResults(),
                    crac,
                    raoParameters,
                    secondPreventivePerimeterOptimReportNode);
            }
            if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults, secondPreventivePerimeterOptimReportNode)) {
                finalSecondPreventiveResult = intermediateSecondPreventiveResult;
                finalPostContingencyResults = new HashMap<>(secondPreventiveRaoResultsHolder.postContingencyResults());
                mergedRaoResults = secondPreventiveRaoResults;
                mergedRaoResults.setExecutionDetails(OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST);
                context.setLogFinalResultsOutsideOfSecondPreventive(false);
            } else {
                mergedRaoResults.setExecutionDetails(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
            }
        }

        context.setMergedRaoResults(mergedRaoResults);
        context.setFinalSecondPreventiveResult(finalSecondPreventiveResult);
        context.setFinalPostContingencyResults(finalPostContingencyResults);
    }

    private boolean shouldStopOptimisationIfPreventiveUnsecure(double preventiveOptimalCost) {
        return raoParameters.getObjectiveFunctionParameters().getType().equals(ObjectiveFunctionParameters.ObjectiveFunctionType.SECURE_FLOW)
            && preventiveOptimalCost > 0
            && !raoParameters.getObjectiveFunctionParameters().getEnforceCurativeSecurity();
    }

    /**
     * Return true if 2P has decreased cost
     */
    private boolean secondPreventiveImprovesResults(final RaoResult secondPreventiveRaoResults,
                                                    final RaoResult mergedRaoResults,
                                                    final ReportNode secondPreventiveReportNode) {
        if (secondPreventiveRaoResults instanceof FailedRaoResultImpl) {
            CastorReports.reportSecondPreventiveFailed(secondPreventiveReportNode);
            return false;
        }
        if (mergedRaoResults.getComputationStatus() == ComputationStatus.FAILURE && secondPreventiveRaoResults.getComputationStatus() != ComputationStatus.FAILURE) {
            CastorReports.reportSecondPreventiveMadeRaoSucceed(secondPreventiveReportNode);
            return true;
        }
        Instant curativeInstant = crac.getLastInstant();
        double firstPreventiveCost = mergedRaoResults.getCost(curativeInstant);
        double secondPreventiveCost = secondPreventiveRaoResults.getCost(curativeInstant);
        if (secondPreventiveCost > firstPreventiveCost) {
            CastorReports.reportSecondPreventiveIncreasedOverallCost(
                secondPreventiveReportNode, firstPreventiveCost, secondPreventiveCost, curativeInstant, mergedRaoResults, secondPreventiveRaoResults
            );
            return false;
        }
        return true;
    }

    /**
     * Return initial result if RAO has increased cost and handleCostIncrease is set to true
     */
    private CompletableFuture<RaoResult> postCheckResults(final RaoResult raoResult,
                                                          final PrePerimeterResult initialResult,
                                                          final ObjectiveFunctionParameters objectiveFunctionParameters,
                                                          final boolean handleCostIncrease,
                                                          final ReportNode optimizationReportNode) {
        RaoResult finalRaoResult = raoResult;

        double initialCost = initialResult.getCost();
        double initialFunctionalCost = initialResult.getFunctionalCost();
        double initialVirtualCost = initialResult.getVirtualCost();
        Instant lastInstant = crac.getLastInstant();
        double finalCost = finalRaoResult.getCost(lastInstant);
        double finalFunctionalCost = finalRaoResult.getFunctionalCost(lastInstant);
        double finalVirtualCost = finalRaoResult.getVirtualCost(lastInstant);

        if (handleCostIncrease && finalCost > initialCost + EPSILON) {
            CastorReports.reportRaoIncreasedOverallCost(optimizationReportNode, initialCost, initialFunctionalCost, initialVirtualCost, finalCost, finalFunctionalCost, finalVirtualCost);
            // log results
            MostLimitingElementsReports.reportBusinessMostLimitingElements(
                optimizationReportNode,
                initialResult,
                initialResult,
                objectiveFunctionParameters.getType(),
                getFlowUnit(raoParameters),
                NUMBER_LOGGED_ELEMENTS_END_RAO
            );
            finalRaoResult = new UnoptimizedRaoResultImpl(initialResult);
            finalCost = initialCost;
            finalFunctionalCost = initialFunctionalCost;
            finalVirtualCost = initialVirtualCost;
            if (raoResult.getExecutionDetails().equals(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY)) {
                finalRaoResult.setExecutionDetails(OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            } else {
                finalRaoResult.setExecutionDetails(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION);
            }
        }

        // Log costs before and after RAO
        CastorReports.reportCostsBeforeAndAfterRao(
            optimizationReportNode,
            initialCost,
            initialFunctionalCost,
            initialVirtualCost,
            initialResult,
            finalCost,
            finalFunctionalCost,
            finalVirtualCost,
            finalRaoResult,
            crac.getLastInstant()
        );

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private OptimizationResult optimizePreventivePerimeter(final StateTree stateTree,
                                                                  final ToolProvider toolProvider,
                                                                  final PrePerimeterResult initialResult,
                                                                  final ReportNode preventivePerimeterOptimReportNode) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(
            stateTree.getBasecaseScenario(),
            crac,
            network,
            raoParameters,
            initialResult,
            preventivePerimeterOptimReportNode
        );

        SearchTreeParameters.SearchTreeParametersBuilder searchTreeParametersBuilder = SearchTreeParameters.create(reportNode)
            .withConstantParametersOverAllRao(raoParameters, crac)
            .withTreeParameters(TreeParameters.buildForPreventivePerimeter(raoParameters))
            .withUnoptimizedCnecParameters(UnoptimizedCnecParameters.build(raoParameters.getNotOptimizedCnecsParameters(), stateTree.getOperatorsNotSharingCras()));

        if (!getHvdcRangeActionsOnHvdcLineInAcEmulation(crac.getHvdcRangeActions(), network).isEmpty()) {
            LoadFlowAndSensitivityParameters loadFlowAndSensitivityParameters =
                raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)
                    ? raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getLoadFlowAndSensitivityParameters()
                    : new LoadFlowAndSensitivityParameters(preventivePerimeterOptimReportNode);
            searchTreeParametersBuilder.withLoadFlowAndSensitivityParameters(loadFlowAndSensitivityParameters);
        }

        SearchTreeParameters searchTreeParameters = searchTreeParametersBuilder.build();

        Set<State> statesToOptimize = new HashSet<>(optPerimeter.getMonitoredStates());
        statesToOptimize.add(optPerimeter.getMainOptimizationState());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            optPerimeter.getFlowCnecs(),
            optPerimeter.getLoopFlowCnecs(),
            initialResult,
            initialResult,
            Collections.emptySet(),
            raoParameters,
            statesToOptimize
        );
        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true, preventivePerimeterOptimReportNode).run().join();
        applyRemedialActions(network, optResult, crac.getPreventiveState());
        return optResult;
    }
}
