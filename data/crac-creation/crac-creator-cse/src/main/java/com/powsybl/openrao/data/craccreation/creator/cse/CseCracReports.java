package com.powsybl.openrao.data.craccreation.creator.cse;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

public final class CseCracReports {
    private CseCracReports() {
        // utility class
    }

    public static ReportNode reportCseValidCracDocument(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("cseValidCracDocument", "CSE CRAC document is valid")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("CSE CRAC document is valid");
        return addedNode;
    }

    public static ReportNode reportCseCracCreator(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("cseCracCreator", "CSE CRAC creator")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }
}
