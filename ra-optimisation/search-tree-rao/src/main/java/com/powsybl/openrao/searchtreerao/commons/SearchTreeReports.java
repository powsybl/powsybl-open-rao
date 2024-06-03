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
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;

import java.time.Instant;
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

    public static ReportNode reportRao(String networkId, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("rao", "Running RAO on network '${networkId}'")
                .withUntypedValue("networkId", networkId)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Running RAO on network '{}'", networkId);
        return addedNode;
    }

    public static ReportNode reportRaoFailure(String state, Exception exception, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("raoFailure", "Optimizing state '${state}' failed with message: ${message}")
                .withUntypedValue("state", state)
                .withUntypedValue("message", exception.getMessage())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", state, exception);
        return addedNode;
    }

    public static ReportNode reportInitialSensitivity(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("initialSensitivity", "Initial sensitivity analysis")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Initial sensitivity analysis");
        return addedNode;
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

    public static ReportNode reportInitialSensitivityFailure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("initialSensitivityFailure", "Initial sensitivity analysis failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
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

    public static ReportNode reportPreventivePerimeter(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeter", "Preventive perimeter optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreventivePerimeterEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeterEnd", "End of preventive perimeter optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportPreCurativeSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preCurativeSensitivityFailure", "Systematic sensitivity analysis after preventive remedial actions failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
        return addedNode;
    }

    public static ReportNode reportDoNotOptimizeCurativeBecausePreventiveUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("doNotOptimizeCurativeBecausePreventiveUnsecure", "Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimeters(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postContingencyPerimeters", "Post contingency perimeters optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimetersEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postContingencyPerimetersEnd", "End of post contingency perimeters optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("resultsMergingPreventiveAndPostContingency", "Merging preventive and post-contingency RAO results")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveFailed", "Second preventive failed. Falling back to previous solution")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFixedSituation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveFixedSituation", "RAO has succeeded thanks to second preventive step when first preventive step had failed")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveIncreasedOverallCost(ReportNode reportNode, double firstPreventiveCost, double firstPreventiveFunctionalCost, double firstPreventiveVirtualCost, double secondPreventiveCost, double secondPreventiveFunctionalCost, double secondPreventiveVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveIncreasedOverallCost", "Second preventive step has increased the overall cost from ${firstPreventiveCost} (functional: ${firstPreventiveFunctionalCost}, virtual: ${firstPreventiveVirtualCost}) to ${secondPreventiveCost} (functional: ${secondPreventiveFunctionalCost}, virtual: ${secondPreventiveVirtualCost}). Falling back to previous solution:")
                .withUntypedValue("firstPreventiveCost", firstPreventiveCost)
                .withUntypedValue("firstPreventiveFunctionalCost", firstPreventiveFunctionalCost)
                .withUntypedValue("firstPreventiveVirtualCost", firstPreventiveVirtualCost)
                .withUntypedValue("secondPreventiveCost", secondPreventiveCost)
                .withUntypedValue("secondPreventiveFunctionalCost", secondPreventiveFunctionalCost)
                .withUntypedValue("secondPreventiveVirtualCost", secondPreventiveVirtualCost)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
                formatDouble(firstPreventiveCost), formatDouble(firstPreventiveFunctionalCost), formatDouble(firstPreventiveVirtualCost),
                formatDouble(secondPreventiveCost), formatDouble(secondPreventiveFunctionalCost), formatDouble(secondPreventiveVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoIncreasedOverallCost(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveIncreasedOverallCost", "RAO has increased the overall cost from ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}) to ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost}). Falling back to initial solution:")
                .withUntypedValue("initialCost", initialCost)
                .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
                .withUntypedValue("initialVirtualCost", initialVirtualCost)
                .withUntypedValue("finalCost", finalCost)
                .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
                .withUntypedValue("finalVirtualCost", finalVirtualCost)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
                formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoCostEvolution(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("raoCostEvolution", "Cost before RAO = ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}), cost after RAO = ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost})")
                .withUntypedValue("initialCost", initialCost)
                .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
                .withUntypedValue("initialVirtualCost", initialVirtualCost)
                .withUntypedValue("finalCost", finalCost)
                .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
                .withUntypedValue("finalVirtualCost", finalVirtualCost)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
                formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
                formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondAutomatonSimulation", "Second automaton simulation")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondAutomatonSimulationEnd", "End of second automaton simulation")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveFirstAndSecondAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("resultsMergingPreventiveFirstAndSecondAndPostContingency", "Merging first, second preventive and post-contingency RAO results")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportPostRaoSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postRaoSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimization(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveOptimization", "Second preventive perimeter optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("preSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportPostSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("postSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimizationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("secondPreventiveOptimizationEnd", "End of second preventive perimeter optimization")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportRangeActionRemovedFromSecondCurative(ReportNode reportNode, String rangeActionId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("rangeActionRemovedFromSecondCurative", "Range action ${rangeActionId} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)")
                .withUntypedValue("rangeActionId", rangeActionId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)", rangeActionId);
        return addedNode;
    }

    public static ReportNode reportNotEnoughTimeForSecondPreventive(ReportNode reportNode, Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("notEnoughTimeForSecondPreventive", "There is not enough time to run a 2nd preventive RAO (target end time: ${targetEndInstant}, estimated time needed based on first preventive RAO: ${estimatedPreventiveRaoTimeInSeconds} seconds)")
                .withUntypedValue("targetEndInstant", targetEndInstant.toString())
                .withUntypedValue("estimatedPreventiveRaoTimeInSeconds", estimatedPreventiveRaoTimeInSeconds)
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao", "Cost has not increased during RAO, there is no need to run a 2nd preventive RAO")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure", "First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time")
                .withSeverity(INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
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

    public static ReportNode reportLeafEvaluationError(ReportNode reportNode, Leaf rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("leafEvaluationError", "Could not evaluate leaf: ${leafDescription}")
                .withUntypedValue("leafDescription", rootLeaf.toString())
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

    public static ReportNode reportAutomatonSimulationFailedRangeActionSensitivityComputation(ReportNode reportNode, String stateId, String instantQualifier) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSimulationFailedRangeActionSensitivityComputation", "Automaton state ${stateId} has failed during sensitivity computation ${instantQualifier} automaton simulation.")
            .withUntypedValue("stateId", stateId)
            .withUntypedValue("instantQualifier", instantQualifier)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {} automaton simulation.", stateId, instantQualifier);
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

    public static ReportNode reportHvdcRangeActionNotActivatedOutsideRange(ReportNode reportNode, String hvdcRaId, double activePowerSetpoint, double minAdmissibleSetpoint, double maxAdmissibleSetpoint) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportHvdcRangeActionNotActivatedOutsideRange", "HVDC range action ${hvdcRaId} could not be activated because its initial set-point (${activePowerSetpoint}) does not fall within its allowed range (${minAdmissibleSetpoint} - ${maxAdmissibleSetpoint})")
            .withUntypedValue("hvdcRaId", hvdcRaId)
            .withUntypedValue("activePowerSetpoint", activePowerSetpoint)
            .withUntypedValue("minAdmissibleSetpoint", minAdmissibleSetpoint)
            .withUntypedValue("maxAdmissibleSetpoint", maxAdmissibleSetpoint)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info(String.format("HVDC range action %s could not be activated because its initial set-point (%.1f) does not fall within its allowed range (%.1f - %.1f)",
            hvdcRaId, activePowerSetpoint, minAdmissibleSetpoint, maxAdmissibleSetpoint));
        return addedNode;
    }

    public static ReportNode reportNewAutomatonSimulator(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewAutomatonSimulator", "New  automaton simulator")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportOptimizingAutomatingState(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportOptimizingAutomatingState", "Optimizing automaton state ${automatonStateId}.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonSimulatorInitialSituation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSimulatorInitialSituation", "Initial situation:")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Initial situation:");
        return addedNode;
    }

    public static ReportNode reportAutomatonStateOptimized(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonStateOptimized", "Automaton state ${automatonStateId} has been optimized.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonStateId);
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

    public static ReportNode reportSearchTreeStopCriterionReached(ReportNode reportNode, Leaf rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeStopCriterionReached", "Stop criterion reached on ${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf.toString())
            .withSeverity(INFO_SEVERITY)
            .add();
        logger(severity).info("Stop criterion reached on {}", rootLeaf);
        return addedNode;
    }

    public static ReportNode reportSearchTreeLeaf(ReportNode reportNode, Leaf rootLeaf, TypedValue severity) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeaf", "${rootLeaf}")
            .withUntypedValue("rootLeaf", rootLeaf.toString())
            .withSeverity(TRACE_SEVERITY)
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

    public static ReportNode reportSearchTreeBestLeaf(ReportNode reportNode, Leaf optimalLeaf) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSearchTreeLeaf", "${rootLeaf}")
            .withUntypedValue("rootLeaf", optimalLeaf.toString())
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Best leaf: {}", optimalLeaf);
        return addedNode;
    }
}
