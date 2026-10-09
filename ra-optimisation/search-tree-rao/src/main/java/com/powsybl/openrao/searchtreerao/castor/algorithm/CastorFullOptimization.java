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
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private static final String WORKING_SCENARIO = "WorkingScenario";
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
            ToolProvider toolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, raoParameters);
            if (crac.getFlowCnecs().isEmpty()) {
                PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true)
                    .runInitialSensitivityAnalysis(network, optimizationReportNode);
                return CompletableFuture.completedFuture(new UnoptimizedRaoResultImpl(initialResult));
            }
            StateTree stateTree = new StateTree(crac, optimizationReportNode);
            ToolProvider workingToolProvider = toolProvider;
            RaoParameters dcRaoParameters = raoParameters;
            boolean useWorkingVars = LoadFlowAndSensitivityParameters.getOptimizeInDc(raoParameters)
                    && !LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc();
            if (useWorkingVars) {
                // clone RaoParameters and setDc(true)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JsonRaoParameters.write(raoParameters, baos, reportNode);
                dcRaoParameters = JsonRaoParameters.read(new ByteArrayInputStream(baos.toByteArray()), reportNode);
                LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(dcRaoParameters).getLoadFlowParameters().setDc(true);
                workingToolProvider = ToolProvider.buildFromRaoInputAndParameters(raoInput, dcRaoParameters);
                network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), WORKING_SCENARIO);
            }

            currentStep = "initial sensitivity analysis";
            // ----- INITIAL SENSI -----
            // compute initial sensitivity on all CNECs
            // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
            PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, toolProvider, true);

            PrePerimeterResult initialOutput;
            initialOutput = prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, optimizationReportNode);
            network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);

            PrePerimeterResult workingInitialOutput = initialOutput;
            if (useWorkingVars) {
                network.getVariantManager().setWorkingVariant(WORKING_SCENARIO);
                PrePerimeterSensitivityAnalysis workingprePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                        crac,
                        crac.getFlowCnecs(),
                        crac.getRangeActions(),
                        dcRaoParameters,
                        workingToolProvider,
                        true);
                workingInitialOutput = workingprePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode);
                if (workingInitialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                    // BUSINESS_LOGS.error("Initial sensitivity analysis in DC failed. Disable performance improvement using DC.");
                    useWorkingVars = false;
                    workingToolProvider = toolProvider;
                    workingInitialOutput = initialOutput;
                    network.getVariantManager().removeVariant(WORKING_SCENARIO);
                } else {
                    //                    RaoLogger.logSensitivityAnalysisResults("Initial DC sensitivity analysis: ",
                    //                            workingprePerimeterSensitivityAnalysis.getObjectiveFunction(),
                    //                            RemedialActionActivationResultImpl.empty(workingInitialOutput),
                    //                            workingInitialOutput,
                    //                            dcRaoParameters,
                    //                            NUMBER_LOGGED_ELEMENTS_DURING_RAO);
                }
                network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
            }

            if (initialOutput.getSensitivityStatus() == ComputationStatus.FAILURE) {
                CommonReports.reportInitialSensitivityAnalysisFailed(optimizationReportNode);
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Initial sensitivity analysis failed"));
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

            final ReportNode preventivePerimeterOptimReportNode = CastorReports.reportPreventivePerimeterOptimization(optimizationReportNode);

            FirstPreventiveRaoOutcome firstPreventiveRaoOutcome = runFirstPreventiveOptimization(
                stateTree, toolProvider, workingToolProvider, useWorkingVars, initialOutput, workingInitialOutput, preventivePerimeterOptimReportNode);
            OptimizationResult preventiveResult = firstPreventiveRaoOutcome.preventiveResult();
            long preventiveRaoTime = firstPreventiveRaoOutcome.preventiveRaoTime();

            // ----- SENSI POST-PRA -----
            // mutualize the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs
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
                return CompletableFuture.completedFuture(new FailedRaoResultImpl("Systematic sensitivity analysis after preventive remedial actions failed"));
            }
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterPraResults(optimizationReportNode,
                prePerimeterSensitivityAnalysis.getObjectiveFunction(),
                new RemedialActionActivationResultImpl(preventiveResult, preventiveResult),
                preCurativeSensitivityAnalysisOutput,
                raoParameters,
                NUMBER_LOGGED_ELEMENTS_DURING_RAO);

            if (stateTree.getContingencyScenarios().isEmpty()) {
                // log final result
                MostLimitingElementsReports.reportTechnicalMostLimitingElements(
                    preventivePerimeterOptimReportNode,
                    preventiveResult,
                    preventiveResult,
                    null,
                    raoParameters.getObjectiveFunctionParameters().getType(),
                    getFlowUnit(raoParameters),
                    10
                );
                CastorReports.reportIfMostLimitingElementIsFictional(preventivePerimeterOptimReportNode, preventiveResult);
                RaoResult raoResult = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, postPreventiveResult, crac, raoParameters, preventivePerimeterOptimReportNode);
                return postCheckResults(raoResult, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
            }

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            // optimize contingency scenarios (auto + curative instants)
            currentStep = "contingency scenarios";

            final ReportNode curativePerimeterOptimReportNode = CastorReports.reportCurativePerimeterOptimization(optimizationReportNode);

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter enforce-curative-if-basecase-unsecure)
            double preventiveOptimalCost = preventiveResult.getCost();
            if (shouldStopOptimisationIfPreventiveUnsecure(preventiveOptimalCost)) {
                CastorReports.reportPreventivePerimeterNotSecure(curativePerimeterOptimReportNode);
                RaoResult mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(stateTree, initialOutput, postPreventiveResult, crac, raoParameters, curativePerimeterOptimReportNode);
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
                return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
            }

            final ReportNode postContingencyPerimeterOptimReportNode = CastorReports.reportPostContingencyPerimeterOptimization(curativePerimeterOptimReportNode);
            TreeParameters curativeTreeParameters = TreeParameters.buildForCurativePerimeter(raoParameters, preventiveOptimalCost);
            // curative stop criterion must be compared against leaf costs computed by the same (working) tool provider used in the curative search,
            // otherwise a DC search is judged against an AC-recomputed target
            CastorContingencyScenarios castorContingencyScenarios = new CastorContingencyScenarios(
                    crac, raoParameters, toolProvider, stateTree, curativeTreeParameters, initialOutput);
            if (useWorkingVars) {
                castorContingencyScenarios.setWorkingToolProvider(workingToolProvider);
                castorContingencyScenarios.setWorkingInitialSensitivityOutput(workingInitialOutput);
            }

            Map<State, PostPerimeterResult> postContingencyResults = castorContingencyScenarios.optimizeContingencyScenarios(
                network,
                preCurativeSensitivityAnalysisOutput,
                false,
                postContingencyPerimeterOptimReportNode
            );
            CastorReports.reportPostContingencyPerimeterOptimizationEnd();

            // ----- SECOND PREVENTIVE PERIMETER OPTIMIZATION -----
            currentStep = "second preventive optimization";

            RaoResult mergedRaoResults = runSecondPreventiveOptimization(
                optimizationReportNode,
                stateTree,
                initialOutput,
                postPreventiveResult,
                postContingencyResults,
                preventiveResult,
                preventiveRaoTime,
                toolProvider,
                useWorkingVars,
                workingToolProvider,
                workingInitialOutput,
                dcRaoParameters,
                castorContingencyScenarios,
                prePerimeterSensitivityAnalysis
            );

            return postCheckResults(mergedRaoResults, initialOutput, raoParameters.getObjectiveFunctionParameters(), true, optimizationReportNode);
        } catch (Exception e) {
            CastorReports.reportExceptionMessageAndStacktrace(optimizationReportNode, e);
            return CompletableFuture.completedFuture(new FailedRaoResultImpl(String.format("RAO failed during %s : %s", currentStep, e.getMessage())));
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariantName);
        }
    }

    private record FirstPreventiveRaoOutcome(OptimizationResult preventiveResult, long preventiveRaoTime) {
    }

    private FirstPreventiveRaoOutcome runFirstPreventiveOptimization(final StateTree stateTree,
                                                                      final ToolProvider toolProvider,
                                                                      final ToolProvider workingToolProvider,
                                                                      final boolean useWorkingVars,
                                                                      final PrePerimeterResult initialOutput,
                                                                      final PrePerimeterResult workingInitialOutput,
                                                                      final ReportNode preventivePerimeterOptimReportNode) {
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();

        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), List.of(PREVENTIVE_SCENARIO, SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT));
        if (useWorkingVars) {
            network.getVariantManager().setWorkingVariant(WORKING_SCENARIO);
        } else {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        }

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
            preventiveResult = optimizePreventivePerimeter(stateTree, workingToolProvider, toolProvider, workingInitialOutput, initialOutput, preventivePerimeterOptimReportNode);
        }
        CastorReports.reportPreventivePerimeterOptimizationEnd();
        java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
        long preventiveRaoTime = ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant);

        if (network.getVariantManager().getWorkingVariantId().equals(WORKING_SCENARIO)) {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        }
        if (useWorkingVars) {
            network.getVariantManager().removeVariant(WORKING_SCENARIO);
        }

        return new FirstPreventiveRaoOutcome(preventiveResult, preventiveRaoTime);
    }

    /**
     * Run the second preventive RAO (and, where relevant, re-run auto/curative perimeters) when the first
     * preventive and post-contingency results warrant it, and merge the results. Falls back to the first
     * preventive + post-contingency merged result when second preventive does not improve it.
     */
    private RaoResult runSecondPreventiveOptimization(final ReportNode optimizationReportNode,
                                                      final StateTree stateTree,
                                                      final PrePerimeterResult initialOutput,
                                                      final PostPerimeterResult postPreventiveResult,
                                                      final Map<State, PostPerimeterResult> postContingencyResults,
                                                      final OptimizationResult preventiveResult,
                                                      final long preventiveRaoTime,
                                                      final ToolProvider toolProvider,
                                                      final boolean useWorkingVars,
                                                      final ToolProvider workingToolProvider,
                                                      final PrePerimeterResult workingInitialOutput,
                                                      final RaoParameters dcRaoParameters,
                                                      final CastorContingencyScenarios castorContingencyScenarios,
                                                      final PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis) {
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
        boolean logFinalResultsOutsideOfSecondPreventive = true;
        // Run second preventive when necessary
        CastorSecondPreventive castorSecondPreventive = new CastorSecondPreventive(
            crac,
            raoParameters,
            network,
            stateTree,
            toolProvider,
            targetEndInstant,
            secondPreventivePerimeterOptimReportNode
        );

        if (useWorkingVars) {
            castorSecondPreventive.setWorkingToolProvider(workingToolProvider);
            castorSecondPreventive.setWorkingInitialOutput(workingInitialOutput);
        }

        // define variables to set with second preventive results only if it improves first
        PostPerimeterResult finalSecondPreventiveResult = postPreventiveResult;
        PostPerimeterResult intermediateSecondPreventiveResult = postPreventiveResult;
        Map<State, PostPerimeterResult> finalPostContingencyResults = new HashMap<>(postContingencyResults);

        if (castorSecondPreventive.shouldRunSecondPreventiveRao(preventiveResult, postContingencyResults.values(), mergedRaoResults, preventiveRaoTime)) {
            CastorSecondPreventive.SecondPreventiveRaoResultsHolder secondPreventiveRaoResultsHolder = castorSecondPreventive.runSecondPreventiveAndAutoRao(
                castorContingencyScenarios, prePerimeterSensitivityAnalysis, initialOutput, postPreventiveResult, postContingencyResults);
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
                    dcRaoParameters,
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
        return mergedRaoResults;
    }

    private PostPerimeterResult computePostPreventiveResult(final ToolProvider toolProvider,
                                                            final PrePerimeterResult initialOutput,
                                                            final OptimizationResult preventiveResult,
                                                            final ReportNode reportNode) {
        PostPerimeterResult postPreventiveResult;
        postPreventiveResult = new PostPerimeterSensitivityAnalysis(
                crac,
                crac.getFlowCnecs(),
                crac.getRangeActions(),
                toolProvider != null ? toolProvider.getRaoParameters() : raoParameters,
                toolProvider,
                true)
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
                                                           final ToolProvider workingToolProvider,
                                                           final ToolProvider baseToolProvider,
                                                           final PrePerimeterResult workingInitialResult,
                                                           final PrePerimeterResult initialResult,
                                                           final ReportNode preventivePerimeterOptimReportNode) {

        RaoParameters raoParameters = workingToolProvider != null ? workingToolProvider.getRaoParameters() : this.raoParameters;

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(
            stateTree.getBasecaseScenario(),
            crac,
            network,
            raoParameters,
            workingInitialResult,
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
            workingInitialResult,
            workingInitialResult,
            Collections.emptySet(),
            raoParameters,
            statesToOptimize
        );
        SearchTreeInput searchTreeInput = SearchTreeInput.create()
            .withNetwork(network)
            .withOptimizationPerimeter(optPerimeter)
            .withInitialFlowResult(workingInitialResult)
            .withPrePerimeterResult(workingInitialResult)
            .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions()) //no remedial Action applied
            .withObjectiveFunction(objectiveFunction)
            .withToolProvider(workingToolProvider)
            .withOutageInstant(crac.getOutageInstant())
            .build();

        OptimizationResult optResult = new SearchTree(searchTreeInput, searchTreeParameters, true, preventivePerimeterOptimReportNode).run().join();
        OptimizationResult finalResult;
        if (workingToolProvider == baseToolProvider) {
            finalResult = optResult;
        } else {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
            PreventiveOptimizationPerimeter optPerimeterBase = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(
                    stateTree.getBasecaseScenario(), crac, network, baseToolProvider.getRaoParameters(), initialResult, reportNode);
            statesToOptimize = new HashSet<>(optPerimeterBase.getMonitoredStates());
            statesToOptimize.add(optPerimeterBase.getMainOptimizationState());

            SearchTreeInput baseSearchTreeInput = SearchTreeInput.create()
                    .withNetwork(network)
                    .withOptimizationPerimeter(optPerimeterBase)
                    .withInitialFlowResult(initialResult)
                    .withPrePerimeterResult(initialResult)
                    .withPreOptimizationAppliedNetworkActions(new AppliedRemedialActions())
                    .withObjectiveFunction(ObjectiveFunction.build(
                            optPerimeterBase.getFlowCnecs(),
                            optPerimeterBase.getLoopFlowCnecs(),
                            initialResult,
                            initialResult,
                            Set.of(),
                            baseToolProvider.getRaoParameters(),
                            statesToOptimize))
                    .withToolProvider(baseToolProvider)
                    .withOutageInstant(crac.getOutageInstant())
                    .build();
            finalResult = new SearchTree(baseSearchTreeInput, searchTreeParameters, true, reportNode).optimizeDecidedNetworkActionCombination(optResult, reportNode);
        }
        applyRemedialActions(network, finalResult, crac.getPreventiveState());
        return finalResult;
    }
}
