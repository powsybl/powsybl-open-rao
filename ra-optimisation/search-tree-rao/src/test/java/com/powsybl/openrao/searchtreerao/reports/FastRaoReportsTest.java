/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class FastRaoReportsTest {
    private ReportNode reportNode;
    private PrePerimeterResult sensitivityAnalysisResult;
    private RaoParameters raoParameters;
    private int numberOfLoggedLimitingElements;

    @BeforeEach
    void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        sensitivityAnalysisResult = Mockito.mock(PrePerimeterResult.class);
        raoParameters = Mockito.mock(RaoParameters.class);
        numberOfLoggedLimitingElements = 2;

        when(sensitivityAnalysisResult.getVirtualCostNames()).thenReturn(Set.of("test1", "test2", "test3"));
        when(sensitivityAnalysisResult.getCost()).thenReturn(7.);
        when(sensitivityAnalysisResult.getFunctionalCost()).thenReturn(4.);
        when(sensitivityAnalysisResult.getVirtualCost()).thenReturn(3.);
        when(sensitivityAnalysisResult.getVirtualCost("test1")).thenReturn(1.);
        when(sensitivityAnalysisResult.getVirtualCost("test2")).thenReturn(2.);
        final ObjectiveFunctionParameters objectiveFunctionParameters = Mockito.mock(ObjectiveFunctionParameters.class);
        when(raoParameters.getObjectiveFunctionParameters()).thenReturn(objectiveFunctionParameters);
        when(objectiveFunctionParameters.getType()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
    }

    @Test
    void testLogFastRaoInitialSensitivityAnalysisResults() {
        final ObjectiveFunction objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        final RemedialActionActivationResult remedialActionActivationResult = Mockito.mock(RemedialActionActivationResult.class);

        final ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(objectiveFunction.evaluate(any(), any(), any())).thenReturn(prePerimeterObjectiveFunctionResult);
        when(prePerimeterObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("test1", "test2", "test3"));
        when(prePerimeterObjectiveFunctionResult.getCost()).thenReturn(7.);
        when(prePerimeterObjectiveFunctionResult.getFunctionalCost()).thenReturn(4.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost()).thenReturn(3.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost("test1")).thenReturn(1.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost("test2")).thenReturn(2.);
        final ObjectiveFunctionParameters objectiveFunctionParameters = Mockito.mock(ObjectiveFunctionParameters.class);
        when(objectiveFunctionParameters.getType()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);

        try (
            final MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            final MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            FastRaoReports.reportFastRaoInitialSensitivityAnalysisResults(
                reportNode,
                objectiveFunction,
                remedialActionActivationResult,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals("[FAST RAO] Initial sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", infoReports.getFirst().getMessage());
            assertEquals("[INFO] [FAST RAO] Initial sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", businessLogs.getFirst().toString());
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }

    @Test
    void testLogFastRaoIterationIntermediateResult() {
        try (
            final MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            final MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            FastRaoReports.reportFastRaoIterationIntermediateResult(
                reportNode,
                1,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals("[FAST RAO] Sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", infoReports.getFirst().getMessage());
            assertEquals("[INFO] [FAST RAO] Iteration 1: sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", businessLogs.getFirst().toString());
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }

    @Test
    void testLogFastRaoFinalResult() {
        try (
            final MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            final MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            FastRaoReports.reportFastRaoFinalResult(
                reportNode,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals("[FAST RAO] Final Result: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", infoReports.getFirst().getMessage());
            assertEquals("[INFO] [FAST RAO] Final Result: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", businessLogs.getFirst().toString());
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }
}
