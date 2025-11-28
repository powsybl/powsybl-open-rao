/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoPstRegulationParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation.CastorPstRegulation;
import com.powsybl.openrao.searchtreerao.castor.algorithm.pstregulation.PstRegulationResult;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.UnoptimizedCnecParameters;
import com.powsybl.openrao.searchtreerao.reports.CastorReports;
import com.powsybl.openrao.searchtreerao.reports.CommonReports;
import com.powsybl.openrao.searchtreerao.reports.MostLimitingElementsReports;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.FailedRaoResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.NetworkActionsResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.OneStateOnlyRaoResultImpl;
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
import org.apache.commons.lang3.tuple.Pair;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.getHvdcRangeActionsOnHvdcLineInAcEmulation;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyRemedialActions;

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
    private static final String PST_REGULATION = "PSTRegulation";
    private static final int NUMBER_LOGGED_ELEMENTS_DURING_RAO = 2;
    private static final int NUMBER_LOGGED_ELEMENTS_END_RAO = 10;
    private static final double EPSILON = 1e-6;

    private final RaoInput raoInput;
    private final Crac crac;
    private final Network network;
    private final RaoParameters raoParameters;
    private final java.time.Instant targetEndInstant;
    private final ReportNode reportNode;

    public CastorFullOptimization(final RaoInput raoInput, final RaoParameters raoParameters, final java.time.Instant targetEndInstant, final ReportNode reportNode) {
        this.raoInput = raoInput;
        this.crac = raoInput.getCrac();
        this.network = raoInput.getNetwork();
        this.raoParameters = raoParameters;
        this.targetEndInstant = targetEndInstant;
        this.reportNode = reportNode;
    }

    public CompletableFuture<RaoResult> run() {
        TECHNICAL_LOGS.info("Starting Castor full optimization");

        String currentStep = "data initialization";

        try {
            RaoUtil.initData(raoInput, raoParameters, reportNode);
            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
            if (crac.getFlowCnecs().isEmpty()) {
                PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider).runInitialSensitivityAnalysis(network, reportNode);
                return CompletableFuture.completedFuture(new UnoptimizedRaoResultImpl(initialResult));
            }
            StateTree stateTree = new StateTree(crac, reportNode);

            currentStep = "initial sensitivity analysis";
            // ----- INITIAL SENSI -----
            // compute initial sensitivity on all CNECs
            // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                crac,
                crac.getFlowCnecs(),
                crac.getRangeActions(),
                raoParameters,
                toolProvider);

            PrePerimeterResult initialOutput;
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);
            if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                CommonReports.reportInitialSensitivityAnalysisFailed(reportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
            }
            CastorReports.reportCastorInitialSensitivityAnalysisResults(reportNode,
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                RemedialActionActivationResultImpl.empty(initialOutput),
                initialOutput,
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            // ----- PREVENTIVE PERIMETER OPTIMIZATION -----
            // run search tree on preventive perimeter
            currentStep = "first preventive";
            java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();

            final ReportNode preventivePerimeterOptimReportNode = CastorReports.reportPreventivePerimeterOptimization(reportNode);

            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);

            if (stateTree.getContingencyScenarios().isEmpty()) {
                Pair<OptimizationResult, Set<FlowCnec>> preventiveResultsAndOptimizedCnecs = optimizePreventivePerimeter(stateTree, toolProvider, initialOutput, preventivePerimeterOptimReportNode);
                OptimizationResult preventiveResult = preventiveResultsAndOptimizedCnecs.getLeft();
                Set<FlowCnec> optimizedFlowCnecs = preventiveResultsAndOptimizedCnecs.getRight();
                CastorReports.reportPreventivePerimeterOptimizationEnd();
                // log final result
                MostLimitingElementsReports.reportTechnicalMostLimitingElements(preventivePerimeterOptimReportNode, preventiveResult, preventiveResult, null, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), 10);
                CastorReports.reportIfMostLimitingElementIsFictional(preventivePerimeterOptimReportNode, preventiveResult);
                RaoResult raoResult = new OneStateOnlyRaoResultImpl(crac.getPreventiveState(), initialOutput, preventiveResult, optimizedFlowCnecs);
                return postCheckResults(raoResult, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, reportNode);
            }

            OptimizationResult preventiveResult = optimizePreventivePerimeter(stateTree, toolProvider, initialOutput, preventivePerimeterOptimReportNode).getLeft();
            CastorReports.reportPreventivePerimeterOptimizationEnd();
            java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
            long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

            // ----- SENSI POST-PRA -----
            // mutualise the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs
            currentStep = "post-PRA sensitivity analysis";

            final ReportNode postPraSensiAnalysisReportNode = CastorReports.reportPostPraSensiAnalysis(reportNode);

            network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_SCENARIO, true);
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
            applyRemedialActions(network, preventiveResult, crac.getPreventiveState());

            PostPerimeterResult postPreventiveResult = computePostPreventiveResult(toolProvider, initialOutput, preventiveResult, postPraSensiAnalysisReportNode);
            PrePerimeterResult preCurativeSensitivityAnalysisOutput = postPreventiveResult.prePerimeterResultForAllFollowingStates();
            if (preCurativeSensitivityAnalysisOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                CastorReports.reportSystematicSensitivityAnalysisAfterPraFailed(postPraSensiAnalysisReportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
            }
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterPraResults(reportNode,
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                new RemedialActionActivationResultImpl(preventiveResult, preventiveResult),
                preCurativeSensitivityAnalysisOutput,
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            RaoResult mergedRaoResults;

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            // optimize contingency scenarios (auto + curative instants)
            currentStep = "contingency scenarios";

            final ReportNode curativePerimeterOptimReportNode = CastorReports.reportCurativePerimeterOptimization(reportNode);

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter enforce-curative-if-basecase-unsecure)
            double preventiveOptimalCost = preventiveResult.getCost();
            if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
                CastorReports.reportPreventivePerimeterNotSecure(curativePerimeterOptimReportNode);
                mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, postPreventiveResult, crac, raoParameters, curativePerimeterOptimReportNode);
                // log results
                MostLimitingElementsReports.reportBusinessMostLimitingElements(curativePerimeterOptimReportNode, preCurativeSensitivityAnalysisOutput, preCurativeSensitivityAnalysisOutput, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
                CastorReports.reportIfMostLimitingElementIsFictional(curativePerimeterOptimReportNode, preCurativeSensitivityAnalysisOutput);
                return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, reportNode);
            }

            final ReportNode postContingencyPerimeterOptimReportNode = CastorReports.reportPostContingencyPerimeterOptimization(curativePerimeterOptimReportNode);
            TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, preventiveOptimalCost);
            CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(crac, raoParameters, toolProvider, stateTree, curativeTreeParameters, initialOutput);
            Map<State, PostPerimeterResult> postContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(network, preCurativeSensitivityAnalysisOutput, false, postContingencyPerimeterOptimReportNode);
            CastorReports.reportPostContingencyPerimeterOptimizationEnd();

            // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
            currentStep = "second preventive optimization";

            final ReportNode secondPreventivePerimeterOptimReportNode = CastorReports.reportSecondPreventivePerimeterOptimization(reportNode);
            mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, postPreventiveResult, postContingencyResults, crac, raoParameters, secondPreventivePerimeterOptimReportNode);
            boolean logFinalResultsOutsideOfSecondPreventive = true;
            // Run second preventive when necessary
            CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(crac, raoParameters, network, stateTree, toolProvider, targetEndInstant, secondPreventivePerimeterOptimReportNode);

            // define variables to set with second preventive results only if it improves first
            PostPerimeterResult finalSecondPreventiveResult = postPreventiveResult;
            PostPerimeterResult intermediateSecondPreventiveResult = postPreventiveResult;
            Map<State, PostPerimeterResult> finalPostContingencyResults = new HashMap<>(postContingencyResults);

            if (castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, postContingencyResults.values(), mergedRaoResults, preventiveRaoTime)) {
                CastorSecondPreventive.SecondPreventiveRaoResultsHolder secondPreventiveRaoResultsHolder = castorSecondPreventive.runSecondPreventiveAndAutoRao(castorContingencyScenarios, prePerimeterSensitivityAnalysis, initialOutput, postPreventiveResult, postContingencyResults);
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
                    logFinalResultsOutsideOfSecondPreventive = false;
                } else {
                    mergedRaoResults.setExecutionDetails(OptimizationStepsExecuted.SECOND_PREVENTIVE_FELLBACK_TO_FIRST_PREVENTIVE_SITUATION);
                }
            }
            // Log final results
            if (logFinalResultsOutsideOfSecondPreventive) {
                final ReportNode finalResultsReportNode = CastorReports.reportMergingPreventiveAndPostContingencyRaoResults(reportNode);
                MostLimitingElementsReports.reportBusinessMostLimitingElements(finalResultsReportNode, stateTree.getBasecaseScenario(), finalSecondPreventiveResult.optimizationResult(), stateTree.getContingencyScenarios(), finalPostContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
                CastorReports.reportIfMostLimitingElementIsFictional(finalResultsReportNode, stateTree.getBasecaseScenario(), finalSecondPreventiveResult.optimizationResult(), stateTree.getContingencyScenarios(), finalPostContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit());
            }

            CompletableFuture<RaoResult> raoResult = postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, reportNode);

            // PST regulation
            Map<String, String> pstsToRegulate = SearchTreeRaoPstRegulationParameters.getPstsToRegulate(raoParameters);
            if (!pstsToRegulate.isEmpty()) {
                final ReportNode pstRegulationReportNode = CastorReports.reportPstRegulation(reportNode);
                network.getVariantManager().cloneVariant(INITIAL_SCENARIO, PST_REGULATION);
                network.getVariantManager().setWorkingVariant(PST_REGULATION);
                Set<PstRegulationResult> pstRegulationResults = CastorPstRegulation.regulatePsts(pstsToRegulate, finalPostContingencyResults, network, crac, raoParameters, mergedRaoResults);
                Map<State, PostPerimeterResult> postRegulationResults = mergeRaoAndPstRegulationResults(pstRegulationResults, finalSecondPreventiveResult, finalPostContingencyResults, prePerimeterSensitivityAnalysis, initialOutput, toolProvider, pstRegulationReportNode);
                RaoResult raoResultWithRegulation = new PreventiveAndCurativesRaoResultImpl(
                    stateTree,
                    initialOutput,
                    postPreventiveResult,
                    finalSecondPreventiveResult,
                    postRegulationResults,
                    crac,
                    raoParameters,
                    pstRegulationReportNode);
                raoResultWithRegulation.setExecutionDetails(mergedRaoResults.getExecutionDetails());
                CastorReports.reportPstRegulationEnd();
                final ReportNode finalResultsReportNode = CastorReports.reportMergingRaoAndPstRegulationResults(reportNode);
                MostLimitingElementsReports.reportBusinessMostLimitingElements(finalResultsReportNode, stateTree.getBasecaseScenario(), finalSecondPreventiveResult.optimizationResult(), stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit(), NUMBER_LOGGED_ELEMENTS_END_RAO);
                CastorReports.reportIfMostLimitingElementIsFictional(finalResultsReportNode, stateTree.getBasecaseScenario(), finalSecondPreventiveResult.optimizationResult(), stateTree.getContingencyScenarios(), postContingencyResults, raoParameters.getObjectiveFunctionParameters().getType(), raoParameters.getObjectiveFunctionParameters().getUnit());
                return postCheckResults(raoResultWithRegulation, initialOutput, raoParameters.getObjectiveFunctionParameters(), false, reportNode);
            }

            return raoResult;
        } catch (RuntimeException e) {
            CastorReports.reportExceptionMessageAndStacktrace(reportNode, e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(String.format("RAO failed during %s : %s", currentStep, e.getMessage())));
        }
    }

    private PostPerimeterResult computePostPreventiveResult(final ToolProvider toolProvider,
                                                            final PrePerimeterResult initialOutput,
                                                            final OptimizationResult preventiveResult,
                                                            final ReportNode reportNode) {
        PostPerimeterResult postPreventiveResult;
        try {
            postPreventiveResult = new PostPerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider)
                .runBasedOnInitialPreviousAndOptimizationResults(network, initialOutput, CompletableFuture.completedFuture(initialOutput), Collections.emptySet(), preventiveResult, null, reportNode)
                .get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new OpenRaoException("Exception during post preventive sensitivity analysis", e);
        }
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
            CastorReports.reportSecondPreventiveIncreasedOverallCost(secondPreventiveReportNode, firstPreventiveCost, secondPreventiveCost, curativeInstant, mergedRaoResults, secondPreventiveRaoResults);
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
            MostLimitingElementsReports.reportBusinessMostLimitingElements(optimizationReportNode, initialResult, initialResult, objectiveFunctionParameters.getType(), objectiveFunctionParameters.getUnit(), NUMBER_LOGGED_ELEMENTS_DURING_RAO);
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
        CastorReports.reportCostsBeforeAndAfterRao(optimizationReportNode, initialCost, initialFunctionalCost, initialVirtualCost, initialResult, finalCost, finalFunctionalCost, finalVirtualCost, finalRaoResult, crac.getLastInstant());

        return CompletableFuture.completedFuture(finalRaoResult);
    }

    private Pair<OptimizationResult, Set<FlowCnec>> optimizePreventivePerimeter(final StateTree stateTree,
                                                                  final ToolProvider toolProvider,
                                                                  final PrePerimeterResult initialResult,
                                                                  final ReportNode preventivePerimeterOptimReportNode) {

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(stateTree.getBasecaseScenario(), crac, network, raoParameters, initialResult, preventivePerimeterOptimReportNode);

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

        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(initialResult)
            .withPrePerimeterResult(initialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(ObjectiveFunction.build(optPerimeter.getFlowCnecs(), optPerimeter.getLoopFlowCnecs(), initialResult, initialResult, Collections.emptySet(), raoParameters, statesToOptimize))
            .withToolProvider(toolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true, preventivePerimeterOptimReportNode).run().join();
        applyRemedialActions(network, optResult, crac.getPreventiveState());
        return Pair.of(optResult, optPerimeter.getFlowCnecs());
    }

    private Map<State, PostPerimeterResult> mergeRaoAndPstRegulationResults(final Set<PstRegulationResult> pstRegulationResults,
                                                                            final PostPerimeterResult postPraResult,
                                                                            final Map<State, PostPerimeterResult> postContingencyResults,
                                                                            final PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis,
                                                                            final FlowResult initialFlowResult,
                                                                            final ToolProvider toolProvider,
                                                                            final ReportNode pstRegulationReportNode) {
        // create a new network variant from initial variant for performing the results merging
        String variantName = "PSTRegulationResultsMerging";
        network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        network.getVariantManager().cloneVariant(INITIAL_SCENARIO, variantName);
        network.getVariantManager().setWorkingVariant(variantName);

        // apply PRAs
        applyRemedialActions(network, postPraResult.optimizationResult(), crac.getPreventiveState());

        Set<State> regulatedStates = pstRegulationResults.stream().map(pstRegulationResult -> crac.getState(pstRegulationResult.contingency().getId(), crac.getLastInstant())).collect(Collectors.toSet());
        Map<State, Set<NetworkAction>> appliedNetworkActions = new HashMap<>();

        // gather all applied ARAs and CRAs
        AppliedRemedialActions appliedRemedialActions = new AppliedRemedialActions();
        postContingencyResults.forEach((state, postPerimeterResult) -> {
            appliedNetworkActions.put(state, postPerimeterResult.optimizationResult().getActivatedNetworkActions());
            appliedRemedialActions.addAppliedNetworkActions(state, postPerimeterResult.optimizationResult().getActivatedNetworkActions());
            appliedRemedialActions.addAppliedRangeActions(state, getAppliedRangeActionsAndSetPoint(state, postPraResult.optimizationResult()));
            appliedRemedialActions.addAppliedRangeActions(state, getAppliedRangeActionsAndSetPoint(state, postPerimeterResult.optimizationResult()));
        });

        // overwrite PST range action results for regulated PSTs
        pstRegulationResults.forEach(pstRegulationResult -> pstRegulationResult.regulatedTapPerPst().forEach((pstRangeAction, regulatedTap) -> appliedRemedialActions.addAppliedRangeAction(crac.getState(pstRegulationResult.contingency().getId(), crac.getLastInstant()), pstRangeAction, pstRangeAction.convertTapToAngle(regulatedTap))));

        PrePerimeterResult postCraSensitivityAnalysisOutput = prePerimeterSensitivityAnalysis.runBasedOnInitialResults(network, initialFlowResult, Collections.emptySet(), appliedRemedialActions, pstRegulationReportNode);

        Map<State, PostPerimeterResult> postRegulationPostContingencyResults = new HashMap<>();

        // override optimization result
        RangeActionActivationResultImpl postRegulationRangeActionActivationResult = new RangeActionActivationResultImpl(postCraSensitivityAnalysisOutput);
        postContingencyResults.keySet().forEach(state -> appliedRemedialActions.getAppliedRangeActions(state).forEach((rangeAction, setPoint) -> postRegulationRangeActionActivationResult.putResult(rangeAction, state, setPoint)));
        OptimizationResult newOptimizationResult = new OptimizationResultImpl(postCraSensitivityAnalysisOutput, postCraSensitivityAnalysisOutput, postCraSensitivityAnalysisOutput, new NetworkActionsResultImpl(appliedNetworkActions), postRegulationRangeActionActivationResult);

        for (State state : postContingencyResults.keySet()) {
            // For instants before pst regulation instant, keep previous results
            if (state.getInstant().comesBefore(crac.getLastInstant())) {
                postRegulationPostContingencyResults.put(state, postContingencyResults.get(state));
            } else {
                // For curative instant, update regulatedStates with newly computed sensi result
                postRegulationPostContingencyResults.put(state, new PostPerimeterResult(
                    regulatedStates.contains(state) ? newOptimizationResult : postContingencyResults.get(state).optimizationResult(),
                    postCraSensitivityAnalysisOutput
                ));
            }
        }

        return postRegulationPostContingencyResults;
    }

    private static Map<RangeAction<?>, Double> getAppliedRangeActionsAndSetPoint(State state, OptimizationResult optimizationResult) {
        Map<RangeAction<?>, Double> optimizedRangeActions = new HashMap<>();
        optimizationResult.getActivatedRangeActions(state).forEach(rangeAction -> optimizedRangeActions.put(rangeAction, optimizationResult.getOptimizedSetpoint(rangeAction, state)));
        return optimizedRangeActions;
    }
}
