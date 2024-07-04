package com.powsybl.openrao.monitoring.voltagemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonReports;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class VoltageMonitoringReports {

    private static final String VOLTAGE_CAMEL_CASE = "Voltage";
    private static final String VOLTAGE_LOWER_CASE = "voltage";

    private VoltageMonitoringReports() {
    }

    public static ReportNode reportVoltageMonitoringStart(ReportNode reportNode) {
        return MonitoringCommonReports.reportMonitoringStart(reportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportVoltageMonitoringEnd(ReportNode reportNode) {
        return MonitoringCommonReports.reportMonitoringEnd(reportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportNoVoltageCnecsDefined(ReportNode reportNode) {
        return  MonitoringCommonReports.reportNoCnecsDefined(reportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportMonitoringVoltagesAtState(ReportNode reportNode, State state) {
        return MonitoringCommonReports.reportMonitoringAtState(reportNode, state, VOLTAGE_LOWER_CASE); // TODO test this
    }

    public static ReportNode reportMonitoringVoltagesAtStateEnd(ReportNode reportNode, State state) {
        return MonitoringCommonReports.reportMonitoringAtStateEnd(reportNode, state, VOLTAGE_LOWER_CASE); // TODO test this
    }

    public static ReportNode reportNoConstrainedElements(ReportNode reportNode) {
        return MonitoringCommonReports.reportNoConstrainedElements(reportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportSomeConstrainedElements(ReportNode reportNode) {
        return MonitoringCommonReports.reportSomeConstrainedElements(reportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportConstrainedElement(ReportNode reportNode, String networkElementId, String stateId, double min, double max) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("voltageMonitoringConstrainedElement", "Network element ${networkElementId} at state ${stateId} has a voltage of ${min} - ${max} kV.")
                .withUntypedValue("networkElementId", networkElementId)
                .withUntypedValue("stateId", stateId)
                .withUntypedValue("min", MonitoringCommonReports.formatDouble(min))
                .withUntypedValue("max", MonitoringCommonReports.formatDouble(max))
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info(
                String.format("Network element %s at state %s has a voltage of %.0f - %.0f kV.",
                networkElementId,
                stateId,
                min,
                max
        ));
        return addedNode;
    }

    public static ReportNode reportNoRaAvailable(ReportNode reportNode, String cnecId, String stateId) {
        return MonitoringCommonReports.reportNoRaAvailable(reportNode, cnecId, stateId, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportConstraintInPreventive(ReportNode reportNode, String cnecId) {
        return MonitoringCommonReports.reportConstraintInPreventive(reportNode, cnecId, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportIgnoredRemedialAction(ReportNode reportNode, String remedialActionId, String cnecId, String stateId) {
        return MonitoringCommonReports.reportIgnoredRemedialActionForState(reportNode, remedialActionId, cnecId, stateId, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    static ReportNode reportPostContingencyTask(State state, ReportNode rootReportNode) {
        return MonitoringCommonReports.reportPostContingencyTask(state, rootReportNode, VOLTAGE_CAMEL_CASE); // TODO test this
    }

    static ReportNode generatePostContingencyRootReportNode() {
        return MonitoringCommonReports.generatePostContingencyRootReportNode(VOLTAGE_CAMEL_CASE);
    }
}
