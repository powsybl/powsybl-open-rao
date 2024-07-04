package com.powsybl.openrao.data.swecneexporter;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.util.Optional;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class SweCneExporterReports {
    private SweCneExporterReports() {
        // utility class
    }

    public static ReportNode reportSweConstraintSeriesCreator(ReportNode reportNode) {
        return reportNode.newReportNode() // TODO test this
            .withMessageTemplate("reportSweConstraintSeriesCreator", "SWE constraint series creator")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSweAdditionalConstraintSeriesCreator(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportSweAdditionalConstraintSeriesCreator", "SWE additional constraint series creator")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSweAdditionalConstraintSeriesCreatorAngleCnecIgnored(ReportNode reportNode, String instantId, String nativeId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSweAdditionalConstraintSeriesCreatorAngleCnecIgnored", "${instantId} angle cnec \"${nativeId}\" will not be added to CNE file")
            .withUntypedValue("instantId", Optional.ofNullable(instantId).orElse(""))
            .withUntypedValue("nativeId", Optional.ofNullable(nativeId).orElse(""))
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("{} angle cnec {} will not be added to CNE file", instantId, nativeId);
        return addedNode;
    }

    public static ReportNode reportSweAdditionalConstraintSeriesCreatorPreventiveAngleCnecIgnored(ReportNode reportNode, String nativeId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSweAdditionalConstraintSeriesCreatorPreventiveAngleCnecIgnored", "Preventive angle cnec \"${nativeId}\" will not be added to CNE file")
            .withUntypedValue("nativeId", Optional.ofNullable(nativeId).orElse(""))
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Preventive angle cnec {} will not be added to CNE file", nativeId);
        return addedNode;
    }
}
