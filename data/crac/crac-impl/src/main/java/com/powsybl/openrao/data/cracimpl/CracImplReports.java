package com.powsybl.openrao.data.cracimpl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class CracImplReports {
    private CracImplReports() {
        // utility class
    }

    public static ReportNode reportNewCrac(ReportNode reportNode, String id, String name) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewCrac", "New crac \"${id}\" called \"${name}\"")
            .withUntypedValue("id", id)
            .withUntypedValue("name", name)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewRaUsageLimitsAtInstant(ReportNode reportNode, String instantName) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewRaUsageLimits", "New RA usage limit at instant \"${instantName}\".")
            .withUntypedValue("instantName", instantName)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewInjectionRangeAction(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewInjectionRangeAction", "New injection range action")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewCounterTradeRangeAction(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewCounterTradeRangeAction", "New counter trade range action")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewHvdcRangeAction(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewHvdcRangeAction", "New HVDC range action")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewNetworkAction(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewNetworkAction", "New network action")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNewPstRangeAction(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewPstRangeAction", "New pst range action")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportActionWithoutAnyUsageRule(ReportNode reportNode, String type, String id) {
        ReportNode added = reportNode.newReportNode()
            .withMessageTemplate("reportActionWithoutAnyUsageRule", "${type} \"${id}\" does not contain any usage rule, by default it will never be available")
            .withUntypedValue("type", type)
            .withUntypedValue("id", id)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("{} {} does not contain any usage rule, by default it will never be available", type, id);
        return added;
    }

    public static ReportNode reportPstRangeActionInvalidRange(ReportNode reportNode, String id) {
        ReportNode added = reportNode.newReportNode()
            .withMessageTemplate("reportPstRangeActionInvalidRange", "PstRangeAction \"${id}\" does not contain any valid range, by default the range of the network will be used")
            .withUntypedValue("id", id)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("PstRangeAction {} does not contain any valid range, by default the range of the network will be used", id);
        return added;
    }

    public static ReportNode reportPstRangeActionPreventiveRaFiltered(ReportNode reportNode, String id) {
        ReportNode added = reportNode.newReportNode()
            .withMessageTemplate("reportPstRangeActionPreventiveRaFiltered", "RELATIVE_TO_PREVIOUS_INSTANT range has been filtered from PstRangeAction \"${id}\", as it is a preventive RA")
            .withUntypedValue("id", id)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("RELATIVE_TO_PREVIOUS_INSTANT range has been filtered from PstRangeAction {}, as it is a preventive RA", id);
        return added;
    }
}
