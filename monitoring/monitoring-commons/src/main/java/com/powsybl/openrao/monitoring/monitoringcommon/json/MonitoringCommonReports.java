package com.powsybl.openrao.monitoring.monitoringcommon.json;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.data.cracapi.State;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class MonitoringCommonReports {
    private MonitoringCommonReports() {
    }

    public static String formatDouble(double doubleValue) {
        return String.format("%.0f", doubleValue);
    }

    public static ReportNode reportMonitoringStart(ReportNode reportNode, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonMonitoringStart", "${type} monitoring")
            .withUntypedValue("type", type)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- {} monitoring [start]", type);
        return addedNode;
    }

    public static ReportNode reportMonitoringEnd(ReportNode reportNode, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonMonitoringEnd", "End of ${type} monitoring")
            .withUntypedValue("type", type.toLowerCase())
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- {} monitoring [end]", type);
        return addedNode;
    }

    public static ReportNode reportNoCnecsDefined(ReportNode reportNode, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonNoCnecsDefined", "No ${type} Cnecs defined.")
            .withUntypedValue("type", type)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("No {} Cnecs defined.", type);
        return addedNode;
    }

    public static ReportNode reportMonitoringAtState(ReportNode reportNode, State state, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonMonitoringAtState", "Monitoring ${type}s at state \"${state}\"")
            .withUntypedValue("type", type)
            .withUntypedValue("state", state.getId())
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("-- Monitoring {}s at state \"{}\" [start]", type, state);
        return addedNode;
    }

    public static ReportNode reportMonitoringAtStateEnd(ReportNode reportNode, State state, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonMonitoringAtStateEnd", "End of monitoring ${type}s at state \"${state}\"")
            .withUntypedValue("type", type)
            .withUntypedValue("state", state.getId())
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("-- Monitoring {}s at state \"{}\" [end]", type, state);
        return addedNode;
    }

    public static ReportNode reportNoConstrainedElements(ReportNode reportNode, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonNoConstrainedElements", "All ${type} Cnecs are secure.")
            .withUntypedValue("type", type)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("All {} Cnecs are secure.", type);
        return addedNode;
    }

    public static ReportNode reportSomeConstrainedElements(ReportNode reportNode, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonSomeConstrainedElements", "Some ${type} Cnecs are not secure:")
            .withUntypedValue("type", type)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Some {} Cnecs are not secure:", type);
        return addedNode;
    }

    public static ReportNode reportNoRaAvailable(ReportNode reportNode, String cnecId, String stateId, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonNoRaAvailable", "${type} Cnec ${cnecId} in state ${stateId} has no associated RA. ${type} constraint cannot be secured.")
            .withUntypedValue("type", type)
            .withUntypedValue("cnecId", cnecId)
            .withUntypedValue("stateId", stateId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("{} Cnec {} in state {} has no associated RA. {} constraint cannot be secured.", type, cnecId, stateId, type);
        return addedNode;
    }

    public static ReportNode reportConstraintInPreventive(ReportNode reportNode, String cnecId, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonConstraintInPreventive", "${type} Cnec ${cnecId} is constrained in preventive state, it cannot be secured.")
            .withUntypedValue("type", type)
            .withUntypedValue("cnecId", cnecId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("{} Cnec {} is constrained in preventive state, it cannot be secured.", type, cnecId);
        return addedNode;
    }

    public static ReportNode reportIgnoredRemedialActionForState(ReportNode reportNode, String remedialActionId, String cnecId, String stateId, String type) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonIgnoredRemedialAction", "Remedial action ${remedialActionId} of ${type} Cnec ${cnecId} in state ${stateId} is ignored : it's not a network action.")
            .withUntypedValue("remedialActionId", remedialActionId)
            .withUntypedValue("type", type)
            .withUntypedValue("cnecId", cnecId)
            .withUntypedValue("stateId", stateId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Remedial action {} of {} Cnec {} in state {} is ignored : it's not a network action.", remedialActionId, type, cnecId, stateId);
        return addedNode;
    }

    public static ReportNode reportPostContingencyTask(State state, ReportNode rootReportNode, String type) {
        return rootReportNode.newReportNode()
            .withMessageTemplate("monitoringCommonPostContingencyScenario", "${type} monitoring post-contingency ${contingencyId}.")
            .withUntypedValue("type", type)
            .withUntypedValue("contingencyId", state.getContingency().orElseThrow().getId())
            .withSeverity(TypedValue.DEBUG_SEVERITY)
            .add();
    }

    public static ReportNode generatePostContingencyRootReportNode(String type) {
        return ReportNode.newRootReportNode()
            .withMessageTemplate("monitoringCommonPostContingencyRoot", "${type} monitoring post-contingency.")
            .withUntypedValue("type", type)
            .build();
    }

    public static ReportNode reportAppliedNetworkActions(ReportNode reportNode, String cnecId, String appliedRasList) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonAppliedNetworkActions", "Applying the following remedial action(s) in order to reduce constraints on CNEC \"${cnecId}\": ${appliedRasList}")
            .withUntypedValue("cnecId", cnecId)
            .withUntypedValue("appliedRasList", appliedRasList.isEmpty() ? "[]" : appliedRasList)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Applying the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}", cnecId, appliedRasList);
        return addedNode;
    }

    public static ReportNode reportLoadFlowComputationStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonLoadFlowComputationStart", "Load-flow computation")
            .withSeverity(TypedValue.DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Load-flow computation [start]");
        return addedNode;
    }

    public static ReportNode reportLoadFlowComputationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonLoadFlowComputationEnd", "End of load-flow computation")
            .withSeverity(TypedValue.DEBUG_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Load-flow computation [end]");
        return addedNode;
    }

    public static ReportNode reportLoadFlowError(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonLoadFlowError", "LoadFlow error.")
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("LoadFlow error.");
        return addedNode;
    }

    public static ReportNode reportLoadFlowDivergence(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonLoadFlowDivergence", "Load flow divergence.")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Load flow divergence.");
        return addedNode;
    }

    public static ReportNode reportLoadflowComputationFailedAtStateAfterRA(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("monitoringCommonLoadflowComputationFailedAtStateAfterRA", "Load-flow computation failed at state ${stateId} after applying RAs. Skipping this state.")
            .withUntypedValue("stateId", stateId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", stateId); // TODO test this
        return addedNode;
    }

    public static ReportNode reportLoadFlowComputationFailureAtState(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("rmonitoringCommonLoadFlowComputationFailureAtState", "Load-flow computation failed at state ${stateId}. Skipping this state.")
            .withUntypedValue("stateId", stateId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", stateId);
        return addedNode;
    }
}
