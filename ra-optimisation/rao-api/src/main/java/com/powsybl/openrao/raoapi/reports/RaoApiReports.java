/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.reports;

import com.powsybl.commons.report.ReportNode;

import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class RaoApiReports {
    private RaoApiReports() {
        // Utility class should not be instantiated
    }

    public static void reportRaoVersionAndGitCommit(final ReportNode parentNode, final String openRaoVersion, final String gitCommit) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportRaoVersionAndGitCommit")
                .withUntypedValue("openRaoVersion", openRaoVersion)
                .withUntypedValue("gitCommit", gitCommit)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("Running RAO using Open RAO version {} from git commit {}.", openRaoVersion, gitCommit);
    }

    public static void reportNegativeMinimumObjectiveImprovement(final ReportNode parentNode, final double curativeRaoMinObjImprovement) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportNegativeMinimumObjectiveImprovement")
                .withUntypedValue("curativeRaoMinObjImprovement", curativeRaoMinObjImprovement)
                .withUntypedValue("positiveCurativeRaoMinObjImprovement", -curativeRaoMinObjImprovement)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
    }

    public static void reportNegativeSensitivityFailureOvercost(final ReportNode parentNode, final double sensitivityFailureOvercost) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportNegativeSensitivityFailureOvercost")
                .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
                .withUntypedValue("positiveSensitivityFailureOvercost", -sensitivityFailureOvercost)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("The value {} for `sensitivity-failure-overcost` is smaller than 0. This would encourage the optimizer to make the loadflow diverge. Thus, it will be set to + {}", sensitivityFailureOvercost, -sensitivityFailureOvercost);
    }

    public static void reportNegativeMaxNumberOfBoundariesForSkippingActions(final ReportNode parentNode, final int maxNumberOfBoundariesForSkippingActions) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportNegativeMaxNumberOfBoundariesForSkippingActions")
                .withUntypedValue("maxNumberOfBoundariesForSkippingActions", maxNumberOfBoundariesForSkippingActions)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("The value {} provided for max number of boundaries for skipping actions is smaller than 0. It will be set to 0.", maxNumberOfBoundariesForSkippingActions);
    }

    public static void reportCappingRelativeMinimumImpactThreshold(final ReportNode parentNode, final double relativeMinImpactThreshold) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportCappingRelativeMinimumImpactThreshold")
                .withUntypedValue("relativeMinImpactThreshold", relativeMinImpactThreshold)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("The value {} provided for relativeminimum impact threshold is greater than 1. It will be set to 1.", relativeMinImpactThreshold);
    }

    public static void reportNegativeRelativeMinimumImpactThreshold(final ReportNode parentNode, final double relativeMinImpactThreshold) {
        parentNode.newReportNode()
                .withMessageTemplate("openrao.raoapi.reportNegativeRelativeMinimumImpactThreshold")
                .withUntypedValue("relativeMinImpactThreshold", relativeMinImpactThreshold)
                .withSeverity(WARN_SEVERITY)
                .add();

        BUSINESS_WARNS.warn("The value {} provided for relative minimum impact threshold is smaller than 0. It will be set to 0.", relativeMinImpactThreshold);
    }
}
