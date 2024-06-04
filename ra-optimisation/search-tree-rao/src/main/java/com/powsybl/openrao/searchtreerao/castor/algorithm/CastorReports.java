package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

public final class CastorReports {
    private CastorReports() {
        // Utility class
    }

    public static ReportNode reportRunCastorFullOptimization(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRunCastorFullOptimization", "Run castor full optimization")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportRunCastorOneStateOnly(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRunCastorOneStateOnly", "Run castor one state only")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityAnalysisFailed", "Initial sensitivity analysis failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
        return addedNode;
    }

    public static ReportNode reportCastorError(ReportNode reportNode, String message) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportCastorError", "${message}")
            .withUntypedValue("message", message)
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error(message);
        return addedNode;
    }
}
