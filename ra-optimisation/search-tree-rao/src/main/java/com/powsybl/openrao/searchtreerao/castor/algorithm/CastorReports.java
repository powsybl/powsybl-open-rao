package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

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
}
