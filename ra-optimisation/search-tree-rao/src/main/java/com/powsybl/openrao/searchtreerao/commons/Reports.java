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

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class Reports {
    private Reports() {
    }

    public static ReportNode reportRao(String networkId, ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("rao", "RAO on network '${networkId}'")
                .withUntypedValue("networkId", networkId)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportRaoFailure(String id, String message, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("raoFailure", "Optimizing state '${state}' failed with message: ${message}")
                .withUntypedValue("state", id)
                .withUntypedValue("message", message)
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
    }

    public static ReportNode reportInitialSensitivity(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("initialSensitivity", "Initial sensitivity analysis")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static ReportNode reportSensitivityAnalysisResults(ReportNode reportNode, double cost, double functionalCost, double virtualCost) {
        return reportNode.newReportNode()
                .withMessageTemplate("sensitivityAnalysisResults", "Sensitivity analysis: cost = ${cost} (functional: ${functionalCost}, virtual: ${virtualCost})")
                .withUntypedValue("cost", cost)
                .withUntypedValue("functionalCost", functionalCost)
                .withUntypedValue("virtualCost", virtualCost)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();

    }

    public static ReportNode reportInitialSensitivityFailure(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("initialSensitivityFailure", "Initial sensitivity analysis failed")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static ReportNode reportMostLimitingElement(ReportNode reportNode, int index, String isRelativeMargin, double cnecMargin, Unit unit, String ptdfIfRelative, String cnecNetworkElementName, String cnecStateId, String cnecId) {
        return reportNode.newReportNode()
                .withMessageTemplate("mostLimitingElement", "Limiting element ${index}:${isRelativeMargin} margin = ${cnecMargin} ${unit}${ptdfIfRelative}, element ${cnecNetworkElementName} at state ${cnecStateId}, CNEC ID = '${cnecId}'")
                .withUntypedValue("index", index)
                .withUntypedValue("isRelativeMargin", isRelativeMargin)
                .withUntypedValue("cnecMargin", cnecMargin)
                .withUntypedValue("unit", unit.toString())
                .withUntypedValue("ptdfIfRelative", ptdfIfRelative)
                .withUntypedValue("cnecNetworkElementName", cnecNetworkElementName)
                .withUntypedValue("cnecStateId", cnecStateId)
                .withUntypedValue("cnecId", cnecId)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static ReportNode reportPreventivePerimeter(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeter", "Preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static ReportNode reportPreventivePerimeterEnd(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("preventivePerimeterEnd", "End of preventive perimeter optimization")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }
}
