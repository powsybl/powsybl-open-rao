/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
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
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.RaoResultGenerator;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
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
        Unit flowUnit = RaoUtil.getFlowUnit(raoParameters); // TODO: use as attribute

        try {
            boolean costOptimization = raoParameters.getObjectiveFunctionParameters().getType().costOptimization();

            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
            if (crac.getFlowCnecs().isEmpty()) {
                PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                    .runInitialSensitivityAnalysis(network, optimizationReportNode);
                RaoResult raoResult = RaoResultGenerator.empty(crac, OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY, initialResult, raoParameters);
                return CompletableFuture.completedFuture(raoResult);
            }
            StateTree stateTree = new StateTree(crac, optimizationReportNode);

            currentStep = "initial sensitivity analysis";
            // ----- INITIAL SENSI -----
            // compute initial sensitivity on all CNECs
            // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(),
                crac.getRangeActions(), raoParameters, toolProvider, true);

            PrePerimeterResult initialOutput;
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, optimizationReportNode);
            if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                CommonReports.reportInitialSensitivityAnalysisFailed(optimizationReportNode);
                RaoResult raoResult = RaoResultGenerator.failed(crac, "Initial sensitivity analysis failed");
                return CompletableFuture.completedFuture(raoResult);
            }
            CastorReports.reportCastorInitialSensitivityAnalysisResults(optimizationReportNode,
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                RemedialActionActivationResultImpl.empty(initialOutput),
                initialOutput,
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
            // run search tree on preventive perimeter
            currentStep = "first preventive";
            java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();

            final ReportNode preventivePerimeterOptimReportNode = CastorReports.reportPreventivePerimeterOptimization(optimizationReportNode);

            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

            OptimizationResult preventiveResult = optimizePreventivePerimeter(stateTree, toolProvider, initialOutput, preventivePerimeterOptimReportNode);
            CastorReports.reportPreventivePerimeterOptimizationEnd();
            java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
            long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

            // ----- SENSI POST-PRA -----
            // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs
            currentStep = "post-PRA sensitivity analysis";

            final ReportNode postPraSensiAnalysisReportNode = CastorReports.reportPostPraSensiAnalysis(optimizationReportNode);

            network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
            applyRemedialActions(network, preventiveResult, crac.getPreventiveState());

            PostPerimeterResult postPreventiveResult = computePostPreventiveResult(toolProvider, initialOutput, preventiveResult, postPraSensiAnalysisReportNode);
            PrePerimeterResult preCurativeSensitivityAnalysisOutput = postPreventiveResult.prePerimeterResultForAllFollowingStates();
            if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                CastorReports.reportSystematicSensitivityAnalysisAfterPraFailed(postPraSensiAnalysisReportNode);
                RaoResult raoResult = RaoResultGenerator.failed(crac, "Systematic sensitivity analysis after preventive remedial actions failed");
                return CompletableFuture.completedFuture(raoResult);
            }
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterPraResults(optimizationReportNode,
                prePerimeterSensitivityAnalysis.getObjectiveFunction(), new RemedialActionActivationResultImpl(preventiveResult, preventiveResult),
                preCurativeSensitivityAnalysisOutput, raoParameters, NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            if (stateTree.getContingencyScenarios().isEmpty()) {
                return generateRaoResultWithPrasOnly(preventivePerimeterOptimReportNode, preventiveResult, stateTree, initialOutput,
                    postPreventiveResult, preCurativeSensitivityAnalysisOutput, costOptimization, flowUnit, optimizationReportNode);
            }

            RaoResult mergedRaoResults;

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            // optimize contingency scenarios (auto + curative instants)
            currentStep = "contingency scenarios";

            final ReportNode curativePerimeterOptimReportNode = CastorReports.reportCurativePerimeterOptimization(optimizationReportNode);

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter enforce-curative-if-basecase-unsecure)
            double preventiveOptimalCost = preventiveResult.getCost();
            if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
                return generateUnsecureRaoResultWithPrasOnly(curativePerimeterOptimReportNode, stateTree, initialOutput, postPreventiveResult,
                    preCurativeSensitivityAnalysisOutput, preventiveResult, costOptimization, flowUnit, optimizationReportNode);
            }

            final ReportNode postContingencyPerimeterOptimReportNode = CastorReports.reportPostContingencyPerimeterOptimization(curativePerimeterOptimReportNode);
            TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, preventiveOptimalCost);
            CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(crac, raoParameters, toolProvider, stateTree, curativeTreeParameters, initialOutput);
            Map<State, PostPerimeterResult> postContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(
                network, preCurativeSensitivityAnalysisOutput, false, postContingencyPerimeterOptimReportNode);
            CastorReports.reportPostContingencyPerimeterOptimizationEnd();

            // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
            currentStep = "second preventive optimization";

            final ReportNode secondPreventivePerimeterOptimReportNode = CastorReports.reportSecondPreventivePerimeterOptimization(optimizationReportNode);
            mergedRaoResults = RaoResultGenerator.preventiveAndCurative(
                crac,
                OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY,
                initialOutput,
                postPreventiveResult,
                postContingencyResults,
                raoParameters,
                stateTree,
                secondPreventivePerimeterOptimReportNode
            );

            boolean logFinalResultsOutsideOfSecondPreventive = true;
            // Run second preventive when necessary
            CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(
                crac, raoParameters, network, stateTree, toolProvider, targetEndInstant, secondPreventivePerimeterOptimReportNode);

            // define variables to set with second preventive results only if it improves first
            PostPerimeterResult finalSecondPreventiveResult = postPreventiveResult;
            PostPerimeterResult intermediateSecondPreventiveResult = postPreventiveResult;
            Map<State, PostPerimeterResult> finalPostContingencyResults = new HashMap<>(postContingencyResults);

            boolean secondPreventiveRan = false;
            String executionDetails = OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY;
            if (castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, postContingencyResults.values(), mergedRaoResults, preventiveRaoTime)) {
                CastorSecondPreventive.SecondPreventiveRaoResultsHolder secondPreventiveRaoResultsHolder = castorSecondPreventive.runSecondPreventiveAndAutoRao(
                    castorContingencyScenarios, prePerimeterSensitivityAnalysis, initialOutput, postPreventiveResult, postContingencyResults);
                secondPreventiveRan = true;
                RaoResult secondPreventiveRaoResults;
                if (secondPreventiveRaoResultsHolder.hasFailed()) {
                    secondPreventiveRaoResults = RaoResultGenerator.failed(crac, secondPreventiveRaoResultsHolder.errorMessage());
                } else {
                    intermediateSecondPreventiveResult = new PostPerimeterResult(
                        secondPreventiveRaoResultsHolder.secondPreventiveRaoResult().perimeterResult(),
                        secondPreventiveRaoResultsHolder.secondPreventiveRaoResult().postPraSensitivityAnalysisOutput()
                    );
                    secondPreventiveRaoResults = RaoResultGenerator.preventiveAndCurative(
                        crac,
                        OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST,
                        initialOutput,
                        intermediateSecondPreventiveResult,
                        secondPreventiveRaoResultsHolder.postContingencyResults(),
                        raoParameters,
                        stateTree,
                        secondPreventivePerimeterOptimReportNode
                    );
                }

                if (secondPreventiveImprovesResults(secondPreventiveRaoResults, mergedRaoResults, secondPreventivePerimeterOptimReportNode)) {
                    finalSecondPreventiveResult = intermediateSecondPreventiveResult;
                    finalPostContingencyResults = new HashMap<>(secondPreventiveRaoResultsHolder.postContingencyResults());
                    mergedRaoResults = secondPreventiveRaoResults;
                    executionDetails = OptimizationStepsExecuted.SECOND_PREVENTIVE_IMPROVED_FIRST;
                    logFinalResultsOutsideOfSecondPreventive = false;
                } else {
                    executionDetails = OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION;
                }
            }
            // Log final results
            if (logFinalResultsOutsideOfSecondPreventive) {
                logFinalResults(optimizationReportNode, stateTree, finalSecondPreventiveResult, finalPostContingencyResults);
            }

            Metadata metadata = mergedRaoResults.getExtension(Metadata.class);
            metadata.setExecutionDetails(executionDetails);

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), true,
                optimizationReportNode, flowUnit, secondPreventiveRan);
        } catch (Exception e) {
            CastorReports.reportExceptionMessageAndStacktrace(optimizationReportNode, e);
            String failureReason = String.format("RAO failed during %s : %s", currentStep, e.getMessage());
            RaoResult raoResult = RaoResultGenerator.failed(crac, failureReason);
            return CompletableFuture.completedFuture(raoResult);
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariantName);
        }
    }

    private void logFinalResults(ReportNode optimizationReportNode,
                                 StateTree stateTree,
                                 PostPerimeterResult finalSecondPreventiveResult,
                                 Map<State, PostPerimeterResult> finalPostContingencyResults) {
        final ReportNode finalResultsReportNode = CastorReports.reportMergingPreventiveAndPostContingencyRaoResults(optimizationReportNode);
        MostLimitingElementsReports.reportBusinessMostLimitingElements(
            finalResultsReportNode,
            stateTree.getBasecaseScenario(),
            finalSecondPreventiveResult.optimizationResult(),
            stateTree.getContingencyScenarios(),
            finalPostContingencyResults,
            raoParameters.getObjectiveFunctionParameters().getType(),
            getFlowUnit(raoParameters),
            NUMBER_LOGGED_ELEMENTS_END_RAO
        );
        CastorReports.reportIfMostLimitingElementIsFictional(
            finalResultsReportNode,
            stateTree.getBasecaseScenario(),
            finalSecondPreventiveResult.optimizationResult(),
            stateTree.getContingencyScenarios(),
            finalPostContingencyResults,
            raoParameters.getObjectiveFunctionParameters().getType(),
            getFlowUnit(raoParameters)
        );
    }

    private @NonNull CompletableFuture<RaoResult> generateRaoResultWithPrasOnly(ReportNode preventivePerimeterOptimReportNode,
                                                                                OptimizationResult preventiveResult,
                                                                                StateTree stateTree,
                                                                                PrePerimeterResult initialOutput,
                                                                                PostPerimeterResult postPreventiveResult,
                                                                                PrePerimeterResult preCurativeSensitivityAnalysisOutput,
                                                                                boolean costOptimization,
                                                                                Unit flowUnit,
                                                                                ReportNode optimizationReportNode) {
        // log final result
        MostLimitingElementsReports.reportTechnicalMostLimitingElements(
            preventivePerimeterOptimReportNode,
            preventiveResult,
            preventiveResult,
            null,
            raoParameters.getObjectiveFunctionParameters().getType(),
            getFlowUnit(raoParameters),
            NUMBER_LOGGED_ELEMENTS_END_RAO
        );
        CastorReports.reportIfMostLimitingElementIsFictional(preventivePerimeterOptimReportNode, preventiveResult);
        RaoResult raoResult = RaoResultGenerator.preventive(crac, initialOutput, postPreventiveResult, raoParameters, optimizationReportNode);
        return postCheckResults(
            raoResult,
            initialOutput,
            raoParameters.getObjectiveFunctionParameters(),
            true,
            optimizationReportNode,
            flowUnit,
            false
        );
    }

    private @NonNull CompletableFuture<RaoResult> generateUnsecureRaoResultWithPrasOnly(ReportNode curativePerimeterOptimReportNode,
                                                                                        StateTree stateTree,
                                                                                        PrePerimeterResult initialOutput,
                                                                                        PostPerimeterResult postPreventiveResult,
                                                                                        PrePerimeterResult preCurativeSensitivityAnalysisOutput,
                                                                                        OptimizationResult preventiveResult,
                                                                                        boolean costOptimization,
                                                                                        Unit flowUnit,
                                                                                        ReportNode optimizationReportNode) {
        CastorReports.reportPreventivePerimeterNotSecure(curativePerimeterOptimReportNode);
        RaoResult mergedRaoResults = RaoResultGenerator.preventive(crac, initialOutput, postPreventiveResult, raoParameters, optimizationReportNode);
        // log results
        MostLimitingElementsReports.reportBusinessMostLimitingElements(
            curativePerimeterOptimReportNode,
            preCurativeSensitivityAnalysisOutput,
            preCurativeSensitivityAnalysisOutput,
            raoParameters.getObjectiveFunctionParameters().getType(),
            getFlowUnit(raoParameters),
            NUMBER_LOGGED_ELEMENTS_END_RAO
        );
        CastorReports.reportIfMostLimitingElementIsFictional(curativePerimeterOptimReportNode, preCurativeSensitivityAnalysisOutput);
        return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(),
            true, optimizationReportNode, flowUnit, false);
    }

    private PostPerimeterResult computePostPreventiveResult(final ToolProvider toolProvider,
                                                            final PrePerimeterResult initialOutput,
                                                            final OptimizationResult preventiveResult,
                                                            final ReportNode reportNode) {
        PostPerimeterResult postPreventiveResult;
        postPreventiveResult = new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
            .runBasedOnInitialPreviousAndOptimizationResults(network, initialOutput, initialOutput, Collections.emptySet(), preventiveResult, null, reportNode);
        return postPreventiveResult;
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
        Metadata metadata = secondPreventiveRaoResults.getExtension(Metadata.class);
        if (metadata != null && metadata.getComputationStatus() == ComputationStatus.FAILURE) {
            CastorReports.reportSecondPreventiveFailed(secondPreventiveReportNode);
            return false;
        }

        Instant curativeInstant = crac.getLastInstant();

        double firstPreventiveCost = mergedRaoResults.getExtension(CostResult.class).getCost(curativeInstant);
        double secondPreventiveCost = secondPreventiveRaoResults.getExtension(CostResult.class).getCost(curativeInstant);
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
                                                          final ReportNode optimizationReportNode,
                                                          final Unit flowUnit,
                                                          final boolean secondPreventiveRan) {
        RaoResult finalRaoResult = raoResult;

        double initialCost = initialResult.getCost();
        double initialFunctionalCost = initialResult.getFunctionalCost();
        double initialVirtualCost = initialResult.getVirtualCost();
        Instant lastInstant = crac.getLastInstant();
        double finalCost = finalRaoResult.getExtension(CostResult.class).getCost(lastInstant);
        double finalFunctionalCost = finalRaoResult.getExtension(CostResult.class).getFunctionalCost(lastInstant);
        double finalVirtualCost = finalRaoResult.getExtension(CostResult.class).getVirtualCost(lastInstant);

        if (handleCostIncrease && finalCost > initialCost + EPSILON) {
            CastorReports.reportRaoIncreasedOverallCost(optimizationReportNode, initialCost, initialFunctionalCost, initialVirtualCost, finalCost, finalFunctionalCost, finalVirtualCost);
            // log results
            MostLimitingElementsReports.reportBusinessMostLimitingElements(
                optimizationReportNode,
                initialResult,
                initialResult,
                objectiveFunctionParameters.getType(),
                flowUnit,
                NUMBER_LOGGED_ELEMENTS_END_RAO
            );
            String executionDetails = secondPreventiveRan
                ? OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION
                : OptimizationStepsExecuted.FIRST_PREVENTIVE_FELLBACK_TO_INITIAL_SITUATION;
            // FIXME: initialResult might have been filtered if a curative failure occurred during 2P
            finalRaoResult = RaoResultGenerator.empty(crac, executionDetails, initialResult, raoParameters);

            finalCost = initialCost;
            finalFunctionalCost = initialFunctionalCost;
            finalVirtualCost = initialVirtualCost;
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
