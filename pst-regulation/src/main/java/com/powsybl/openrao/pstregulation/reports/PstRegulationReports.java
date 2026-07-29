/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.pstregulation.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;

import java.util.Set;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class PstRegulationReports {
    private PstRegulationReports() {
        // Utility class should not be instantiated
    }

    public static ReportNode reportPstRegulation(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportPstRegulation")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("PST regulation [start]");

        return addedNode;
    }

    public static void reportPstRegulationEnd() {
        BUSINESS_LOGS.info("PST regulation [end]");
    }

    public static void reportContingencyScenariosToRegulate(final ReportNode parentNode, final Set<Contingency> contingencies) {
        final int nbContingencies = contingencies.size();
        final String contingenciesList = String.join(", ", contingencies.stream().map(contingency -> contingency.getName().orElse(contingency.getId())).sorted().toList());

        parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportContingencyScenariosToRegulate")
            .withUntypedValue("nbContingencies", nbContingencies)
            .withUntypedValue("contingenciesList", contingenciesList)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("{} contingency scenario(s) to regulate: {}", nbContingencies, contingenciesList);
    }

    public static void reportPstsToRegulate(final ReportNode parentNode, final Set<PstRangeAction> rangeActionsToRegulate) {
        final int nbPsts = rangeActionsToRegulate.size();
        final String pstsList = String.join(", ", rangeActionsToRegulate.stream().map(PstRangeAction::getName).sorted().toList());

        parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportPstsToRegulate")
            .withUntypedValue("nbPsts", nbPsts)
            .withUntypedValue("pstsList", pstsList)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("{} PST(s) to regulate: {}", nbPsts, pstsList);
    }

    public static void reportErrorDuringPstRegulation(final ReportNode parentNode, final String errorMessage) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportErrorDuringPstRegulation")
            .withUntypedValue("errorMessage", errorMessage)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("An error occurred during PST regulation, pre-regulation RAO result will be kept. Error was: {}", errorMessage);
    }

    public static void reportPstCannotBeRegulated(final ReportNode parentNode, final String pstId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportPstCannotBeRegulated")
            .withUntypedValue("pstId", pstId)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("PST {} cannot be regulated as no curative PST range action was defined for it.", pstId);
    }

    public static void reportPstRegulationTriggeredDueToOverloadedFlowCnec(final ReportNode parentNode,
                                                                           final String flowCnec,
                                                                           final String contingency,
                                                                           final String allShiftedPstsDetails) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.pstregulation.reportPstRegulationTriggeredDueToOverloadedFlowCnec")
            .withUntypedValue("flowCnec", flowCnec)
            .withUntypedValue("contingency", contingency)
            .withUntypedValue("allShiftedPstsDetails", allShiftedPstsDetails)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("FlowCNEC '{}' of contingency scenario '{}' is overloaded and is the most limiting element, PST regulation has been triggered: {}", flowCnec, contingency, allShiftedPstsDetails);
    }
}
