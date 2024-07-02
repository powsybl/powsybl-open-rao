package com.powsybl.openrao.data.cracapi;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class CracApiReports {
    private CracApiReports() {
        // utility class
    }

    public static ReportNode reportRaUsageLimitsNegativeMaxRa(ReportNode reportNode, int maxRa) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raUsageLimitsNegativeMaxRa", "The value ${maxRa} provided for max number of RAs is smaller than 0. It will be set to 0 instead.")
            .withUntypedValue("maxRa", maxRa)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} provided for max number of RAs is smaller than 0. It will be set to 0 instead.", maxRa);
        return addedNode;
    }

    public static ReportNode reportRaUsageLimitsNegativeMaxTso(ReportNode reportNode, int maxTso) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raUsageLimitsNegativeMaxTso", "The value ${maxTso} provided for max number of TSOs is smaller than 0. It will be set to 0 instead.")
            .withUntypedValue("maxTso", maxTso)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} provided for max number of TSOs is smaller than 0. It will be set to 0 instead.", maxTso);
        return addedNode;
    }

    public static ReportNode reportRaUsageLimitsNegativeMaxRaForTso(ReportNode reportNode, Integer limit, String tso) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raUsageLimitsNegativeMaxTsoForTso", "The value ${limit} provided for max number of RAs for TSO ${tso} is smaller than 0. It will be set to 0 instead.")
            .withUntypedValue("limit", limit)
            .withUntypedValue("tso", tso)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} provided for max number of RAs for TSO {} is smaller than 0. It will be set to 0 instead.", limit, tso);
        return addedNode;
    }
}
