package com.powsybl.openrao.data.refprog.referenceprogram;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

public final class Reports {
    private Reports() {
    }

    public static ReportNode reportLoadflowUnsecure(ReportNode reportNode) {
        String warnMessage = "LoadFlow could not be computed. The ReferenceProgram will be built without a prior LoadFlow computation";
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowUnsecure", warnMessage)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(warnMessage);
        return addedNode;
    }

    public static ReportNode reportLoadflowException(ReportNode reportNode, String exceptionMessage) {
        ReportNode addedNode = reportNode.newReportNode()
                .withMessageTemplate("loadflowUnsecure", "LoadFlow could not be computed. The ReferenceProgram will be built without a prior LoadFlow computation: ${exceptionMessage}")
                .withUntypedValue("exceptionMessage", exceptionMessage)
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
        BUSINESS_WARNS.warn(String.format("LoadFlow could not be computed. The ReferenceProgram will be built without a prior LoadFlow computation: %s", exceptionMessage));
        return addedNode;
    }
}
