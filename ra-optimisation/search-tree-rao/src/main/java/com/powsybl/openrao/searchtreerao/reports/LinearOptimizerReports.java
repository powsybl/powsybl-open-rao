/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;

import java.util.Locale;
import java.util.Objects;

import static com.powsybl.commons.report.TypedValue.ERROR_SEVERITY;
import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class LinearOptimizerReports {
    private LinearOptimizerReports() {
        // Utility class should not be instantiated
    }

    public static void reportCostOf(final ReportNode parentNode, final String costName, final double cost) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportCostOf")
            .withUntypedValue("costName", costName)
            .withUntypedValue("cost", cost)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("{} cost of {}", costName, cost);
    }

    public static void reportLinearOptimFoundWorseResult(final ReportNode parentNode,
                                                         final int iteration,
                                                         final double bestResultCost,
                                                         final double currentResultCost,
                                                         final double bestResultFunctionalCost,
                                                         final double currentResultFunctionalCost) {
        final String bestResultCostFormatted = String.format(Locale.ENGLISH, "%.2f", bestResultCost);
        final String currentResultCostFormatted = String.format(Locale.ENGLISH, "%.2f", currentResultCost);
        final String bestResultFunctionalCostFormatted = String.format(Locale.ENGLISH, "%.2f", bestResultFunctionalCost);
        final String currentResultFunctionalCostFormatted = String.format(Locale.ENGLISH, "%.2f", currentResultFunctionalCost);
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearOptimFoundWorseResult")
            .withUntypedValue("iteration", iteration)
            .withUntypedValue("bestResultCost", bestResultCostFormatted)
            .withUntypedValue("currentResultCost", currentResultCostFormatted)
            .withUntypedValue("bestResultFunctionalCost", bestResultFunctionalCostFormatted)
            .withUntypedValue("currentResultFunctionalCost", currentResultFunctionalCostFormatted)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info(
            "Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})",
            iteration, bestResultCostFormatted, currentResultCostFormatted, bestResultFunctionalCostFormatted, currentResultFunctionalCostFormatted);
    }

    public static void reportLinearOptimFoundBetterSolution(final ReportNode parentNode,
                                                            final int iteration,
                                                            final double cost,
                                                            final double functionalCost) {
        final String costFormatted = String.format(Locale.ENGLISH, "%.2f", cost);
        final String functionalCostFormatted = String.format(Locale.ENGLISH, "%.2f", functionalCost);
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearOptimFoundBetterSolution")
            .withUntypedValue("iteration", iteration)
            .withUntypedValue("cost", costFormatted)
            .withUntypedValue("functionalCost", functionalCostFormatted)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Iteration {}: better solution found with a cost of {} (functional: {})", iteration, costFormatted, functionalCostFormatted);
    }

    public static void reportSystematicSensitivityComputationFailedAtIteration(final ReportNode parentNode, final int iteration) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSystematicSensitivityComputationFailedAtIteration")
            .withUntypedValue("iteration", iteration)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
    }

    public static void reportSolverInterrupted(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSolverInterrupted")
            .withSeverity(WARN_SEVERITY)
            .add();

        TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
    }

    public static void reportLinearOptimizationFailedAtIteration(final ReportNode parentNode, final int iteration) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearOptimizationFailedAtIteration")
            .withUntypedValue("iteration", iteration)
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
    }

    public static void reportLinearProblemFailedWithStatus(final ReportNode parentNode, final LinearProblemStatus status) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearProblemFailedWithStatus")
            .withUntypedValue("status", Objects.toString(status))
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("Linear problem failed with the following status : {}, initial situation is kept.", status);
    }

    public static void reportSameResultAsPreviousIterations(final ReportNode parentNode, final int iteration) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSameResultAsPreviousIterations")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
    }
}
