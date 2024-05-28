package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class LoopFlowReports {
    private LoopFlowReports() {
        // Utility class
    }

    public static ReportNode reportNewLoopFlowComputation(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewLoopFlowComputation", "New loop flow computation")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportNoGlskFoundForReferenceArea(ReportNode reportNode, String areaCode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNoGlskFoundForReferenceArea", "No GLSK found for reference area ${areaCode}")
            .withUntypedValue("areaCode", areaCode)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("No GLSK found for reference area {}", areaCode);
        return addedNode;
    }
}
