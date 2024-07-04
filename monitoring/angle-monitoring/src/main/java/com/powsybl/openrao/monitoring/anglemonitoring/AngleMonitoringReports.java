package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.data.cracapi.State;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class AngleMonitoringReports {
    private AngleMonitoringReports() {
    }

    private static String formatDouble(double doubleValue) {
        return String.format("%.0f", doubleValue);
    }

    public static ReportNode reportAngleMonitoringStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("angleMonitoringStart", "Angle monitoring")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Angle monitoring [start]");
        return addedNode;
    }

    public static ReportNode reportAngleMonitoringEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("angleMonitoringEnd", "End of angle monitoring")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("----- Angle monitoring [end]");
        return addedNode;
    }

    public static ReportNode reportNoAngleCnecsDefined(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noAngleCnecsDefined", "No AngleCnecs defined.")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("No AngleCnecs defined.");
        return addedNode;
    }

    public static ReportNode reportMonitoringAnglesAtState(ReportNode reportNode, State state) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("monitoringAnglesAtState", "Monitoring angles at state \"${state}\"")
                .withUntypedValue("state", state.getId())
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("-- Monitoring angles at state \"{}\" [start]", state);
        return addedNode;
    }

    public static ReportNode reportMonitoringAnglesAtStateEnd(ReportNode reportNode, State state) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("monitoringAnglesAtStateEnd", "End of monitoring angles at state \"${state}\"")
                .withUntypedValue("state", state.getId())
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("-- Monitoring angles at state \"{}\" [end]", state);
        return addedNode;
    }

    public static ReportNode reportNoConstrainedElements(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noConstrainedElements", "All AngleCnecs are secure.")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("All AngleCnecs are secure.");
        return addedNode;
    }

    public static ReportNode reportUnknownStatusOnAngleCnecs(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("unknownStatusAngleCnecs", "Unknown status on AngleCnecs.")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Unknown status on AngleCnecs."); // TODO test this
        return addedNode;
    }

    public static ReportNode reportSomeConstrainedElements(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("someConstrainedElements", "Some AngleCnecs are not secure:")
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info("Some AngleCnecs are not secure:");
        return addedNode;
    }

    public static ReportNode reportConstrainedElement(ReportNode reportNode, String angleCnecId, String importingNetworkElementId, String exportingNetworkElementId, String stateId, double angle) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("constrainedElement", "AngleCnec ${angleCnecId} (with importing network element ${importingNetworkElementId} and exporting network element ${exportingNetworkElementId})" +
                    " at state ${stateId} has an angle of ${angle}°.")
                .withUntypedValue("angleCnecId", angleCnecId)
                .withUntypedValue("importingNetworkElementId", importingNetworkElementId)
                .withUntypedValue("exportingNetworkElementId", exportingNetworkElementId)
                .withUntypedValue("stateId", stateId)
                .withUntypedValue("angle", formatDouble(angle))
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
        BUSINESS_LOGS.info(
                String.format("AngleCnec %s (with importing network element %s and exporting network element %s)" +
                        " at state %s has an angle of %.0f°.",
                    angleCnecId,
                    importingNetworkElementId,
                    exportingNetworkElementId,
                    stateId,
                    angle
        ));
        return addedNode;
    }

    public static ReportNode reportLoadflowComputationFailed(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowComputationFailed", "Load-flow computation failed at state ${stateId} after applying RAs. Skipping this state.")
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", stateId); // TODO test this
        return addedNode;
    }

    public static ReportNode reportNoRaAvailable(ReportNode reportNode, String cnecId, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("noRaAvailable", "AngleCnec ${cnecId} in state ${stateId} has no associated RA. Angle constraint cannot be secured.")
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("AngleCnec {} in state {} has no associated RA. Angle constraint cannot be secured.", cnecId, stateId);
        return addedNode;
    }

    public static ReportNode reportConstraintInPreventive(ReportNode reportNode, String cnecId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("constraintInPreventive", "AngleCnec ${cnecId} is constrained in preventive state, it cannot be secured.")
                .withUntypedValue("cnecId", cnecId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("AngleCnec {} is constrained in preventive state, it cannot be secured.", cnecId);
        return addedNode;
    }

    public static ReportNode reportIgnoredRemedialActionForState(ReportNode reportNode, String remedialActionId, String cnecId, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("ignoredRemedialAction", "Remedial action ${remedialActionId} of AngleCnec ${cnecId} in state ${stateId} is ignored : it's not a network action.")
                .withUntypedValue("remedialActionId", remedialActionId)
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} in state {} is ignored : it's not a network action.", remedialActionId, cnecId, stateId); // TODO test this
        return addedNode;
    }

    public static ReportNode reportIgnoredRemedialAction(ReportNode reportNode, String remedialActionId, String cnecId, String cause) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("ignoredRemedialAction", "Remedial action ${remedialActionId} of AngleCnec ${cnecId} is ignored : ${cause}.")
            .withUntypedValue("remedialActionId", remedialActionId)
            .withUntypedValue("cnecId", cnecId)
            .withUntypedValue("cause", cause)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : {}.", remedialActionId, cnecId, cause);
        return addedNode;
    }

    public static ReportNode reportAppliedNetworkActions(ReportNode reportNode, String cnecId, String appliedRasList) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("appliedNetworkActions", "Applying the following remedial action(s) in order to reduce constraints on CNEC \"${cnecId}\": ${appliedRasList}")
                .withUntypedValue("cnecId", cnecId)
                .withUntypedValue("appliedRasList", appliedRasList.isEmpty() ? "[]" : appliedRasList)
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

    public static ReportNode reportLoadflowDivergence(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("loadflowDivergence", "Load flow divergence.")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Load flow divergence.");
        return addedNode;
    }

    public static ReportNode reportAngleMonitoringFailureAtState(ReportNode reportNode, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("angleMonitoringFailureAtState", "Load-flow computation failed at state ${stateId}. Skipping this state.")
                .withUntypedValue("stateId", stateId)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", stateId);
        return addedNode;
    }

    static ReportNode reportPostContingencyTask(State state, ReportNode rootReportNode) {
        ReportNode scenarioReportNode = rootReportNode.newReportNode()
                .withMessageTemplate("postContingencyScenarioAngleMonitoring", "Angle monitoring post-contingency ${contingencyId}.")
                .withUntypedValue("contingencyId", state.getContingency().orElseThrow().getId())
                .withSeverity(TypedValue.DEBUG_SEVERITY)
                .add();
        return scenarioReportNode;
    }

    static ReportNode generatePostContingencyRootReportNode() {
        ReportNode rootReportNode = ReportNode.newRootReportNode()
                .withMessageTemplate("postContingencyAngleMonitoring", "Angle monitoring post-contingency.")
                .build();
        return rootReportNode;
    }

    public static ReportNode reportRedispatchingStart(ReportNode reportNode, Double redispatchingValue, String countryId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("redispatchingAngleMonitoring", "Redispatching ${redispatchingValue} MW in ${countryId}")
            .withTypedValue("redispatchingValue", redispatchingValue, TypedValue.ACTIVE_POWER)
            .withUntypedValue("countryId", countryId)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", redispatchingValue, countryId);
        return addedNode;
    }

    public static ReportNode reportRedispatchingEnd(ReportNode reportNode, Double redispatchingValue, String countryId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRedispatchingEnd", "End of redispatching ${redispatchingValue} MW in ${countryId}")
            .withTypedValue("redispatchingValue", redispatchingValue, TypedValue.ACTIVE_POWER)
            .withUntypedValue("countryId", countryId)
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", redispatchingValue, countryId);
        return addedNode;
    }

    public static ReportNode reportRedispatchingFailed(ReportNode reportNode, double powerToRedispatch, double redispatchedPower) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRedispatchingFailed", "Redispatching failed: asked=${powerToRedispatch} MW, applied=${redispatchedPower} MW")
            .withTypedValue("powerToRedispatch", powerToRedispatch, TypedValue.ACTIVE_POWER)
            .withTypedValue("redispatchedPower", redispatchedPower, TypedValue.ACTIVE_POWER)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Redispatching failed: asked={} MW, applied={} MW", powerToRedispatch, redispatchedPower);
        return addedNode;
    }
}
