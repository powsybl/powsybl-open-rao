/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.commons.report.TypedValue.ERROR_SEVERITY;
import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class CastorReports {
    private CastorReports() {
        // Utility class should not be instantiated
    }

    public static ReportNode reportCastorFullOptimization(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCastorFullOptimization")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Starting Castor full optimization");

        return addedNode;
    }

    public static ReportNode reportCastorOneStateOnly(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCastorOneStateOnly")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Starting Castor one state only");

        return addedNode;
    }

    public static void reportCastorInitialSensitivityAnalysisResults(final ReportNode parentNode,
                                                                     final ObjectiveFunction objectiveFunction,
                                                                     final RemedialActionActivationResult remedialActionActivationResult,
                                                                     final PrePerimeterResult sensitivityAnalysisResult,
                                                                     final RaoParameters raoParameters,
                                                                     final int numberOfLoggedLimitingElements) {
        final String messageTemplate = "openrao.searchtreerao.reportCastorInitialSensitivityAnalysisResults";
        final String prefix = "Initial sensitivity analysis: ";
        CommonReports.reportSensitivityAnalysisResults(parentNode, messageTemplate, prefix, objectiveFunction, remedialActionActivationResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportCastorSystematicSensitivityAnalysisAfterPraResults(final ReportNode parentNode,
                                                                                final ObjectiveFunction objectiveFunction,
                                                                                final RemedialActionActivationResult remedialActionActivationResult,
                                                                                final PrePerimeterResult sensitivityAnalysisResult,
                                                                                final RaoParameters raoParameters,
                                                                                final int numberOfLoggedLimitingElements) {
        final String messageTemplate = "openrao.searchtreerao.reportCastorSystematicSensitivityAnalysisAfterPraResults";
        final String prefix = "Systematic sensitivity analysis after preventive remedial actions: ";
        CommonReports.reportSensitivityAnalysisResults(parentNode, messageTemplate, prefix, objectiveFunction, remedialActionActivationResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportCastorSystematicSensitivityAnalysisAfterCraResults(final ReportNode parentNode,
                                                                                final ObjectiveFunction objectiveFunction,
                                                                                final RemedialActionActivationResult remedialActionActivationResult,
                                                                                final PrePerimeterResult sensitivityAnalysisResult,
                                                                                final RaoParameters raoParameters,
                                                                                final int numberOfLoggedLimitingElements) {
        final String messageTemplate = "openrao.searchtreerao.reportCastorSystematicSensitivityAnalysisAfterCraResults";
        final String prefix = "Systematic sensitivity analysis after curative remedial actions before second preventive optimization: ";
        CommonReports.reportSensitivityAnalysisResults(parentNode, messageTemplate, prefix, objectiveFunction, remedialActionActivationResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportIfMostLimitingElementIsFictional(final ReportNode parentNode,
                                                              final Perimeter preventivePerimeter,
                                                              final OptimizationResult basecaseOptimResult,
                                                              final Set<ContingencyScenario> contingencyScenarios,
                                                              final Map<State, PostPerimeterResult> contingencyOptimizationResults,
                                                              final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                              final Unit unit) {

        final List<FlowCnec> sortedFlowCnecs = getSortedFlowCnecs(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunction, unit);
        final String mostLimitingCnecId = sortedFlowCnecs.getFirst().getId();
        reportIfMostLimitingElementIsFictional(parentNode, mostLimitingCnecId);
    }

    private static List<FlowCnec> getSortedFlowCnecs(final Perimeter preventivePerimeter,
                                                     final OptimizationResult basecaseOptimResult,
                                                     final Set<ContingencyScenario> contingencyScenarios,
                                                     final Map<State, PostPerimeterResult> contingencyOptimizationResults,
                                                     final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                     final Unit unit) {

        // get list of the most limiting element (preventive, auto and curative perimeter combined)
        final boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        final Map<FlowCnec, Double> mostLimitingElementsAndMargins =
            ReportUtils.getMostLimitingElementsAndMargins(basecaseOptimResult, preventivePerimeter.getAllStates(), unit, relativePositiveMargins, 1);

        contingencyScenarios.forEach(contingencyScenario -> {
            final Optional<State> automatonState = contingencyScenario.getAutomatonState();
            automatonState.ifPresent(state -> mostLimitingElementsAndMargins.putAll(
                ReportUtils.getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(state).optimizationResult(), Set.of(state), unit, relativePositiveMargins, 1)
            ));
            contingencyScenario.getCurativePerimeters()
                .forEach(
                    curativePerimeter -> mostLimitingElementsAndMargins.putAll(
                        ReportUtils.getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(curativePerimeter.getRaOptimisationState()).optimizationResult(), Set.of(curativePerimeter.getRaOptimisationState()), unit, relativePositiveMargins, 1)
                    )
                );
        });

        return mostLimitingElementsAndMargins.keySet().stream()
            .sorted(Comparator.comparing(mostLimitingElementsAndMargins::get))
            .toList();
    }

    public static void reportIfMostLimitingElementIsFictional(final ReportNode parentNode, final ObjectiveFunctionResult objectiveFunctionResult) {
        final String mostLimitingCnecId = objectiveFunctionResult.getMostLimitingElements(1).getFirst().getId();
        reportIfMostLimitingElementIsFictional(parentNode, mostLimitingCnecId);
    }

    public static void reportIfMostLimitingElementIsFictional(final ReportNode parentNode, final String mostLimitingCnecId) {
        if (mostLimitingCnecId.contains("OUTAGE_DUPLICATE")) {
            parentNode.newReportNode()
                .withMessageTemplate("openrao.searchtreerao.reportIfMostLimitingElementIsFictional")
                .withSeverity(INFO_SEVERITY)
                .add();

            BUSINESS_LOGS.info("Limiting element is a fictional CNEC that is excluded from final cost computation");
        }
    }

    public static void reportRaoFailure(final ReportNode parentNode, final String stateId, final Exception exception) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRaoFailure")
            .withUntypedValue("stateId", stateId)
            .withUntypedValue("exception", Objects.toString(exception))
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Optimizing state \"{}\" failed: {}", stateId, exception);
    }

    public static void reportOptimizingScenarioPostContingency(final ReportNode parentNode, final String contingencyId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportOptimizingScenarioPostContingency")
            .withUntypedValue("contingencyId", contingencyId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Optimizing scenario post-contingency {}.", contingencyId);
    }

    public static void reportRemainingPostContingencyScenariosToOptimize(final ReportNode parentNode, final int nbOfRemainingScenarios) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRemainingPostContingencyScenariosToOptimize")
            .withUntypedValue("nbOfRemainingScenarios", nbOfRemainingScenarios)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("Remaining post-contingency scenarios to optimize: {}", nbOfRemainingScenarios);
    }

    public static void reportOptimizingCurativeState(final ReportNode parentNode, final String stateId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportOptimizingCurativeState")
            .withUntypedValue("stateId", stateId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Optimizing curative state {}.", stateId);
    }

    public static void reportCurativeStateOptimized(final ReportNode parentNode, final String stateId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCurativeStateOptimized")
            .withUntypedValue("stateId", stateId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Curative state {} has been optimized.", stateId);
    }

    public static ReportNode reportPreventivePerimeterOptimization(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPreventivePerimeterOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static void reportPreventivePerimeterOptimizationStart(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPreventivePerimeterOptimizationStart")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");
    }

    public static void reportPreventivePerimeterOptimizationEnd(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPreventivePerimeterOptimizationEnd")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
    }

    public static ReportNode reportPostPraSensiAnalysis(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPostPraSensiAnalysis")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportCurativePerimeterOptimization(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCurativePerimeterOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSecondPreventivePerimeterOptimization(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventivePerimeterOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static void reportSecondPreventivePerimeterOptimizationStart(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventivePerimeterOptimizationStart")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
    }

    public static void reportSecondPreventivePerimeterOptimizationEnd(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventivePerimeterOptimizationEnd")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
    }

    public static void reportSystematicSensitivityAnalysisAfterPraFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSystematicSensitivityAnalysisAfterPraFailed")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
    }

    public static void reportPreventivePerimeterNotSecure(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPreventivePerimeterNotSecure")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
    }

    public static void reportPostContingencyPerimeterOptimizationStart(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPostContingencyPerimeterOptimizationStart")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
    }

    public static void reportPostContingencyPerimeterOptimizationEnd(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPostContingencyPerimeterOptimizationEnd")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");
    }

    public static ReportNode reportMergingPreventiveAndPostContingencyRaoResults(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMergingPreventiveAndPostContingencyRaoResults")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");

        return addedNode;
    }

    public static void reportSecondPreventiveFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventiveFailed")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
    }

    public static void reportSecondPreventiveMadeRaoSucceed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventiveMadeRaoSucceed")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
    }

    public static void reportSecondPreventiveIncreasedOverallCost(final ReportNode parentNode,
                                                                  final double firstPreventiveCost,
                                                                  final double secondPreventiveCost,
                                                                  final Instant curativeInstant,
                                                                  final RaoResult mergedRaoResults,
                                                                  final RaoResult secondPreventiveRaoResults) {
        final String firstPreventiveCostFormatted = ReportUtils.formatDoubleBasedOnMargin(firstPreventiveCost, -firstPreventiveCost);
        final String firstPreventiveFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(mergedRaoResults.getFunctionalCost(curativeInstant), -firstPreventiveCost);
        final String firstPreventiveVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(mergedRaoResults.getVirtualCost(curativeInstant), -firstPreventiveCost);
        final String secondPreventiveCostFormatted = ReportUtils.formatDoubleBasedOnMargin(secondPreventiveCost, -secondPreventiveCost);
        final String secondPreventiveFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(secondPreventiveRaoResults.getFunctionalCost(curativeInstant), -secondPreventiveCost);
        final String secondPreventiveVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(secondPreventiveRaoResults.getVirtualCost(curativeInstant), -secondPreventiveCost);

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventiveIncreasedOverallCost")
            .withUntypedValue("firstPreventiveCost", firstPreventiveCostFormatted)
            .withUntypedValue("firstPreventiveFunctionalCost", firstPreventiveFunctionalCostFormatted)
            .withUntypedValue("firstPreventiveVirtualCost", firstPreventiveVirtualCostFormatted)
            .withUntypedValue("secondPreventiveCost", secondPreventiveCostFormatted)
            .withUntypedValue("secondPreventiveFunctionalCost", secondPreventiveFunctionalCostFormatted)
            .withUntypedValue("secondPreventiveVirtualCost", secondPreventiveVirtualCostFormatted)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
            firstPreventiveCostFormatted, firstPreventiveFunctionalCostFormatted, firstPreventiveVirtualCostFormatted,
            secondPreventiveCostFormatted, secondPreventiveFunctionalCostFormatted, secondPreventiveVirtualCostFormatted);
    }

    public static void reportRaoIncreasedOverallCost(final ReportNode parentNode,
                                                     final double initialCost,
                                                     final double initialFunctionalCost,
                                                     final double initialVirtualCost,
                                                     final double finalCost,
                                                     final double finalFunctionalCost,
                                                     final double finalVirtualCost) {
        final String initialCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialCost, -initialCost);
        final String initialFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialFunctionalCost, -initialCost);
        final String initialVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialVirtualCost, -initialCost);
        final String finalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalCost, -finalCost);
        final String finalFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalFunctionalCost, -finalCost);
        final String finalVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalVirtualCost, -finalCost);

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRaoIncreasedOverallCost")
            .withUntypedValue("initialCost", initialCostFormatted)
            .withUntypedValue("initialFunctionalCost", initialFunctionalCostFormatted)
            .withUntypedValue("initialVirtualCost", initialVirtualCostFormatted)
            .withUntypedValue("finalCost", finalCostFormatted)
            .withUntypedValue("finalFunctionalCost", finalFunctionalCostFormatted)
            .withUntypedValue("finalVirtualCost", finalVirtualCostFormatted)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
            initialCostFormatted, initialFunctionalCostFormatted, initialVirtualCostFormatted,
            finalCostFormatted, finalFunctionalCostFormatted, finalVirtualCostFormatted);
    }

    public static void reportCostsBeforeAndAfterRao(final ReportNode parentNode,
                                                    final double initialCost,
                                                    final double initialFunctionalCost,
                                                    final double initialVirtualCost,
                                                    final ObjectiveFunctionResult initialResult,
                                                    final double finalCost,
                                                    final double finalFunctionalCost,
                                                    final double finalVirtualCost,
                                                    final RaoResult finalRaoResult,
                                                    final Instant instant) {
        final Map<String, Double> initialVirtualCostDetailed = RaoLogger.getVirtualCostDetailed(initialResult);
        final Map<String, Double> finalVirtualCostDetailed = getVirtualCostDetailed(finalRaoResult, instant);

        final String initialCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialCost, -initialCost);
        final String initialFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialFunctionalCost, -initialCost);
        final String initialVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(initialVirtualCost, -initialCost);
        final String initialVirtualCostDetailedFormatted = initialVirtualCostDetailed.isEmpty() ? "" : " " + initialVirtualCostDetailed;
        final String finalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalCost, -finalCost);
        final String finalFunctionalCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalFunctionalCost, -finalCost);
        final String finalVirtualCostFormatted = ReportUtils.formatDoubleBasedOnMargin(finalVirtualCost, -finalCost);
        final String finalVirtualCostDetailedFormatted = finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed;

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCostsBeforeAndAfterRao")
            .withUntypedValue("initialCost", initialCostFormatted)
            .withUntypedValue("initialFunctionalCost", initialFunctionalCostFormatted)
            .withUntypedValue("initialVirtualCost", initialVirtualCostFormatted)
            .withUntypedValue("initialVirtualCostDetail", initialVirtualCostDetailedFormatted)
            .withUntypedValue("finalCost", finalCostFormatted)
            .withUntypedValue("finalFunctionalCost", finalFunctionalCostFormatted)
            .withUntypedValue("finalVirtualCost", finalVirtualCostFormatted)
            .withUntypedValue("finalVirtualCostDetail", finalVirtualCostDetailedFormatted)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}{}), cost after RAO = {} (functional: {}, virtual: {}{})",
            initialCostFormatted,
            initialFunctionalCostFormatted,
            initialVirtualCostFormatted,
            initialVirtualCostDetailedFormatted,
            finalCostFormatted,
            finalFunctionalCostFormatted,
            finalVirtualCostFormatted,
            finalVirtualCostDetailedFormatted);
    }

    public static Map<String, Double> getVirtualCostDetailed(RaoResult raoResult, Instant instant) {
        return raoResult.getVirtualCostNames().stream()
            .filter(virtualCostName -> raoResult.getVirtualCost(instant, virtualCostName) > 1e-6)
            .collect(Collectors.toMap(Function.identity(),
                name -> Math.round(raoResult.getVirtualCost(instant, name) * 100.0) / 100.0));
    }

    public static void reportNotEnoughTimeToRunSecondPreventiveRao(final ReportNode parentNode,
                                                                   final java.time.Instant targetEndInstant,
                                                                   final long estimatedPreventiveRaoTimeInSeconds) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNotEnoughTimeToRunSecondPreventiveRao")
            .withUntypedValue("targetEndInstant", Objects.toString(targetEndInstant))
            .withUntypedValue("estimatedPreventiveRaoTimeInSeconds", estimatedPreventiveRaoTimeInSeconds)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
    }

    public static void reportCostNotIncreasedDuringRao(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCostNotIncreasedDuringRao")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
    }

    public static void reportSecondPreventiveCancelledToSaveComputationTime(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondPreventiveCancelledToSaveComputationTime")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
    }

    public static void reportExceptionMessageAndStacktrace(final ReportNode parentNode, final RuntimeException exception) {
        final String exceptionMessage = exception.getMessage();
        final String stackTrace = ExceptionUtils.getStackTrace(exception);
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportExceptionMessageAndStacktrace")
            .withUntypedValue("exceptionMessage", exceptionMessage)
            .withUntypedValue("stackTrace", stackTrace)
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("{} \n {}", exceptionMessage, stackTrace);
    }

    public static ReportNode reportSecondAutomatonSimulation(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondAutomatonSimulation")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static void reportSecondAutomatonSimulationStart(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondAutomatonSimulationStart")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
    }

    public static void reportSecondAutomatonSimulationEnd(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSecondAutomatonSimulationEnd")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- Second automaton simulation [end]");
    }

    public static void reportMergingFirstSecondPreventiveAndPostContingencyRaoResults(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMergingFirstSecondPreventiveAndPostContingencyRaoResults")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
    }

    public static void reportSystematicSensitivityAnalysisAfterCraAfterSecondPreventiveFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSystematicSensitivityAnalysisAfterCraAfterSecondPreventiveFailed")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
    }

    public static void reportSystematicSensitivityAnalysisAfterPraAfterSecondPreventiveFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSystematicSensitivityAnalysisAfterPraAfterSecondPreventiveFailed")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
    }

    public static ReportNode reportMergingRaoAndPstRegulationResults(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMergingRaoAndPstRegulationResults")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Merging RAO and PST regulation results:");

        return addedNode;
    }

    public static ReportNode reportPstRegulation(final ReportNode parentNode) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPstRegulation")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static void reportPstRegulationStart(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPstRegulationStart")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- PST regulation [start]");
    }

    public static void reportPstRegulationEnd(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPstRegulationEnd")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("----- PST regulation [end]");
    }
}
