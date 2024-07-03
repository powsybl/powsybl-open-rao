package com.powsybl.openrao.searchtreerao.searchtree;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;

import java.util.Locale;

import static com.powsybl.commons.report.TypedValue.*;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class SearchTreeReports {
    private static OpenRaoLogger logger(TypedValue severity) {
        return INFO_SEVERITY.equals(severity) ? BUSINESS_LOGS : TECHNICAL_LOGS;
    }

    private static String formatIndex(int index) {
        return String.format("#%02d", index);
    }

    private static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }

    private SearchTreeReports() {
        // Utility class
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

    public static ReportNode reportStopCriterionReached(ReportNode reportNode, String rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeStopCriterionReached", "Stop criterion reached on ${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("Stop criterion reached on {}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportLeaf(ReportNode reportNode, String rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeaf", "${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("{}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportLinearOptimization(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLinearOptimization", "Linear optimization on root leaf")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Linear optimization on root leaf");
        return addedNode;
    }

    public static ReportNode reportRaoCompleted(ReportNode reportNode, ComputationStatus sensitivityStatus) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeRaoCompleted", "Search-tree RAO completed with status ${sensitivityStatus}")
            .withUntypedValue("sensitivityStatus", sensitivityStatus.toString())
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search-tree RAO completed with status {}", sensitivityStatus);
        return addedNode;
    }

    public static ReportNode reportBestLeaf(ReportNode reportNode, String optimalLeaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeBestLeaf", "Best leaf: ${optimalLeaf}")
            .withUntypedValue("optimalLeaf", optimalLeaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        return addedNode;
    }

    public static ReportNode reportNoNetworkActionAvailable(ReportNode reportNode, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeNoNetworkActionAvailable", "No network action available")
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("No network action available");
        return addedNode;
    }

    public static ReportNode reportLeavesInParallel(ReportNode reportNode, int leavesInParallel) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeavesInParallel", "Evaluating ${leavesInParallel} leaves in parallel")
            .withUntypedValue("leavesInParallel", leavesInParallel)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Evaluating {} leaves in parallel", leavesInParallel);
        return addedNode;
    }

    public static ReportNode reportDepthStart(ReportNode reportNode, int depth) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthStart", "Search depth ${depth}")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search depth {} [start]", depth);
        return addedNode;
    }

    public static ReportNode reportDepthEnd(ReportNode reportNode, int depth) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthEnd", "End of search depth ${depth}")
            .withUntypedValue("depth", depth)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Search depth {} [end]", depth);
        return addedNode;
    }

    public static ReportNode reportBestLeafAtDepth(ReportNode reportNode, int depth, String optimalLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeBestLeafAtDepth", "Search depth ${depth} best leaf: ${optimalLeaf}")
            .withUntypedValue("depth", depth)
            .withUntypedValue("optimalLeaf", optimalLeaf)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("Search depth {} best leaf: {}", depth, optimalLeaf);
        return addedNode;
    }

    public static ReportNode reportDepthNoBetterResult(ReportNode reportNode, int depth, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeDepthNoBetterResult", "No better result found in search depth ${depth}, exiting search tree")
            .withUntypedValue("depth", depth)
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("No better result found in search depth {}, exiting search tree", depth);
        return addedNode;
    }

    public static ReportNode reportMaxDepth(ReportNode reportNode, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeMaxDepth", "maximum search depth has been reached, exiting search tree")
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("maximum search depth has been reached, exiting search tree");
        return addedNode;
    }

    public static ReportNode reportInterrupted(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeInterrupted", "A computation thread was interrupted")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("A computation thread was interrupted");
        return addedNode;
    }

    public static ReportNode reportNoMoreNetworkAction(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeNoMoreNetworkAction", "No more network action available")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("No more network action available");
        return addedNode;
    }

    public static ReportNode reportLeavesToEvaluate(ReportNode reportNode, int numberOfCombinations) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeavesToEvaluate", "Leaves to evaluate: ${numberOfCombinations}")
            .withUntypedValue("numberOfCombinations", numberOfCombinations)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Leaves to evaluate: {}", numberOfCombinations);
        return addedNode;
    }

    public static ReportNode generateOneLeafRootReportNode() {
        return ReportNode.newRootReportNode()
            .withMessageTemplate("generateOneLeafRootReportNode", "Optimization of one search tree leaf")
            .withSeverity(INFO_SEVERITY)
            .build();
    }

    public static ReportNode reportOneLeaf(ReportNode reportNode, String concatenatedId, int remainingLeaves) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeaf", "Leaf with network actions ${concatenatedId} and ${remainingLeaves} remaining leaves")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withUntypedValue("remainingLeaves", remainingLeaves)
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportOneLeafSkipped(ReportNode reportNode, String concatenatedId, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafSkipped", "Skipping ${concatenatedId} optimization because earlier combination fulfills stop criterion.")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Skipping {} optimization because earlier combination fulfills stop criterion.", concatenatedId);
        return addedNode;
    }

    public static ReportNode reportOneLeafCannotOptimize(ReportNode reportNode, String concatenatedId, String message) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafCannotOptimize", "Cannot optimize remedial action combination ${concatenatedId}: ${message}")
            .withUntypedValue("concatenatedId", concatenatedId)
            .withUntypedValue("message", message)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Cannot optimize remedial action combination {}: {}", concatenatedId, message);
        return addedNode;
    }

    public static ReportNode reportOneLeafRemainingLeaves(ReportNode reportNode, int remainingLeaves) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafRemainingLeaves", "Remaining leaves to evaluate: ${remainingLeaves}")
            .withUntypedValue("remainingLeaves", remainingLeaves)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Remaining leaves to evaluate: {}", remainingLeaves);
        return addedNode;
    }

    public static ReportNode reportOneLeafCouldNotEvaluateCombination(ReportNode reportNode, String networkActions, String message, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafCouldNotEvaluateCombination", "Could not evaluate network action combination \"${networkActions}\": ${message}")
            .withUntypedValue("networkActions", networkActions)
            .withUntypedValue("message", message)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Could not evaluate network action combination \"{}\": {}", networkActions, message);
        return addedNode;
    }

    public static ReportNode reportOneLeafEvaluated(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafEvaluated", "Evaluated ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Evaluated {}", leaf);
        return addedNode;
    }

    public static ReportNode reportOneLeafOptimized(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafOptimized", "Optimized ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Optimized {}", leaf);
        return addedNode;
    }

    public static ReportNode reportOneLeafFailedToOptimize(ReportNode reportNode, String leaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafFailedToOptimize", "Failed to optimize leaf: ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        logger(severity).info("Failed to optimize leaf: {}", leaf);
        return addedNode;
    }

    public static ReportNode reportOneLeafNoRangeActionToOptimize(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafNoRangeActionToOptimize", "No range actions to optimize")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("No range actions to optimize");
        return addedNode;
    }

    public static ReportNode reportOneLeafStopCriterionReached(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafStopCriterionReached", "Stop criterion reached, other threads may skip optimization.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Stop criterion reached, other threads may skip optimization.");
        return addedNode;
    }

    public static ReportNode reportOneLeafPerimeterPurelyVirtual(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeOneLeafPerimeterPurelyVirtual", "Perimeter is purely virtual and virtual cost is zero. Exiting search tree.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Perimeter is purely virtual and virtual cost is zero. Exiting search tree.");
        return addedNode;
    }

    public static ReportNode reportVirtualCostDetail(ReportNode reportNode, String prefix, String identifier, String virtualCostName, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("virtualCostDetail", "${prefix}${identifier}, stop criterion could have been reached without \"${virtualCostName}\" virtual cost")
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
            .withMessageTemplate("virtualCostlyElementsLog", "${prefix}${identifier}, limiting \"${virtualCostName}\" constraint ${index}: flow = ${flow} ${unit}, threshold = ${limitingThreshold} ${unit}, margin = ${margin} ${unit}, element ${networkElementId} at state ${stateId}, CNEC ID = \"${cnecId}\", CNEC name = \"${cnecName}\"")
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

    public static ReportNode reportSearchTreeParameter(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeParameter", "New Search Tree Parameter")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportLeafAlreadyEvaluated(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportLeafAlreadyEvaluated", "Leaf has already been evaluated")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Leaf has already been evaluated");
        return addedNode;
    }

    public static ReportNode reportEvaluatingLeaf(ReportNode reportNode, String leaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportEvaluatingLeaf", "Evaluating ${leaf}")
            .withUntypedValue("leaf", leaf)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Evaluating {}", leaf);
        return addedNode;
    }

    public static ReportNode reportFailedEvaluateLeafSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportFailedEvaluateLeafSensitivityAnalysisFailed", "Failed to evaluate leaf: sensitivity analysis failed")
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Failed to evaluate leaf: sensitivity analysis failed");
        return addedNode;
    }

    public static ReportNode reportResettingRangeActionSetPointsToPreOptimValues(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportResettingRangeActionSetPointsToPreOptimValues", "Resetting range action setpoints to their pre-optim values")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Resetting range action setpoints to their pre-optim values");
        return addedNode;
    }

    public static ReportNode reportOptimizingLeaf(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportOptimizingLeaf", "Optimizing leaf...")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Optimizing leaf...");
        return addedNode;
    }

    public static ReportNode reportEvaluationFailed(ReportNode reportNode, String leaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportEvaluationFailed", "Impossible to optimize leaf: ${leaf}\n because evaluation failed")
            .withUntypedValue("leaf", leaf)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation failed", leaf);
        return addedNode;
    }

    public static ReportNode reportEvaluationNotPerformed(ReportNode reportNode, String leaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportEvaluationNotPerformed", "Impossible to optimize leaf: ${leaf}\n because evaluation has not been performed")
            .withUntypedValue("leaf", leaf)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Impossible to optimize leaf: {}\n because evaluation has not been performed", leaf);
        return addedNode;
    }

    public static ReportNode reportNetworkActionsTooFarFromMostLimitingElement(ReportNode reportNode, int numberOfEliminatedNetworkActions) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNetworkActionsTooFarFromMostLimitingElement", "${numberOfEliminatedNetworkActions} network action combinations have been filtered out because they are too far from the most limiting element")
            .withUntypedValue("numberOfEliminatedNetworkActions", numberOfEliminatedNetworkActions)
            .withSeverity(DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because they are too far from the most limiting element", numberOfEliminatedNetworkActions);
        return addedNode;
    }

    public static ReportNode reportTooManyNetworkActionsForTheirTso(ReportNode reportNode, int numberOfEliminatedNetworkActions) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportTooManyNetworkActionsForTheirTso", "${numberOfEliminatedNetworkActions} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached")
            .withUntypedValue("numberOfEliminatedNetworkActions", numberOfEliminatedNetworkActions)
            .withSeverity(DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the maximum number of network actions for their TSO has been reached", numberOfEliminatedNetworkActions);
        return addedNode;
    }

    public static ReportNode reportNetworkActionsFilteredTooManyUsableRemedialActions(ReportNode reportNode, int numberOfEliminatedNetworkActions) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNetworkActionsFilteredTooManyUsableRemedialActions", "${numberOfEliminatedNetworkActions} network action combinations have been filtered out because the max number of usable RAs has been reached")
            .withUntypedValue("numberOfEliminatedNetworkActions", numberOfEliminatedNetworkActions)
            .withSeverity(DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable RAs has been reached", numberOfEliminatedNetworkActions);
        return addedNode;
    }

    public static ReportNode reportNetworkActionsFilteredTooManyUsableTso(ReportNode reportNode, int numberOfEliminatedNetworkActions) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNetworkActionsFilteredTooManyUsableTso", "${numberOfEliminatedNetworkActions} network action combinations have been filtered out because the max number of usable TSOs has been reached")
            .withUntypedValue("numberOfEliminatedNetworkActions", numberOfEliminatedNetworkActions)
            .withSeverity(DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("{} network action combinations have been filtered out because the max number of usable TSOs has been reached", numberOfEliminatedNetworkActions);
        return addedNode;
    }

    public static ReportNode reportRunSearchTree(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRunSearchTree", "Run search tree")
            .withSeverity(INFO_SEVERITY)
            .add();
    }
}
