/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.Map;

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
        reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, prePerimeterObjectiveFunctionResult, sensitivityAnalysisResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportObjectiveFunctionResult(final ReportNode parentNode,
                                                     final String messageTemplate,
                                                     final String prefix,
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

        parentNode.newReportNode()
            .withMessageTemplate(messageTemplate)
            .withUntypedValue("cost", cost)
            .withUntypedValue("functionalCost", functionalCost)
            .withUntypedValue("virtualCost", virtualCost)
            .withUntypedValue("virtualCostDetail", virtualCostDetail)
            .withSeverity(INFO_SEVERITY)
            .add();

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

    public static void reportPstsMustBeApproximatedAsIntegers(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportPstsMustBeApproximatedAsIntegers")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("The PSTs must be approximated as integers to use the limitations of elementary actions as a constraint in the RAO.");
    }

    public static void reportLoopflowComputationLacksReferenceProgramOrGlskProvider(final ReportNode parentNode, final String cracId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportLoopflowComputationLacksReferenceProgramOrGlskProvider")
            .withUntypedValue("cracId", cracId)
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Loopflow computation cannot be performed on CRAC {} because it lacks a ReferenceProgram or a GlskProvider", cracId);
    }

    public static void reportNoReferenceProgramProvided(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportNoReferenceProgramProvided")
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("No ReferenceProgram provided. A ReferenceProgram will be generated using information in the network file.");
    }

    public static void reportThresholdForFlowCnecDefinedInMwButLoadflowComputationIsInAc(final ReportNode parentNode, final String flowCnecId) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportThresholdForFlowCnecDefinedInMwButLoadflowComputationIsInAc")
            .withUntypedValue("flowCnecId", flowCnecId)
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("A threshold for the flowCnec {} is defined in MW but the loadflow computation is in AC. It will be imprecisely converted by the RAO which could create uncoherent results due to side effects", flowCnecId);
    }
}
