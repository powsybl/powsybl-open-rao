package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonReports;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class AngleMonitoringReports {

    private static final String ANGLE_CAMEL_CASE = "Angle";
    private static final String ANGLE_LOWER_CASE = "angle";

    private AngleMonitoringReports() {
    }

    public static ReportNode reportAngleMonitoringStart(ReportNode reportNode) {
        return MonitoringCommonReports.reportMonitoringStart(reportNode, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportAngleMonitoringEnd(ReportNode reportNode) {
        return MonitoringCommonReports.reportMonitoringEnd(reportNode, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportNoAngleCnecsDefined(ReportNode reportNode) {
        return MonitoringCommonReports.reportNoCnecsDefined(reportNode, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportMonitoringAnglesAtState(ReportNode reportNode, State state) {
        return MonitoringCommonReports.reportMonitoringAtState(reportNode, state, ANGLE_LOWER_CASE); // TODO test this
    }

    public static ReportNode reportMonitoringAnglesAtStateEnd(ReportNode reportNode, State state) {
        return MonitoringCommonReports.reportMonitoringAtStateEnd(reportNode, state, ANGLE_LOWER_CASE); // TODO test this
    }

    public static ReportNode reportNoConstrainedElements(ReportNode reportNode) {
        return MonitoringCommonReports.reportNoConstrainedElements(reportNode, ANGLE_CAMEL_CASE); // TODO test this
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
        return MonitoringCommonReports.reportSomeConstrainedElements(reportNode, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportConstrainedElement(ReportNode reportNode, String angleCnecId, String importingNetworkElementId, String exportingNetworkElementId, String stateId, double angle) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("angleMonitoringConstrainedElement", "AngleCnec ${angleCnecId} (with importing network element ${importingNetworkElementId} and exporting network element ${exportingNetworkElementId})" +
                " at state ${stateId} has an angle of ${angle}°.")
            .withUntypedValue("angleCnecId", angleCnecId)
            .withUntypedValue("importingNetworkElementId", importingNetworkElementId)
            .withUntypedValue("exportingNetworkElementId", exportingNetworkElementId)
            .withUntypedValue("stateId", stateId)
            .withUntypedValue(ANGLE_LOWER_CASE, MonitoringCommonReports.formatDouble(angle))
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

    public static ReportNode reportNoRaAvailable(ReportNode reportNode, String cnecId, String stateId) {
        return MonitoringCommonReports.reportNoRaAvailable(reportNode, cnecId, stateId, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportConstraintInPreventive(ReportNode reportNode, String cnecId) {
        return MonitoringCommonReports.reportConstraintInPreventive(reportNode, cnecId, ANGLE_CAMEL_CASE); // TODO test this
    }

    public static ReportNode reportIgnoredRemedialActionForState(ReportNode reportNode, String remedialActionId, String cnecId, String stateId) {
        return MonitoringCommonReports.reportIgnoredRemedialActionForState(reportNode, remedialActionId, cnecId, stateId, ANGLE_CAMEL_CASE); // TODO test this
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

    static ReportNode reportPostContingencyTask(State state, ReportNode rootReportNode) {
        return MonitoringCommonReports.reportPostContingencyTask(state, rootReportNode, ANGLE_CAMEL_CASE); // TODO test this
    }

    static ReportNode generatePostContingencyRootReportNode() {
        return MonitoringCommonReports.generatePostContingencyRootReportNode(ANGLE_CAMEL_CASE);
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
