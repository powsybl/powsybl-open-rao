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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class MarmotReports {
    private MarmotReports() {
        // Utility class should not be instantiated
    }

    private static final String MIN_MARGIN_VIOLATION_EVALUATOR = "min-margin-violation-evaluator";

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

    public static void reportMarmotApplyingOptimalTopologicalActionsOnNetworks(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotApplyingOptimalTopologicalActionsOnNetworks")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Applying optimal topological actions on networks");
    }

    public static void reportMarmotEvaluatingGlobalResultAfterIndependentOptimizations(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotEvaluatingGlobalResultAfterIndependentOptimizations")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Evaluating global result after independent optimizations");
    }

    public static void reportMarmotNoInterTemporalConstraintProvided(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotNoInterTemporalConstraintProvided")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] No inter-temporal constraint provided; no need to re-optimize range actions");
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

    public static ReportNode reportMarmotSystematicInterTemporalSensitivityAnalysis(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotSystematicInterTemporalSensitivityAnalysis")
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [start]");

        return addedNode;
    }

    public static void reportMarmotSystematicInterTemporalSensitivityAnalysisEnd() {
        TECHNICAL_LOGS.info("[MARMOT] Systematic inter-temporal sensitivity analysis [end]");
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

    public static ReportNode reportMarmotRunRaoForTimestamp(final ReportNode parentNode, final OffsetDateTime timestamp) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMarmotRunRaoForTimestamp")
            .withUntypedValue("timestamp", Objects.toString(timestamp))
            .withSeverity(INFO_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[MARMOT] Running RAO for timestamp {} [start]", timestamp);

        return addedNode;
    }

    public static void reportMarmotRunRaoForTimestampEnd(final OffsetDateTime timestamp) {
        TECHNICAL_LOGS.info("[MARMOT] Running RAO for timestamp {} [end]", timestamp);
    }

    record MarmotAddedCnecsElement(String timestamp, String vcName, String addedCnecs) {
    }
}
