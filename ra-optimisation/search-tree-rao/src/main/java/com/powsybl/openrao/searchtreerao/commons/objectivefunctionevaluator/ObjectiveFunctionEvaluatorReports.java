package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class ObjectiveFunctionEvaluatorReports {
    private ObjectiveFunctionEvaluatorReports() {
        // Utility class
    }

    public static ReportNode reportSensitivityFailure(ReportNode reportNode, double sensitivityFailureOvercost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityFailure", "Sensitivity failure : assigning virtual overcost of ${sensitivityFailureOvercost}")
            .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Sensitivity failure : assigning virtual overcost of {}", sensitivityFailureOvercost);
        return addedNode;
    }

    public static ReportNode reportSensitivityFailureForState(ReportNode reportNode, String stateId, double sensitivityFailureOvercost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityFailureForState", "Sensitivity failure for state ${stateId} : assigning virtual overcost of ${sensitivityFailureOvercost}")
            .withUntypedValue("stateId", stateId)
            .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Sensitivity failure for state {} : assigning virtual overcost of {}", stateId, sensitivityFailureOvercost); // TODO test this
        return addedNode;
    }
}
