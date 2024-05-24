package com.powsybl.openrao.flowbasedcomputation.impl;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class FlowbasedComputationReports {
    private FlowbasedComputationReports() {
        // utility class
    }

    public static ReportNode reportFlowbasedComputation(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportFlowbasedComputationReportsFlowbasedComputation", "Flow based computation")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportRemedialActionAppliedEvenIfConditionNotChecked(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportFlowbasedComputationReportsRemedialActionAppliedEvenIfConditionNotChecked", "Remedial action may be available only on constraint. Condition is not checked but remedial action is applied")
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Remedial action may be available only on constraint. Condition is not checked but remedial action is applied");
        return addedNode;
    }

    public static ReportNode reportNullRaoResult(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportFlowbasedComputationReportsNullRaoResult", "RAO result is null: applying all network actions from CRAC.")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("RAO result is null: applying all network actions from CRAC.");
        return addedNode;
    }

    public static ReportNode reportNotNullRaoResult(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportFlowbasedComputationReportsNullRaoResult", "RAO result is not null: applying remedial actions selected by the RAO.")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("RAO result is not null: applying remedial actions selected by the RAO.");
        return addedNode;
    }
}
