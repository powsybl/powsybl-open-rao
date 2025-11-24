/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeAdder;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.commons.report.TypedValue.ERROR_SEVERITY;
import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class CommonReports {
    private CommonReports() {
        // Utility class should not be instantiated
    }

    public static void reportExceptionMessage(final ReportNode parentNode, final String exceptionMessage) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportExceptionMessage")
            .withUntypedValue("exceptionMessage", exceptionMessage)
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error(exceptionMessage);
    }

    public static void reportSensitivityAnalysisResults(final ReportNode parentNode,
                                                        final String messageTemplate,
                                                        final String prefix,
                                                        final ObjectiveFunction objectiveFunction,
                                                        final RemedialActionActivationResult remedialActionActivationResult,
                                                        final PrePerimeterResult sensitivityAnalysisResult,
                                                        final RaoParameters raoParameters,
                                                        final int numberOfLoggedLimitingElements) {
        final ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(sensitivityAnalysisResult, remedialActionActivationResult, parentNode);
        reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, null, prePerimeterObjectiveFunctionResult, sensitivityAnalysisResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportObjectiveFunctionResult(final ReportNode parentNode,
                                                     final String messageTemplate,
                                                     final String prefix,
                                                     final Integer iterationCounter,
                                                     final ObjectiveFunctionResult objectiveFunctionResult,
                                                     final ObjectiveFunctionResult sensitivityAnalysisObjectiveFunctionResult,
                                                     final FlowResult sensitivityAnalysisFlowResult,
                                                     final RaoParameters raoParameters,
                                                     final int numberOfLoggedLimitingElements) {
        final Map<String, Double> virtualCostDetailed = RaoLogger.getVirtualCostDetailed(objectiveFunctionResult);

        final String cost = ReportUtils.formatDoubleBasedOnMargin(objectiveFunctionResult.getCost(), -objectiveFunctionResult.getCost());
        final String functionalCost = ReportUtils.formatDoubleBasedOnMargin(objectiveFunctionResult.getFunctionalCost(), -objectiveFunctionResult.getCost());
        final String virtualCost = ReportUtils.formatDoubleBasedOnMargin(objectiveFunctionResult.getVirtualCost(), -objectiveFunctionResult.getCost());
        final String virtualCostDetail = virtualCostDetailed.isEmpty() ? "" : " " + virtualCostDetailed;

        final ReportNodeAdder reportNodeAdder = parentNode.newReportNode()
            .withMessageTemplate(messageTemplate)
            .withUntypedValue("cost", cost)
            .withUntypedValue("functionalCost", functionalCost)
            .withUntypedValue("virtualCost", virtualCost)
            .withUntypedValue("virtualCostDetail", virtualCostDetail)
            .withSeverity(INFO_SEVERITY);
        if (iterationCounter != null) {
            reportNodeAdder.withUntypedValue("iterationCounter", iterationCounter);
        }
        reportNodeAdder.add();

        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {}{})", cost, functionalCost, virtualCost, virtualCostDetail);

        MostLimitingElementsReports.reportBusinessMostLimitingElements(parentNode,
            sensitivityAnalysisObjectiveFunctionResult,
            sensitivityAnalysisFlowResult,
            raoParameters.getObjectiveFunctionParameters().getType(),
            raoParameters.getObjectiveFunctionParameters().getUnit(),
            numberOfLoggedLimitingElements);
    }

    public static void reportInitialSensitivityAnalysisFailed(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportInitialSensitivityAnalysisFailed")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
    }

    public static void reportContingencyWithAutomatonOrCraButNoCnec(final ReportNode parentNode, final String contingencyId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportContingencyWithAutomatonOrCraButNoCnec")
            .withUntypedValue("contingencyId", contingencyId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative remedial action but no CNECs associated.", contingencyId);
    }

    public static void reportLoopflowConstraintsNotRespected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLoopflowConstraintsNotRespected")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Some loopflow constraints are not respected.");
    }

    public static void reportMnecConstraintsNotRespected(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMnecConstraintsNotRespected")
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info("Some MNEC constraints are not respected.");
    }

    public static void reportRangeActionVariation(final ReportNode parentNode,
                                                  final RangeAction<?> rangeAction,
                                                  final double variation,
                                                  final State state,
                                                  final double after) {
        final String variationUnit = (rangeAction instanceof PstRangeAction) ? "taps" : "MW";
        final String rangeActionId = rangeAction.getId();
        final String variationAmount = BigDecimal.valueOf(variation).setScale(2, RoundingMode.HALF_UP).toString();
        final String initialValue = BigDecimal.valueOf(after - variation).setScale(2, RoundingMode.HALF_UP).toString();
        final String finalValue = BigDecimal.valueOf(after).setScale(2, RoundingMode.HALF_UP).toString();

        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRangeActionVariation")
            .withUntypedValue("rangeActionId", rangeActionId)
            .withUntypedValue("variationAmount", variationAmount)
            .withUntypedValue("variationUnit", variationUnit)
            .withUntypedValue("state", Objects.toString(state))
            .withUntypedValue("initialValue", initialValue)
            .withUntypedValue("finalValue", finalValue)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.debug("{} variation of {} {} at state {} ({} -> {})",
            rangeActionId, variationAmount, variationUnit, state, initialValue, finalValue);
    }

    public static void reportAssigningVirtualCostToSensitivityFailure(final ReportNode parentNode, final double sensitivityFailureOvercost) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportAssigningVirtualCostToSensitivityFailure")
            .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info(String.format("Sensitivity failure : assigning virtual overcost of %s", sensitivityFailureOvercost));
    }

    public static void reportAssigningVirtualCostToSensitivityFailureForState(final ReportNode parentNode,
                                                                              final String stateId,
                                                                              final double sensitivityFailureOvercost) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportAssigningVirtualCostToSensitivityFailureForState")
            .withUntypedValue("stateId", stateId)
            .withUntypedValue("sensitivityFailureOvercost", sensitivityFailureOvercost)
            .withSeverity(TRACE_SEVERITY)
            .add();

        TECHNICAL_LOGS.info(String.format("Sensitivity failure for state %s : assigning virtual overcost of %s", stateId, sensitivityFailureOvercost));
    }

    public static void reportRangeActionInitialSetpointDoesNotRespectAllowedRange(final ReportNode parentNode,
                                                                                  final String rangeActionId,
                                                                                  final double initialSetPoint,
                                                                                  final double minSetPoint,
                                                                                  final double maxSetPoint) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRangeActionInitialSetpointDoesNotRespectAllowedRange")
            .withUntypedValue("rangeActionId", rangeActionId)
            .withUntypedValue("initialSetPoint", initialSetPoint)
            .withUntypedValue("minSetPoint", minSetPoint)
            .withUntypedValue("maxSetPoint", maxSetPoint)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Range action {} has an initial setpoint of {} that does not respect its allowed range [{} {}]. It will be filtered out of the linear problem.", rangeActionId, initialSetPoint, minSetPoint, maxSetPoint);
    }

    public static void reportRangeActionsOfGroupDoNotHaveSamePrePerimeterSetpoint(final ReportNode parentNode, final String groupId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportRangeActionsOfGroupDoNotHaveSamePrePerimeterSetpoint")
            .withUntypedValue("groupId", groupId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Range actions of group {} do not have the same prePerimeter setpoint. They will be filtered out of the linear problem.", groupId);
    }

    public static void reportPredefinedCombinationShouldContainAtLeast2NetworkActionIds(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPredefinedCombinationShouldContainAtLeast2NetworkActionIds")
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("A predefined combination should contain at least 2 NetworkAction ids");
    }

    public static void reportUnknownNetworkActionIdInPredefinedCombinationsParameter(final ReportNode parentNode, final String networkActionId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportUnknownNetworkActionIdInPredefinedCombinationsParameter")
            .withUntypedValue("networkActionId", networkActionId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Unknown network action id in predefined-combinations parameter: {}", networkActionId);
    }

    public static void reportNoGlskFoundForCountryEICode(final ReportNode parentNode, final String eiCode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoGlskFoundForCountryEICode")
            .withUntypedValue("eiCode", eiCode)
            .withSeverity(WARN_SEVERITY)
            .add();

        TECHNICAL_LOGS.warn("No GLSK found for CountryEICode {}", eiCode);
    }
}
