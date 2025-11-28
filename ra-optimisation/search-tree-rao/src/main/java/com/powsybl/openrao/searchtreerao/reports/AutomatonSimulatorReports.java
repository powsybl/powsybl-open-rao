/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;

import java.util.Map;
import java.util.Set;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class AutomatonSimulatorReports {
    private AutomatonSimulatorReports() {
        // Utility class should not be instantiated
    }

    public static ReportNode reportOptimizingAutomatonState(final ReportNode parentNode, final String automatonStateId) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportOptimizingAutomatonState")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonStateId);

        return addedNode;
    }

    public static void reportInitialSituation(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportInitialSituation")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Initial situation:");
    }

    public static void reportSimulatingAutomatonBatch(final ReportNode parentNode, final int speed, final String automatonStateId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportSimulatingAutomatonBatch")
            .withUntypedValue("speed", speed)
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Simulating automaton batch of speed {} for automaton state {}", speed, automatonStateId);
    }

    public static void reportAutomatonStateOptimized(final ReportNode parentNode, final String automatonStateId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportAutomatonStateOptimized")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonStateId);
    }

    public static void reportAutomatonSimulationFailedRangeActionSensitivityComputation(final ReportNode parentNode,
                                                                                        final String automatonStateId,
                                                                                        final String failDescription) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportAutomatonSimulationFailedRangeActionSensitivityComputation")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withUntypedValue("failDescription", failDescription)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {}", automatonStateId, failDescription);
    }

    public static void reportFailedOptimizationSummary(final ReportNode parentNode,
                                                       final State optimizedState,
                                                       final Set<NetworkAction> networkActions,
                                                       final Map<RangeAction<?>, Double> rangeActions) {
        final String scenarioName = ReportUtils.getScenarioName(optimizedState);
        final String raResult = ReportUtils.getRaResult(networkActions, rangeActions);
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFailedOptimizationSummary")
            .withUntypedValue("scenarioName", scenarioName)
            .withUntypedValue("raResult", raResult)
            .withSeverity(INFO_SEVERITY)
            .add();

        logFailedOptimizationSummary(BUSINESS_LOGS, scenarioName, raResult);
    }

    public static void logFailedOptimizationSummary(final OpenRaoLogger logger, final String scenarioName, final String raResult) {
        logger.info("Scenario \"{}\": {}", scenarioName, raResult);
    }

    public static void reportAutomatonSkipped(final ReportNode parentNode, final String networkActionId, final String networkActionName) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportAutomatonSkipped")
            .withUntypedValue("networkActionId", networkActionId)
            .withUntypedValue("networkActionName", networkActionName)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Automaton {} - {} has been skipped as it has no impact on network.", networkActionId, networkActionName);
    }

    public static void reportRunSensitivityAnalysisPostApplicationForStateAndSpeed(final ReportNode parentNode,
                                                                                   final String automatonStateId,
                                                                                   final int speed) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRunSensitivityAnalysisPostApplicationForStateAndSpeed")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withUntypedValue("speed", speed)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Running sensitivity analysis post application of auto network actions for automaton state {} for speed {}.", automatonStateId, speed);
    }

    public static void reportHeterogenousRangeActionGroupTypes(final ReportNode parentNode, final String groupId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportHeterogenousRangeActionGroupTypes")
            .withUntypedValue("groupId", groupId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Range action group {} contains range actions of different types; they are not simulated", groupId);
    }

    public static void reportRangeActionGroupNotAllAvailableAtAutoInstant(final ReportNode parentNode, final String groupId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRangeActionGroupNotAllAvailableAtAutoInstant")
            .withUntypedValue("groupId", groupId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Range action group {} contains range actions not all available at AUTO instant; they are not simulated", groupId);
    }

    public static void reportRunPostRangeSensitivityAnalysisForStateAndSpeed(final ReportNode parentNode,
                                                                             final String automatonStateId,
                                                                             final int speed) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRunPostRangeSensitivityAnalysisForStateAndSpeed")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withUntypedValue("speed", speed)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Running post range automatons sensitivity analysis after auto state {} for speed {}.", automatonStateId, speed);
    }

    public static void reportRunSensitivityAnalysisAfterDisablingAngleDroopActivePowerControlOnHvdcRa(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRunSensitivityAnalysisAfterDisablingAngleDroopActivePowerControlOnHvdcRa")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Running sensitivity analysis after disabling AngleDroopActivePowerControl on HVDC RAs.");
    }
}
