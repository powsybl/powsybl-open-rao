package com.powsybl.openrao.data.craccreation.creator.fbconstraint.craccreator;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

public class FbConstraintReports {
    public static ReportNode reportFbConstraintCracCreator(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("fbConstraintCracCreator", "Fb constraint crac creator")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportFbConstraintCracCreationContext(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("fbConstraintCracCreationContext", "Fb constraint crac creation context")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }
}
