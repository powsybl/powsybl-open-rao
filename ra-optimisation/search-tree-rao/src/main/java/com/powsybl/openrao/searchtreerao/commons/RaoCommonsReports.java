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

import java.util.Locale;

import static com.powsybl.commons.report.TypedValue.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class RaoCommonsReports {
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

    private RaoCommonsReports() {
        // Utility class
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
