package com.powsybl.openrao.data.craccreation.creator.csaprofile;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.time.OffsetDateTime;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class CsaProfileReports {
    private CsaProfileReports() {
        // utility class
    }

    public static ReportNode reportCsaProfileCracDateInconsistency(ReportNode reportNode, String contextName, OffsetDateTime offsetDateTime) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("csaProfileCracDateInconsistency", "[REMOVED] The file : ${contextName} will be ignored. Its dates are not consistent with the import date : ${offsetDateTime}")
            .withUntypedValue("contextName", contextName)
            .withUntypedValue("offsetDateTime", offsetDateTime.toString())
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("[REMOVED] The file : {} will be ignored. Its dates are not consistent with the import date : {}", contextName, offsetDateTime);
        return addedNode;
    }
}
