/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;

import java.util.Locale;

import static com.powsybl.commons.report.TypedValue.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class SearchTreeReports {
    private static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }

    private static String formatIndex(int index) {
        return String.format("#%02d", index);
    }

    private static OpenRaoLogger logger(TypedValue severity) {
        return INFO_SEVERITY.equals(severity) ? BUSINESS_LOGS : TECHNICAL_LOGS;
    }

    private SearchTreeReports() {
    }

    public static ReportNode reportSensitivityAnalysisResults(ReportNode reportNode, String prefix, double cost, double functionalCost, double virtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("sensitivityAnalysisResults", prefix + "cost = ${cost} (functional: ${functionalCost}, virtual: ${virtualCost})")
                .withUntypedValue("cost", cost)
                .withUntypedValue("functionalCost", functionalCost)
                .withUntypedValue("virtualCost", virtualCost)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("{}cost = {} (functional: {}, virtual: {})", prefix, formatDouble(cost), formatDouble(functionalCost), formatDouble(virtualCost));
        return addedNode;

    }

    public static ReportNode reportMostLimitingElement(ReportNode reportNode, TypedValue severity, int index, String isRelativeMargin, double cnecMargin, Unit unit, String ptdfIfRelative, String cnecNetworkElementName, String cnecStateId, String cnecId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("mostLimitingElement", "Limiting element ${index}:${isRelativeMargin} margin = ${cnecMargin} ${unit}${ptdfIfRelative}, element ${cnecNetworkElementName} at state ${cnecStateId}, CNEC ID = '${cnecId}'")
                .withUntypedValue("index", formatIndex(index))
                .withUntypedValue("isRelativeMargin", isRelativeMargin)
                .withUntypedValue("cnecMargin", formatDouble(cnecMargin))
                .withUntypedValue("unit", unit.toString())
                .withUntypedValue("ptdfIfRelative", ptdfIfRelative)
                .withUntypedValue("cnecNetworkElementName", cnecNetworkElementName)
                .withUntypedValue("cnecStateId", cnecStateId)
                .withUntypedValue("cnecId", cnecId)
                .withSeverity(severity)
                .add();
        String message = String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s%s, element %s at state %s, CNEC ID = \"%s\"", index, isRelativeMargin, cnecMargin, unit, ptdfIfRelative, cnecNetworkElementName, cnecStateId, cnecId);
        logger(severity).info(message);
        return addedNode;
    }

    public static ReportNode reportRootLeafEvaluation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("rootLeafEvaluation", "Evaluating root leaf")
                .withSeverity(DEBUG_SEVERITY)
                .add();
        TECHNICAL_LOGS.debug("Evaluating root leaf");
        return addedNode;
    }

    public static ReportNode reportLeafEvaluationError(ReportNode reportNode, String rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("leafEvaluationError", "Could not evaluate leaf: ${leafDescription}")
                .withUntypedValue("leafDescription", rootLeaf)
                .withSeverity(severity)
                .add();
        logger(severity).info("Could not evaluate leaf: {}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportOptimizationSummaryOnScenario(ReportNode reportNode, String scenarioName, String initialCost, String initialFunctionalCost, String initialVirtualCost, String initialVirtualCostDetailed, String raResult, String instant, double finalCost, double finalFunctionalCost, double finalVirtualCost, String finalVirtualCostDetailed) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("optimizationSummaryOnScenario", "Scenario '${scenarioName}': initial cost = ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}, detail: ${initialVirtualCostDetailed}), ${raResult}, cost after ${instant} optimization = ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost}, detail: ${finalVirtualCostDetailed})")
                .withUntypedValue("scenarioName", scenarioName)
                .withUntypedValue("initialCost", initialCost)
                .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
                .withUntypedValue("initialVirtualCost", initialVirtualCost)
                .withUntypedValue("initialVirtualCostDetailed", initialVirtualCostDetailed)
                .withUntypedValue("raResult", raResult)
                .withUntypedValue("instant", instant)
                .withUntypedValue("finalCost", formatDouble(finalCost))
                .withUntypedValue("finalFunctionalCost", formatDouble(finalFunctionalCost))
                .withUntypedValue("finalVirtualCost", formatDouble(finalVirtualCost))
                .withUntypedValue("finalVirtualCostDetailed", finalVirtualCostDetailed)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Scenario \"{}\": initial cost = {} (functional: {}, virtual: {}, detail: {}), {}, cost after {} optimization = {} (functional: {}, virtual: {}, detail: {})",
                scenarioName,
                initialCost, initialFunctionalCost, initialVirtualCost, initialVirtualCostDetailed,
                raResult, instant,
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost), finalVirtualCostDetailed);
        return addedNode;
    }

    public static ReportNode reportVirtualCostDetail(ReportNode reportNode, String prefix, String identifier, String virtualCostName, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("virtualCostDetail", "${prefix}${identifier}, stop criterion could have been reached without '${virtualCostName}' virtual cost")
                .withUntypedValue("prefix", prefix)
                .withUntypedValue("identifier", identifier)
                .withUntypedValue("virtualCostName", virtualCostName)
                .withSeverity(severity)
                .add();
        logger(severity).info("{}{}, stop criterion could have been reached without \"{}\" virtual cost", prefix, identifier, virtualCostName);
        return addedNode;
    }

    public static ReportNode reportVirtualCostlyElementsLog(ReportNode reportNode, String prefix, String identifier, String virtualCostName, int index, double flow, Unit unit, double limitingThreshold, double margin, String networkElementId, String stateId, String cnecId, String cnecName, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("virtualCostlyElementsLog", "${prefix}${identifier}, limiting '${virtualCostName}' constraint ${index}: flow = ${flow} ${unit}, threshold = ${limitingThreshold} ${unit}, margin = ${margin} ${unit}, element ${networkElementId} at state ${stateId}, CNEC ID = '${cnecId}', CNEC name = '${cnecName}'")
                .withUntypedValue("prefix", prefix)
                .withUntypedValue("identifier", identifier)
                .withUntypedValue("virtualCostName", virtualCostName)
                .withUntypedValue("index", formatIndex(index))
                .withUntypedValue("flow", formatDouble(flow))
                .withUntypedValue("unit", unit.toString())
                .withUntypedValue("limitingThreshold", formatDouble(limitingThreshold))
                .withUntypedValue("margin", formatDouble(margin))
                .withUntypedValue("networkElementId", networkElementId)
                .withUntypedValue("stateId", stateId)
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("cnecName", cnecName)
                .withSeverity(severity)
                .add();
        String message = String.format(Locale.ENGLISH,
                "%s%s, limiting \"%s\" constraint #%02d: flow = %.2f %s, threshold = %.2f %s, margin = %.2f %s, element %s at state %s, CNEC ID = \"%s\", CNEC name = \"%s\"",
                prefix,
                identifier,
                virtualCostName,
                index,
                flow, unit,
                limitingThreshold, unit,
                margin, unit,
                networkElementId, stateId,
                cnecId, cnecName);
        logger(severity).info(message);
        return addedNode;
    }

    public static ReportNode reportFailedOptimizationSummary(ReportNode reportNode, String scenarioName, String raResult) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportFailedOptimizationSummary", "Scenario \"${scenarioName}\": ${raResult}")
            .withUntypedValue("scenarioName", scenarioName)
            .withUntypedValue("raResult", raResult)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Scenario \"{}\": {}", scenarioName, raResult);
        return addedNode;
    }

    public static ReportNode reportLoopflowComputationErrorLackOfReferenceProgramOrGlskProvider(ReportNode reportNode, String cracId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLoopflowComputationErrorLackOfReferenceProgramOrGlskProvider", "Loopflow computation cannot be performed on CRAC ${cracId} because it lacks a ReferenceProgram or a GlskProvider")
            .withUntypedValue("cracId", cracId)
            .withSeverity(ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Loopflow computation cannot be performed on CRAC %s because it lacks a ReferenceProgram or a GlskProvider", cracId);
        return addedNode;
    }

    public static ReportNode reportReferenceProgramWillBeGeneratedFromNetwork(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportReferenceProgramWillBeGeneratedFromNetwork", "A ReferenceProgram will be generated using information in the network file.")
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
        return addedNode;
    }

    public static ReportNode reportLinearOptimization(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimization", "Linear optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportLinearOptimizationFeasible(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFeasible", "The solver was interrupted. A feasible solution has been produced.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("The solver was interrupted. A feasible solution has been produced.");
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationFailed(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFailed", "Linear optimization failed at iteration ${iteration}")
            .withUntypedValue("iteration", iteration)
            .withSeverity(ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Linear optimization failed at iteration {}", iteration);
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationFailedAtFirstIteration(ReportNode reportNode, String solveStatus) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationFailedAtFirstIteration", "Linear problem failed with the following status : ${solveStatus}, initial situation is kept.")
            .withUntypedValue("solveStatus", solveStatus)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Linear problem failed with the following status : {}, initial situation is kept.", solveStatus);
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationSameResultAsPreviousIteration(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationSameResultAsPreviousIteration", "Iteration ${iteration}: same results as previous iterations, optimal solution found")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Iteration {}: same results as previous iterations, optimal solution found", iteration);
        return addedNode;
    }

    public static ReportNode reportLinearProblemSolveStart(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearProblemSolveStart", "Iteration ${iteration}: linear optimization [start]")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [start]", iteration);
        return addedNode;
    }

    public static ReportNode reportLinearProblemSolveEnd(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearProblemSolveEnd", "Iteration ${iteration}: linear optimization [end]")
            .withUntypedValue("iteration", iteration)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Iteration {}: linear optimization [end]", iteration);
        return addedNode;
    }

    public static ReportNode reportLinearOptimizationSystematicSensitivityComputationFailed(ReportNode reportNode, int iteration) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLinearOptimizationSystematicSensitivityComputationFailed", "Systematic sensitivity computation failed at iteration ${iteration}")
            .withUntypedValue("iteration", iteration)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Systematic sensitivity computation failed at iteration {}", iteration);
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
        TECHNICAL_LOGS.info("Iteration {}: better solution found with a cost of {} (functional: {})", iteration,
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
        TECHNICAL_LOGS.info("Iteration {}: linear optimization found a worse result than best iteration, with a cost increasing from {} to {} (functional: from {} to {})", iteration,
            formatDouble(bestCost), formatDouble(currentCost), formatDouble(bestFunctionalCost), formatDouble(currentFunctionalCost));
        return addedNode;
    }

    public static ReportNode reportSearchTreeStopCriterionReached(ReportNode reportNode, String rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeStopCriterionReached", "Stop criterion reached on ${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("Stop criterion reached on {}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeLeaf(ReportNode reportNode, String rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeaf", "${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("{}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeLinearOptimization(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLinearOptimization", "Linear optimization on root leaf")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        return addedNode;
    }

    public static ReportNode reportSearchTreeRaoCompleted(ReportNode reportNode, ComputationStatus sensitivityStatus) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeRaoCompleted", "Search-tree RAO completed with status ${sensitivityStatus}")
            .withUntypedValue("sensitivityStatus", sensitivityStatus.toString())
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", sensitivityStatus);
        return addedNode;
    }

    public static ReportNode reportSearchTreeBestLeaf(ReportNode reportNode, String optimalLeaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeBestLeaf", "Best leaf: ${optimalLeaf}")
            .withUntypedValue("optimalLeaf", optimalLeaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeNoNetworkActionAvailable(ReportNode reportNode, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeNoNetworkActionAvailable", "No network action available")
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("No network action available");
        return addedNode;
    }

    public static ReportNode reportSearchTreeLeavesInParallel(ReportNode reportNode, int leavesInParallel) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeavesInParallel", "Evaluating ${leavesInParallel} leaves in parallel")
            .withUntypedValue("leavesInParallel", leavesInParallel)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        return addedNode;
    }

    public static ReportNode reportSearchTreeDepthStart(ReportNode reportNode, int depth) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthStart", "Search depth ${depth} [start]")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search depth {} [start]", depth);
        return addedNode;
    }

    public static ReportNode reportSearchTreeDepthEnd(ReportNode reportNode, int depth) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthEnd", "Search depth ${depth} [end]")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search depth {} [end]", depth);
        return addedNode;
    }

    public static ReportNode reportSearchTreeBestLeafAtDepth(ReportNode reportNode, int depth, String optimalLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeBestLeafAtDepth", "Search depth ${depth} best leaf: ${optimalLeaf}")
            .withUntypedValue("depth", depth)
            .withUntypedValue("optimalLeaf", optimalLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("Search depth {} best leaf: {}", depth, optimalLeaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeDepthNoBetterResult(ReportNode reportNode, int depth, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthNoBetterResult", "No better result found in search depth ${depth}, exiting search tree")
            .withUntypedValue("depth", depth)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("No better result found in search depth {}, exiting search tree", depth);
        return addedNode;
    }

    public static ReportNode reportSearchTreeMaxDepth(ReportNode reportNode, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeMaxDepth", "maximum search depth has been reached, exiting search tree")
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("maximum search depth has been reached, exiting search tree");
        return addedNode;
    }

    public static ReportNode reportSearchTreeInterrupted(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeInterrupted", "A computation thread was interrupted")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("A computation thread was interrupted");
        return addedNode;
    }

    public static ReportNode reportSeachTreeNoMoreNetworkAction(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSeachTreeNoMoreNetworkAction", "No more network action available")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("No more network action available");
        return addedNode;
    }

    public static ReportNode reportSeachTreeLeavesToEvaluate(ReportNode reportNode, int numberOfCombinations) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSeachTreeLeavesToEvaluate", "Leaves to evaluate: ${numberOfCombination}")
            .withUntypedValue("numberOfCombinations", numberOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Leaves to evaluate: {}", numberOfCombinations);
        return addedNode;
    }

    public static ReportNode generateOneLeafRootReportNode() {
        return ReportNode.newRootReportNode()
            .withMessageTemplate("generateOneLeafRootReportNode", "Optimization of one search tree leaf")
            .build();
    }

    public static ReportNode reportSearchTreeOneLeaf(ReportNode reportNode, String concatenatedId, int remainingLeaves) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeaf", "Leaf with network actions ${concatenatedId} and ${remainingLeaves} remaining leaves")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withUntypedValue("remainingLeaves", remainingLeaves)
            .add();
    }

    public static ReportNode reportSearchTreeOneLeafSkipped(ReportNode reportNode, String concatenatedId, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafSkipped", "Skipping ${concatenatedId} optimization because earlier combination fulfills stop criterion.")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Skipping {} optimization because earlier combination fulfills stop criterion.", concatenatedId);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafCannotOptimize(ReportNode reportNode, String concatenatedId, String message) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafCannotOptimize", "Cannot optimize remedial action combination ${concatenatedId}: ${message}")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withUntypedValue("message", message)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Cannot optimize remedial action combination {}: {}", concatenatedId, message);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafRemainingLeaves(ReportNode reportNode, int remainingLeaves) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafRemainingLeaves", "Remaining leaves to evaluate: ${remainingLeaves}")
            .withUntypedValue("remainingLeaves", remainingLeaves)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafCouldNotEvaluateCombination(ReportNode reportNode, String networkActions, String message, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafCouldNotEvaluateCombination", "Could not evaluate network action combination \"${networkActions}\": ${message}")
            .withUntypedValue("networkActions", networkActions)
            .withUntypedValue("message", message)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Could not evaluate network action combination \"{}\": {}", networkActions, message);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafEvaluated(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafEvaluated", "Evaluated ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Evaluated {}", leaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafOptimized(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafOptimized", "Optimized ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Optimized {}", leaf);
        return addedNode;
    }

    public static ReportNode reportLeafFailedToOptimize(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLeafFailedToOptimize", "Failed to optimize leaf: ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Failed to optimize leaf: {}", leaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafNoRangeActionToOptimize(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafNoRangeActionToOptimize", "No range actions to optimize")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("No range actions to optimize");
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafStopCriterionReached(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafStopCriterionReached", "Stop criterion reached, other threads may skip optimization.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafPerimeterPurelyVirtual(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafPerimeterPurelyVirtual", "Perimeter is purely virtual and virtual cost is zero. Exiting search tree.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafNoRangeActionActivated(ReportNode reportNode, String prefix) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafNoRangeActionActivated", "${prefix}No range actions activated")
            .withUntypedValue("prefix", prefix)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{}No range actions activated", prefix);
        return addedNode;
    }

    public static ReportNode reportSearchTreeOneLeafRangeActionActivated(ReportNode reportNode, String prefix, String rangeActionSetpoints) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafRangeActionActivated", "${prefix}range action(s): ${rangeActionSetpoints}")
            .withUntypedValue("prefix", prefix)
            .withUntypedValue("rangeActionSetpoints", rangeActionSetpoints)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{}range action(s): {}", prefix, rangeActionSetpoints);
        return null;
    }
}
