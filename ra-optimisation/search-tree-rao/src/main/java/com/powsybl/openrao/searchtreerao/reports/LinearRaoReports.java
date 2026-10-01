/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Comparator;
import java.util.stream.Collectors;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * Reports of the {@code LinearRao} provider.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class LinearRaoReports {

    private LinearRaoReports() {
        // Utility class should not be instantiated
    }

    public static void reportLinearRaoNetworkActionsIgnored(final ReportNode parentNode, final int nbNetworkActions) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearRaoNetworkActionsIgnored")
            .withUntypedValue("nbNetworkActions", nbNetworkActions)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("[LINEAR RAO] The CRAC contains {} network action(s) which will not be taken into account: only range actions are optimized", nbNetworkActions);
    }

    public static void reportLinearRaoInitialSensitivityAnalysisResults(final ReportNode parentNode,
                                                                        final ObjectiveFunction objectiveFunction,
                                                                        final RemedialActionActivationResult remedialActionActivationResult,
                                                                        final PrePerimeterResult sensitivityAnalysisResult,
                                                                        final RaoParameters raoParameters,
                                                                        final int numberOfLoggedLimitingElements) {
        CommonReports.reportSensitivityAnalysisResults(
            parentNode,
            "openrao.searchtreerao.reportLinearRaoInitialSensitivityAnalysisResults",
            "[LINEAR RAO] Initial sensitivity analysis: ",
            objectiveFunction,
            remedialActionActivationResult,
            sensitivityAnalysisResult,
            raoParameters,
            numberOfLoggedLimitingElements
        );
    }

    public static ReportNode reportLinearRaoGlobalOptimization(final ReportNode parentNode) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearRaoGlobalOptimization")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("[LINEAR RAO] Global linear optimization of all range actions");

        return addedNode;
    }

    public static void reportLinearRaoActivatedRangeActions(final ReportNode parentNode,
                                                            final RangeActionActivationResult result) {
        final String variations = result.getActivatedRangeActionsPerState().entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getId()))
            .flatMap(entry -> entry.getValue().stream()
                .sorted(Comparator.comparing(RangeAction::getId))
                .map(rangeAction -> describeActivatedRangeAction(result, entry.getKey(), rangeAction)))
            .collect(Collectors.joining(", "));
        if (variations.isEmpty()) {
            return;
        }
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLinearRaoActivatedRangeAction")
            .withUntypedValue("variations", variations)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("[LINEAR RAO] Activated range actions: {}", variations);
    }

    private static String describeActivatedRangeAction(final RangeActionActivationResult result,
                                                       final State state,
                                                       final RangeAction<?> rangeAction) {
        final String variation;
        if (rangeAction instanceof PstRangeAction pstRangeAction) {
            variation = String.format("%d (delta: %+d)", result.getOptimizedTap(pstRangeAction, state), result.getTapVariation(pstRangeAction, state));
        } else {
            variation = String.format("%.2f (delta: %+.2f)", result.getOptimizedSetpoint(rangeAction, state), result.getSetPointVariation(rangeAction, state));
        }
        return String.format("%s@%s: %s", rangeAction.getId(), state.getId(), variation);
    }

    public static void reportLinearRaoFinalResult(final ReportNode parentNode,
                                                  final PrePerimeterResult sensitivityAnalysisResult,
                                                  final RaoParameters parameters,
                                                  final int numberLoggedElementsDuringRao) {
        CommonReports.reportObjectiveFunctionResult(
            parentNode,
            "openrao.searchtreerao.reportLinearRaoFinalResult",
            "[LINEAR RAO] Final result: ",
            sensitivityAnalysisResult,
            sensitivityAnalysisResult,
            sensitivityAnalysisResult,
            parameters,
            numberLoggedElementsDuringRao
        );
    }
}
