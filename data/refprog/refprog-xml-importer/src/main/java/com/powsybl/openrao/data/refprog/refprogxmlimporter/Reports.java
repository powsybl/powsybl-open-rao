package com.powsybl.openrao.data.refprog.refprogxmlimporter;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.time.OffsetDateTime;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class Reports {
    private Reports() {
    }

    public static ReportNode reportRefprogInvalidForDate(ReportNode reportNode, OffsetDateTime dateTime) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("refprogInvalidForDate", "RefProg file is not valid for this date ${dateTime}")
                .withUntypedValue("dateTime", dateTime.toString())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("RefProg file is not valid for this date {}", dateTime);
        return addedNode;
    }

    public static ReportNode reportRefprogFileImported(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("refprogFileImported", "RefProg file was imported")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        TECHNICAL_LOGS.info("RefProg file was imported");
        return addedNode;
    }

    public static ReportNode reportRefprogImportFailedUnknownTimeInterval(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("refprogImportFailedUnknownTimeInterval", "Cannot import RefProg file because its publication time interval is unknown")
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
        BUSINESS_LOGS.error("Cannot import RefProg file because its publication time interval is unknown"); // TODO test this
        return addedNode;
    }

    public static ReportNode reportRefprogFlowNotFoundForDate(ReportNode reportNode, String outArea, String inArea, OffsetDateTime dateTime) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("refprogFlowNotFoundForDate", "Flow value between ${outArea} and ${inArea} is not found for this date ${date}")
                .withUntypedValue("outArea", outArea)
                .withUntypedValue("inArea", inArea)
                .withUntypedValue("dateTime", dateTime.toString())
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Flow value between {} and {} is not found for this date {}", outArea, inArea, dateTime); // TODO test this
        return addedNode;
    }
}
