package com.powsybl.openrao.monitoring.voltagemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.data.cracapi.State;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class VoltageMonitoringReports {
    private VoltageMonitoringReports() {
    }

    private static String formatDouble(double doubleValue) {
        return String.format("%.0f", doubleValue);
    }

    public static ReportNode reportVoltageMonitoringStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("voltageMonitoringStart", "Voltage monitoring")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Voltage monitoring [start]");
        return addedNode;
    }

    public static ReportNode reportVoltageMonitoringEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("voltageMonitoringEnd", "End of voltage monitoring")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Voltage monitoring [end]");
        return addedNode;
    }

    public static ReportNode reportNoVoltageCnecsDefined(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noVoltageCnecsDefined", "No VoltageCnecs defined.")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("No VoltageCnecs defined.");
        return addedNode;
    }

    public static ReportNode reportMonitoringVoltagesAtState(ReportNode reportNode, State state) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("monitoringVoltagesAtState", "Monitoring voltages at state \"${state}\"")
                .withUntypedValue("state", state.getId())
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("-- Monitoring voltages at state \"{}\" [start]", state);
        return addedNode;
    }

    public static ReportNode reportMonitoringVoltagesAtStateEnd(ReportNode reportNode, State state) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("monitoringVoltagesAtStateEnd", "End of monitoring voltages at state \"${state}\"")
                .withUntypedValue("state", state.getId())
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("-- Monitoring voltages at state \"{}\" [end]", state);
        return addedNode;
    }

    public static ReportNode reportNoConstrainedElements(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noConstrainedElements", "All voltage CNECs are secure.")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("All voltage CNECs are secure.");
        return addedNode;
    }

    public static ReportNode reportSomeConstrainedElements(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("someConstrainedElements", "Some voltage CNECs are not secure:")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Some voltage CNECs are not secure:");
        return addedNode;
    }

    public static ReportNode reportConstrainedElement(ReportNode reportNode, String networkElementId, String stateId, double min, double max) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("constrainedElement", "Network element ${networkElementId} at state ${stateId} has a voltage of ${min} - ${max} kV.")
                .withUntypedValue("networkElementId", networkElementId)
                .withUntypedValue("stateId", stateId)
                .withUntypedValue("min", formatDouble(min))
                .withUntypedValue("max", formatDouble(max))
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

    public static ReportNode reportLoadflowComputationFailed(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowComputationFailed", "Load-flow computation failed at state ${stateId} after applying RAs. Skipping this state.")
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", stateId);
        return addedNode;
    }

    public static ReportNode reportNoRaAvailable(ReportNode reportNode, String cnecId, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noRaAvailable", "VoltageCnec ${cnecId} in state ${stateId} has no associated RA. Voltage constraint cannot be secured.")
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("VoltageCnec {} in state {} has no associated RA. Voltage constraint cannot be secured.", cnecId, stateId);
        return addedNode;
    }

    public static ReportNode reportConstraintInPreventive(ReportNode reportNode, String cnecId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("constraintInPreventive", "VoltageCnec ${cnecId} is constrained in preventive state, it cannot be secured.")
                .withUntypedValue("cnecId", cnecId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("VoltageCnec {} is constrained in preventive state, it cannot be secured.", cnecId);
        return addedNode;
    }

    public static ReportNode reportIgnoredRemedialAction(ReportNode reportNode, String remedialActionId, String cnecId, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("ignoredRemedialAction", "Remedial action ${remedialActionId} of VoltageCnec ${cnecId} in state ${stateId} is ignored : it's not a network action.")
                .withUntypedValue("remedialActionId", remedialActionId)
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Remedial action {} of VoltageCnec {} in state {} is ignored : it's not a network action.", remedialActionId, cnecId, stateId);
        return addedNode;
    }

    public static ReportNode reportAppliedNetworkActions(ReportNode reportNode, String cnecId, String appliedRasList) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("appliedNetworkActions", "Applying the following remedial action(s) in order to reduce constraints on CNEC \"${cnecId}\": ${appliedRasList}")
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("appliedRasList", appliedRasList)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Applying the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}", cnecId, appliedRasList);
        return addedNode;
    }

    public static ReportNode reportLoadflowComputationStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowComputationStart", "Load-flow computation")
                .withSeverity(TypedValue.DEBUG_SEVERITY)
                .add();
        TECHNICAL_LOGS.info("Load-flow computation [start]");
        return addedNode;
    }

    public static ReportNode reportLoadflowComputationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowComputationEnd", "End of load-flow computation")
                .withSeverity(TypedValue.DEBUG_SEVERITY)
                .add();
        TECHNICAL_LOGS.info("Load-flow computation [end]");
        return addedNode;
    }

    public static ReportNode reportLoadflowError(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowError", "LoadFlow error.")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("LoadFlow error.");
        return addedNode;
    }

    public static ReportNode reportVoltageMonitoringFailureAtState(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("voltageMonitoringFailureAtState", "Load-flow computation failed at state ${stateId}. Skipping this state.")
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", stateId);
        return addedNode;
    }

    static ReportNode reportPostContingencyTask(State state, ReportNode rootReportNode) {
        ReportNode scenarioReportNode = rootReportNode.newReportNode()
                .withMessageTemplate("postContingencyScenarioVoltageMonitoring", "Voltage monitoring post-contingency ${contingencyId}.")
                .withUntypedValue("contingencyId", state.getContingency().orElseThrow().getId())
                .withSeverity(TypedValue.DEBUG_SEVERITY)
                .add();
        return scenarioReportNode;
    }

    static ReportNode generatePostContingencyRootReportNode() {
        ReportNode rootReportNode = ReportNode.newRootReportNode()
                .withMessageTemplate("postContingencyVoltageMonitoring", "Voltage monitoring post-contingency.")
                .build();
        return rootReportNode;
    }
}
