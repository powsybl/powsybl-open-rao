package com.powsybl.openrao.searchtreerao.linearoptimisation;

import com.powsybl.commons.report.ReportNode;

import java.util.Locale;

import static com.powsybl.commons.report.TypedValue.*;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class LinearOptimisationReports {
    private static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }

    private LinearOptimisationReports() {
        // Utility class
    }

    public static ReportNode reportLinearOptimization(ReportNode reportNode) {
        return reportNode.newReportNode() // TODO test this
            .withMessageTemplate("reportLinearOptimization", "Linear optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportLinearOptimizationFeasible(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFeasible", "The solver was interrupted. A feasible solution has been produced.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced."); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationFailed(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFailed", "Linear optimization failed at iteration ${iteration}")
            .withUntypedValue("iteration", iteration)
            .withSeverity(ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationFailedAtFirstIteration(ReportNode reportNode, String solveStatus) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFailedAtFirstIteration", "Linear problem failed with the following status : ${solveStatus}, initial situation is kept.")
            .withUntypedValue("solveStatus", solveStatus)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Linear problem failed with the following status : {}, initial situation is kept.", solveStatus); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationSameResultAsPreviousIteration(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationSameResultAsPreviousIteration", "Iteration ${iteration}: same results as previous iterations, optimal solution found")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearProblemSolveStart(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearProblemSolveStart", "Iteration ${iteration}: linear optimization")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearProblemSolveEnd(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearProblemSolveEnd", "End of iteration ${iteration}: linear optimization")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationSystematicSensitivityComputationFailed(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationSystematicSensitivityComputationFailed", "Systematic sensitivity computation failed at iteration ${iteration}")
            .withUntypedValue("iteration", iteration)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationBetterResult(ReportNode reportNode, int iteration, double cost, double functionalCost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationBetterResult", "Iteration ${iteration}: better solution found with a cost of ${cost} (functional: ${functionalCost})")
            .withUntypedValue("iteration", iteration)
            .withUntypedValue("cost", formatDouble(cost))
            .withUntypedValue("functionalCost", formatDouble(functionalCost))
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Iteration {}: better solution found with a cost of {} (functional: {})", iteration, // TODO test this
            formatDouble(cost), formatDouble(functionalCost));
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationWorseResult(ReportNode reportNode, int iteration, double bestCost, double currentCost, double bestFunctionalCost, double currentFunctionalCost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationWorseResult", "Iteration ${iteration}: linear optimization found a worse result than best iteration, with a cost increasing from ${bestCost} to ${currentCost} (functional: from ${bestFunctionalCost} to ${currentFunctionalCost})")
            .withUntypedValue("iteration", iteration)
            .withUntypedValue("bestCost", formatDouble(bestCost))
            .withUntypedValue("currentCost", formatDouble(currentCost))
            .withUntypedValue("bestFunctionalCost", formatDouble(bestFunctionalCost))
            .withUntypedValue("currentFunctionalCost", formatDouble(currentFunctionalCost))
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})", iteration, // TODO test this
            formatDouble(bestCost), formatDouble(currentCost), formatDouble(bestFunctionalCost), formatDouble(currentFunctionalCost));
        return addedNode;
    }

}
