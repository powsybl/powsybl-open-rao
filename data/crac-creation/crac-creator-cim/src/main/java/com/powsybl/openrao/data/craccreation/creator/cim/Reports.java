package com.powsybl.openrao.data.craccreation.creator.cim;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class Reports {
    private Reports() {
    }

    public static ReportNode reportValidCimCrac(ReportNode reportNode, String filename) {
        ReportNode addedNode = reportNode.newReportNode()
                        .withMessageTemplate("validCimCrac", "'${filename}' is a valid CIM CRAC document")
                        .withUntypedValue("filename", filename)
                        .withSeverity(TypedValue.INFO_SEVERITY)
                        .add();
        BUSINESS_LOGS.info("CIM CRAC document is valid");
        return addedNode;
    }

    public static ReportNode reportInvalidCimCrac(ReportNode reportNode, String filename, String reason) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("invalidCimCrac", "'${filename}' is NOT a valid CIM CRAC document. Reason: ${reason}")
                .withUntypedValue("filename", filename)
                .withUntypedValue("reason", reason)
                .withSeverity(TypedValue.TRACE_SEVERITY)
                .add();
        TECHNICAL_LOGS.debug("CIM CRAC document is NOT valid. Reason: {}", reason);
        return addedNode;
    }
}
