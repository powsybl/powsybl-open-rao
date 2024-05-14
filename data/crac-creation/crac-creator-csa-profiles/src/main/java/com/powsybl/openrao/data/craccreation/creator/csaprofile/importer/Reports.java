package com.powsybl.openrao.data.craccreation.creator.csaprofile.importer;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;

public final class Reports {
    private Reports() {
        // utility class
    }

    public static ReportNode reportCsaProfileCracImportFile(ReportNode reportNode, String filename) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raoFailure", "csa profile crac import : import of file ${filename}")
            .withUntypedValue("filename", filename)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("csa profile crac import : import of file {}", filename);
        return addedNode;
    }
}
