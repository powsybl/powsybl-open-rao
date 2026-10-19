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
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoadFlowAndSensitivityParameters;
import com.powsybl.openrao.searchtreerao.commons.ToolProvider;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.PreventiveOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
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
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        Unit flowUnit = RaoUtil.getFlowUnit(raoParameters);

        try {
            RaoRunContext context = createContext();
            if (crac.getFlowCnecs().isEmpty()) {
                PrePerimeterResult initialResult = new PrePerimeterSensitivityAnalysis(crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, context.getToolProvider(), true)
                    .runInitialSensitivityAnalysis(network, optimizationReportNode);
                return CompletableFuture.completedFuture(new UnoptimizedRaoResultImpl(initialResult, Set.of(), flowUnit, Set.of()));
            }

            context.setStateTree(new StateTree(crac, optimizationReportNode));
            context.setWorkingToolProvider(context.getToolProvider());
            context.setDcRaoParameters(raoParameters);
            context.setUseWorkingVars(LoadFlowAndSensitivityParameters.getOptimizeInDc(raoParameters)
                    && !LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(raoParameters).getLoadFlowParameters().isDc());
            if (context.useWorkingVars()) {
                // clone RaoParameters and setDc(true)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JsonRaoParameters.write(raoParameters, baos, reportNode);
                context.setDcRaoParameters(JsonRaoParameters.read(new ByteArrayInputStream(baos.toByteArray()), reportNode));
                LoadFlowAndSensitivityParameters.getSensitivityWithLoadFlowParameters(context.getDcRaoParameters()).getLoadFlowParameters().setDc(true);
                context.setWorkingToolProvider(ToolProvider.buildFromRaoInputAndParameters(raoInput, context.getDcRaoParameters()));
                network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), WORKING_SCENARIO);
            }

            currentStep = "initial sensitivity analysis";
            // ----- INITIAL SENSI -----
            // compute initial sensitivity on all CNECs
            // (this is necessary to have initial flows for MNEC and loopflow constraints on CNECs, in preventive and curative perimeters)
            runInitialSensitivityAnalysis(context, optimizationReportNode);
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
            // mutualize the pre-perimeter sensi analysis for all contingency scenario + get after-PRA result over all CNECs
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
                return postCheckResults(raoResult, context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true,
                        optimizationReportNode, flowUnit, context.getStateTree().getOperatorsNotSharingCras());
            }

            // ----- CURATIVE PERIMETERS OPTIMIZATION -----
            // optimize contingency scenarios (auto + curative instants)
            currentStep = "contingency scenarios";

            final ReportNode curativePerimeterOptimReportNode = CastorReports.reportCurativePerimeterOptimization(optimizationReportNode);

            // If stop criterion is SECURE and preventive perimeter was not secure, do not run post-contingency RAOs
            // (however RAO could continue depending on parameter enforce-curative-if-basecase-unsecure)
            if (shouldStopOptimisationIfPreventiveUnsecure(context.getPreventiveResult().getCost())) {
                CastorReports.reportPreventivePerimeterNotSecure(curativePerimeterOptimReportNode);
                RaoResult mergedRaoResults = new PreventiveAndCurativesRaoResultImpl(
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
                return postCheckResults(mergedRaoResults, context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true,
                        optimizationReportNode, flowUnit, context.getStateTree().getOperatorsNotSharingCras());
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

            return postCheckResults(context.getMergedRaoResults(), context.getInitialOutput(), raoParameters.getObjectiveFunctionParameters(), true,
                    optimizationReportNode, flowUnit, context.getStateTree().getOperatorsNotSharingCras());
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

    /**
     * Compute initial sensitivity on all CNECs (necessary to have initial flows for MNEC and loopflow
     * constraints on CNECs, in preventive and curative perimeters), both in AC and, when relevant, in the
     * DC working variant used to speed up the search tree.
     */
    private void runInitialSensitivityAnalysis(final RaoRunContext context, final ReportNode optimizationReportNode) {
        PrePerimeterSensitivityAnalysis prePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
            crac, crac.getFlowCnecs(), crac.getRangeActions(), raoParameters, context.getToolProvider(), true);
        context.setPrePerimeterSensitivityAnalysis(prePerimeterSensitivityAnalysis);

        context.setInitialOutput(prePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, optimizationReportNode));
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), INITIAL_SCENARIO);

        context.setWorkingInitialOutput(context.initialOutput);
        if (context.useWorkingVars()) {
            network.getVariantManager().setWorkingVariant(WORKING_SCENARIO);
            PrePerimeterSensitivityAnalysis workingprePerimeterSensitivityAnalysis = new PrePerimeterSensitivityAnalysis(
                    crac,
                    crac.getFlowCnecs(),
                    crac.getRangeActions(),
                    context.getDcRaoParameters(),
                    context.getWorkingToolProvider(),
                    true);
            context.setWorkingInitialOutput(workingprePerimeterSensitivityAnalysis.runInitialSensitivityAnalysis(network, reportNode));
            if (context.getWorkingInitialOutput().getSensitivityStatus() == ComputationStatus.FAILURE) {
                context.setUseWorkingVars(false);
                context.setWorkingToolProvider(context.getToolProvider());
                context.setDcRaoParameters(raoParameters);
                context.setWorkingInitialOutput(context.getInitialOutput());
                network.getVariantManager().removeVariant(WORKING_SCENARIO);
                CastorReports.reportCastorInitialDcSensitivityAnalysisFailed(optimizationReportNode);
            } else {
                CastorReports.reportCastorInitialDcSensitivityAnalysisResults(optimizationReportNode,
                        workingprePerimeterSensitivityAnalysis.getObjectiveFunction(),
                        RemedialActionActivationResultImpl.empty(context.getWorkingInitialOutput()),
                        context.getWorkingInitialOutput(),
                        context.getDcRaoParameters(),
                        NUMBER_LOGGED_ELEMENTS_DURING_RAO);
            }
            network.getVariantManager().setWorkingVariant(INITIAL_SCENARIO);
        }
    }

    private void runFirstPreventiveOptimization(final RaoRunContext context, final ReportNode preventivePerimeterOptimReportNode) {
        java.time.Instant preventiveRaoStartInstant = java.time.Instant.now();

        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), List.of(PREVENTIVE_SCENARIO, SECOND_PREVENTIVE_SCENARIO_BEFORE_OPT));
        if (context.useWorkingVars()) {
            network.getVariantManager().setWorkingVariant(WORKING_SCENARIO);
        } else {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        }

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
            preventiveResult = optimizePreventivePerimeter(
                    context.getStateTree(), context.getWorkingToolProvider(), context.getToolProvider(),
                    context.getWorkingInitialOutput(), initialOutput, preventivePerimeterOptimReportNode);
        }
        java.time.Instant preventiveRaoEndInstant = java.time.Instant.now();
        context.setPreventiveRaoTime(ChronoUnit.SECONDS.between(preventiveRaoStartInstant, preventiveRaoEndInstant));

        if (network.getVariantManager().getWorkingVariantId().equals(WORKING_SCENARIO)) {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
        }
        if (context.useWorkingVars()) {
            network.getVariantManager().removeVariant(WORKING_SCENARIO);
        }
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
                crac,
                crac.getFlowCnecs(),
                crac.getRangeActions(),
                context.getToolProvider().getRaoParameters(),
                context.getToolProvider(),
                true)
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
        if (context.useWorkingVars()) {
            castorContingencyScenarios.setWorkingToolProvider(context.getWorkingToolProvider());
            castorContingencyScenarios.setWorkingInitialSensitivityOutput(context.getWorkingInitialOutput());
        }
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

        if (context.useWorkingVars()) {
            castorSecondPreventive.setWorkingToolProvider(context.getWorkingToolProvider());
            castorSecondPreventive.setWorkingInitialOutput(context.getWorkingInitialOutput());
        }

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
                    context.getDcRaoParameters(),
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
                                                          final ReportNode optimizationReportNode,
                                                          final Unit flowUnit,
                                                          final Set<String> operatorsNotSharingCras) {
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
            finalRaoResult = new UnoptimizedRaoResultImpl(initialResult, crac.getFlowCnecs(), flowUnit, operatorsNotSharingCras);
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

        RaoParameters currentRaoParameters = workingToolProvider != null ? workingToolProvider.getRaoParameters() : this.raoParameters;

        PreventiveOptimizationPerimeter optPerimeter = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(
            stateTree.getBasecaseScenario(),
            crac,
            network,
            currentRaoParameters,
            workingInitialResult,
            preventivePerimeterOptimReportNode
        );

        SearchTreeParameters searchTreeParameters = CastorSearchTreeHelper.buildSearchTreeParameters(currentRaoParameters,
            TreeParameters.buildForPreventivePerimeter(currentRaoParameters), crac, network,
            stateTree.getOperatorsNotSharingCras(), preventivePerimeterOptimReportNode);

        Set<State> statesToOptimize = new HashSet<>(optPerimeter.getMonitoredStates());
        statesToOptimize.add(optPerimeter.getMainOptimizationState());

        ObjectiveFunction objectiveFunction = ObjectiveFunction.build(
            optPerimeter.getFlowCnecs(),
            optPerimeter.getLoopFlowCnecs(),
            workingInitialResult,
            workingInitialResult,
            Collections.emptySet(),
            currentRaoParameters,
            statesToOptimize
        );
        OptimizationResult optResult = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeter, objectiveFunction, workingInitialResult, workingInitialResult,
            new AppliedRemedialActions(), workingToolProvider, crac.getOutageInstant(), searchTreeParameters, true, preventivePerimeterOptimReportNode);
        OptimizationResult finalResult;
        if (workingToolProvider == baseToolProvider) {
            finalResult = optResult;
        } else {
            network.getVariantManager().setWorkingVariant(PREVENTIVE_SCENARIO);
            PreventiveOptimizationPerimeter optPerimeterBase = PreventiveOptimizationPerimeter.buildFromBasecaseScenario(
                    stateTree.getBasecaseScenario(), crac, network, baseToolProvider.getRaoParameters(), initialResult, reportNode);
            statesToOptimize = new HashSet<>(optPerimeterBase.getMonitoredStates());
            statesToOptimize.add(optPerimeterBase.getMainOptimizationState());

            ObjectiveFunction baseObjectiveFunction = ObjectiveFunction.build(
                    optPerimeterBase.getFlowCnecs(),
                    optPerimeterBase.getLoopFlowCnecs(),
                    initialResult,
                    initialResult,
                    Set.of(),
                    baseToolProvider.getRaoParameters(),
                    statesToOptimize);
            finalResult = CastorSearchTreeHelper.reoptimizeNetworkActionCombination(network, optPerimeterBase, baseObjectiveFunction, initialResult, initialResult,
                new AppliedRemedialActions(), baseToolProvider, crac.getOutageInstant(), searchTreeParameters, optResult, true, reportNode);

            if (finalResult.getSensitivityStatus() == ComputationStatus.FAILURE) {
                // the DC-decided combination does not even converge once recomputed in AC (e.g. it creates an
                // electrical island that is solvable in DC but not in AC): ignore the DC decision and search for a
                // combination directly in AC, which naturally discards candidates that fail to converge
                CastorReports.reportPreventiveDcDecisionFailedAcVerification(reportNode, optResult.getActivatedNetworkActions().size());
                SearchTreeParameters acSearchTreeParameters = CastorSearchTreeHelper.buildSearchTreeParameters(baseToolProvider.getRaoParameters(),
                    TreeParameters.buildForPreventivePerimeter(baseToolProvider.getRaoParameters()), crac, network,
                    stateTree.getOperatorsNotSharingCras(), reportNode);
                finalResult = CastorSearchTreeHelper.searchNetworkActionCombination(network, optPerimeterBase, baseObjectiveFunction, initialResult, initialResult,
                    new AppliedRemedialActions(), baseToolProvider, crac.getOutageInstant(), acSearchTreeParameters, true, reportNode);
            }
        }
        applyRemedialActions(network, finalResult, crac.getPreventiveState());
        return finalResult;
    }
}
