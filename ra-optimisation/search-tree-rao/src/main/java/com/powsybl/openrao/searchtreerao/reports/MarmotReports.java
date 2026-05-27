/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;
import com.powsybl.openrao.searchtreerao.result.api.LinearOptimizationResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class MarmotReports {
    private MarmotReports() {
        // Utility class should not be instantiated
    }

    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

    public static ReportNode reportMissingMarmotParametersExtension(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMissingMarmotParametersExtension")
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Parameters are missing MarmotParameters extension. Default MarmotParameters will be used");

        return addedNode;
    }

    public static void reportMarmotOptimizerSetToWorkOnNThreads(final ReportNode parentNode, final int parallelism) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotOptimizerSetToWorkOnNThreads")
            .withUntypedValue("parallelism", parallelism)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Optimizer set to work on {} threads", parallelism);
    }

    public static ReportNode reportMarmotRunningInitialSensiAnalyses(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotRunningInitialSensiAnalyses")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] ----- Running initial sensitivity analyses [start]");

        return addedNode;
    }

    public static void reportMarmotRunningInitialSensiAnalysesEnd() {
        TECHNICAL_LOGS.info("[MARMOT] ----- Running initial sensitivity analyses [end]");
    }

    public static ReportNode reportMarmotEvaluatingInitialValueOfGlobalObjFunction(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotEvaluatingInitialValueOfGlobalObjFunction")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] ----- Evaluating the initial value of the global objective function [start]");

        return addedNode;
    }

    public static void reportMarmotEvaluatingInitialValueOfGlobalObjFunctionEnd() {
        TECHNICAL_LOGS.info("[MARMOT] ----- Evaluating the initial value of the global objective function [end]");
    }

    public static void reportMarmotInfeasibleGlobalMip(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotInfeasibleGlobalMip")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.warn("[MARMOT] The global MIP was infeasible, possibly due to time-coupled constraints that are incoherent/inconsistent or that cannot be met. Rolling back to initial situation.");
    }

    public static ReportNode reportMarmotRunningRaoForTimestamp(final ReportNode parentNode, final OffsetDateTime timestamp) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotRunningRaoForTimestamp")
            .withUntypedValue("timestamp", timestamp.toString())
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] >>> Running RAO for timestamp {} [start]", timestamp);

        return addedNode;
    }

    public static void reportMarmotRunningRaoForTimestampEnd(final OffsetDateTime timestamp) {
        TECHNICAL_LOGS.info("[MARMOT] >>> Running RAO for timestamp {} [end]", timestamp);
    }

    public static void reportMarmotApplyingPraAfterOptimForTimestamp(final ReportNode parentNode, final OffsetDateTime timestamp) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotApplyingPraAfterOptimForTimestamp")
            .withUntypedValue("timestamp", timestamp.toString())
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] >>> Applying preventive remedial actions after optimization for timestamp {}", timestamp);
    }

    public static void reportMarmotUnoptimizedRaoResult(final ReportNode parentNode,
                                                        final LinearOptimizationResult sensitivityAnalysisResult,
                                                        final RaoParameters parameters,
                                                        final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportMarmotUnoptimizedRaoResult";
        final String prefix = "[MARMOT] Unoptimized RAO results: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportMarmotNextIterationOfMip(final ReportNode parentNode,
                                                      final LinearOptimizationResult sensitivityAnalysisResult,
                                                      final RaoParameters parameters,
                                                      final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportMarmotNextIterationOfMip";
        final String prefix = "[MARMOT] Next iteration of MIP: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportMarmotResultBeforeTopologicalOptimization(final ReportNode parentNode,
                                                                       final LinearOptimizationResult sensitivityAnalysisResult,
                                                                       final RaoParameters parameters,
                                                                       final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportMarmotResultBeforeTopologicalOptimization";
        final String prefix = "[MARMOT] Before topological optimizations: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportMarmotResultBeforeGlobalLinearOptimization(final ReportNode parentNode,
                                                                        final LinearOptimizationResult sensitivityAnalysisResult,
                                                                        final RaoParameters parameters,
                                                                        final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportMarmotResultBeforeGlobalLinearOptimization";
        final String prefix = "[MARMOT] Before global linear optimization: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportMarmotResultAfterGlobalLinearOptimization(final ReportNode parentNode,
                                                                       final LinearOptimizationResult sensitivityAnalysisResult,
                                                                       final RaoParameters parameters,
                                                                       final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportMarmotResultAfterGlobalLinearOptimization";
        final String prefix = "[MARMOT] After global linear optimization: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static ReportNode reportMarmotTopologicalOptimization(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotTopologicalOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [start]");

        return addedNode;
    }

    public static void reportMarmotTopologicalOptimizationEnd() {
        TECHNICAL_LOGS.info("[MARMOT] ----- Topological optimization [end]");
    }

    public static void reportMarmotNoTimeCoupledConstraintProvided(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotNoTimeCoupledConstraintProvided")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] No time-coupled constraint provided; no need to re-optimize range actions");
    }

    public static ReportNode reportMarmotGlobalRangeActionsOptimization(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotGlobalRangeActionsOptimization")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start]");

        return addedNode;
    }

    public static void reportMarmotGlobalRangeActionsOptimizationEnd() {
        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end]");
    }

    public static ReportNode reportMarmotGlobalRangeActionsOptimizationForIteration(final ReportNode parentNode, final int iterationCounter) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotGlobalRangeActionsOptimizationForIteration")
            .withUntypedValue("iteration", iterationCounter)
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [start] for iteration {}", iterationCounter);

        return addedNode;
    }

    public static void reportMarmotGlobalRangeActionsOptimizationForIterationEnd(final int iterationCounter) {
        TECHNICAL_LOGS.info("[MARMOT] ----- Global range actions optimization [end] for iteration {}", iterationCounter);
    }

    public static ReportNode reportMarmotSystematicTimeCoupledSensitivityAnalysis(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotSystematicTimeCoupledSensitivityAnalysis")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [start]");

        return addedNode;
    }

    public static void reportMarmotSystematicTimeCoupledSensitivityAnalysisEnd() {
        TECHNICAL_LOGS.info("[MARMOT] Systematic time-coupled sensitivity analysis [end]");
    }

    public static ReportNode reportMarmotMergingTopoAndLinearRemedialActionResults(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotMergingTopoAndLinearRemedialActionResults")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Merging topological and linear remedial action results");

        return addedNode;
    }

    public static void reportMarmotCnecs(final ReportNode parentNode, final List<Marmot.LoggingAddedCnecs> addedCnecsForLogging) {
        final StringBuilder logMessage = new StringBuilder();
        final AtomicInteger nbAddedCnecs = new AtomicInteger();
        final AtomicInteger nbTimestamps = new AtomicInteger();

        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotCnecs")
            .withSeverity(TRACE_SEVERITY)
            .add();
        logMessage.append("[MARMOT] Proceeding to next iteration by adding:");

        addedCnecsForLogging.stream()
            .filter(loggingAddedCnecs -> !loggingAddedCnecs.addedCnecs().isEmpty())
            .map(loggingAddedCnecs -> {
                nbTimestamps.incrementAndGet();
                final String timestamp = loggingAddedCnecs.offsetDateTime().toString();
                final String vcName = loggingAddedCnecs.vcName();

                nbAddedCnecs.addAndGet(loggingAddedCnecs.addedCnecs().size());
                Stream<String> addedCnecsStream = loggingAddedCnecs.addedCnecs().stream();
                if (vcName.equals(MIN_MARGIN_VIOLATION_EVALUATOR)) {
                    addedCnecsStream = addedCnecsStream.map(cnec -> cnec + "(" + loggingAddedCnecs.margins().get(cnec) + ")");
                }
                return new MarmotAddedCnecsElement(timestamp, vcName, addedCnecsStream.collect(Collectors.joining(",")));
            })
            .forEach(addedCnecsElement -> {
                addedNode.newReportNode()
                    .withMessageTemplate("openrao.searchtreerao.reportMarmotCnecsForTimestampAndVirtualCost")
                    .withUntypedValue("timestamp", addedCnecsElement.timestamp())
                    .withUntypedValue("vcName", addedCnecsElement.vcName())
                    .withUntypedValue("addedCnecs", addedCnecsElement.addedCnecs())
                    .withSeverity(TRACE_SEVERITY)
                    .add();

                logMessage.append(" for timestamp ").append(addedCnecsElement.timestamp()).append(" and virtual cost ").append(addedCnecsElement.vcName()).append(" ").append(addedCnecsElement.addedCnecs()).append(",");
            });

        addedNode.addUntypedValue("nbAddedCnecs", nbAddedCnecs.get());
        addedNode.addUntypedValue("nbTimestamps", nbTimestamps.get());
        TECHNICAL_LOGS.info(logMessage.toString());
    }

    public static void reportMarmotNoPreventiveTopologicalActionsAppliedForTimestamp(final ReportNode parentNode, final OffsetDateTime timestamp) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotNoPreventiveTopologicalActionsAppliedForTimestamp")
            .withUntypedValue("timestamp", timestamp.toString())
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] No preventive topological actions applied for timestamp {}", timestamp);
    }

    record MarmotAddedCnecsElement(String timestamp, String vcName, String addedCnecs) {
    }
}
