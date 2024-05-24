package com.powsybl.openrao.data.glsk.virtual.hubs;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class GlskVirtualHubsReports {
    private GlskVirtualHubsReports() {
        // utility class
    }

    public static ReportNode reportGlskVirtualHubsBuild(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportGlskVirtualHubs", "Build GLSKs of virtual hubs")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportGlskVirtualHubsNoLoadFound(ReportNode reportNode, String eiCode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportGlskVirtualHubsNoLoadFound", "No load found for virtual hub \"${eiCode}\"")
            .withUntypedValue("eiCode", eiCode)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("No load found for virtual hub {}", eiCode);
        return addedNode;
    }

    public static ReportNode reportGlskVirtualHubsLoadFound(ReportNode reportNode, String injectionId, String eiCode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportGlskVirtualHubsLoadFound", "Load \"${injectionId}\" found for virtual hub \"${eiCode}\"")
            .withUntypedValue("injectionId", injectionId)
            .withUntypedValue("eiCode", eiCode)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Load {} found for virtual hub {}", injectionId, eiCode);
        return addedNode;
    }

    public static ReportNode reportGlskVirtualHubsAssigmentErrorNoLoad(ReportNode reportNode, String eiCode, String nodeName) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportGlskVirtualHubsAssigmentErrorNoLoad", "Virtual hub \"${eiCode}\" cannot be assigned on node \"${nodeName}\" as it has no load in the network")
            .withUntypedValue("eiCode", eiCode)
            .withUntypedValue("nodeName", nodeName)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Virtual hub {} cannot be assigned on node {} as it has no load in the network", eiCode, nodeName);
        return addedNode;
    }

    public static ReportNode reportGlskVirtualHubsAssigmentErrorNoNode(ReportNode reportNode, String eiCode, String nodeName) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportGlskVirtualHubsAssigmentErrorNoNode", "Virtual hub \"${eiCode}\" cannot be assigned on node \"${nodeName}\" as it was not found in the network")
            .withUntypedValue("eiCode", eiCode)
            .withUntypedValue("nodeName", nodeName)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Virtual hub {} cannot be assigned on node {} as it was not found in the network", eiCode, nodeName);
        return addedNode;
    }
}
