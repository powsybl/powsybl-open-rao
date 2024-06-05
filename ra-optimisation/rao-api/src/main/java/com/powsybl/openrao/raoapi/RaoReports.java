package com.powsybl.openrao.raoapi;

import com.powsybl.commons.report.ReportNode;

import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class RaoReports {
    private RaoReports() {
        // Utility class
    }

    public static ReportNode reportRaoVersionAndCommit(ReportNode reportNode, String mavenProjectVersion, String gitVersion) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRaoVersionAndCommit", "Running RAO using Open RAO version ${mavenProjectVersion} from git commit ${gitVersion}.")
            .withUntypedValue("mavenProjectVersion", mavenProjectVersion)
            .withUntypedValue("gitVersion", gitVersion)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Running RAO using Open RAO version {} from git commit {}.", mavenProjectVersion, gitVersion);
        return addedNode;
    }

    public static ReportNode reportNegativeSensitivityFailureOvercost(ReportNode reportNode, double sensitivityFailureOvercost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNegativeSensitivityFailureOvercost", "The value ${sensitivityFailureOvercost} for `sensitivity-failure-overcost` is smaller than 0. This would encourage the optimizer to make the loadflow diverge. Thus, it will be set to + ${positiveSensitivityFailureOvercost}")
            .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
            .withUntypedValue("positiveSensitivityFailureOvercost", -sensitivityFailureOvercost)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} for `sensitivity-failure-overcost` is smaller than 0. This would encourage the optimizer to make the loadflow diverge. Thus, it will be set to + {}", sensitivityFailureOvercost, -sensitivityFailureOvercost);
        return addedNode;
    }

    public static ReportNode reportDisablingHvdcAcEmulation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportDisablingHvdcAcEmulation", "The runs are in DC but the HvdcAcEmulation parameter is on: this is not compatible. HvdcAcEmulation parameter set to false.")
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The runs are in DC but the HvdcAcEmulation parameter is on: this is not compatible. HvdcAcEmulation parameter set to false.");
        return addedNode;
    }

    public static ReportNode reportNegativeMinimumObjectiveImprovement(ReportNode reportNode, double curativeRaoMinObjImprovement) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNegativeMinimumObjectiveImprovement", "The value ${curativeRaoMinObjImprovement} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + ${positiveCurativeRaoMinObjImprovement}")
            .withUntypedValue("curativeRaoMinObjImprovement", curativeRaoMinObjImprovement)
            .withUntypedValue("positiveCurativeRaoMinObjImprovement", -curativeRaoMinObjImprovement)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
        return addedNode;
    }

    public static ReportNode reportNegativeRelativeMinimumImpactThreshold(ReportNode reportNode, double relativeMinImpactThreshold) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNegativeRelativeMinimumImpactThreshold", "The value ${relativeMinImpactThreshold} provided for relative minimum impact threshold is smaller than 0. It will be set to 0.")
            .withUntypedValue("relativeMinImpactThreshold", relativeMinImpactThreshold)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("The value {} provided for relative minimum impact threshold is smaller than 0. It will be set to 0.", relativeMinImpactThreshold);
        return addedNode;
    }
}
