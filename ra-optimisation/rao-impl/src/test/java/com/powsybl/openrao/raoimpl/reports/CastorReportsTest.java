/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoimpl.commons.RaoUtil;
import com.powsybl.openrao.raoimpl.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.raoimpl.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.raoimpl.result.api.PrePerimeterResult;
import com.powsybl.openrao.raoimpl.result.api.RemedialActionActivationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Maxence Fleury {@literal <maxence.fleury at artelys.com>}
 */
class CastorReportsTest {
    private ReportNode reportNode;
    private ObjectiveFunction objectiveFunction;
    private RemedialActionActivationResult remedialActionActivationResult;
    private PrePerimeterResult sensitivityAnalysisResult;
    private RaoParameters raoParameters;
    private int numberOfLoggedLimitingElements;

    @BeforeEach
    void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        objectiveFunction = Mockito.mock(ObjectiveFunction.class);
        remedialActionActivationResult = Mockito.mock(RemedialActionActivationResult.class);
        sensitivityAnalysisResult = Mockito.mock(PrePerimeterResult.class);
        raoParameters = Mockito.mock(RaoParameters.class);
        numberOfLoggedLimitingElements = 2;

        final ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        when(objectiveFunction.evaluate(any(), any(), any())).thenReturn(prePerimeterObjectiveFunctionResult);
        when(prePerimeterObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("test1", "test2", "test3"));
        when(prePerimeterObjectiveFunctionResult.getCost()).thenReturn(7.);
        when(prePerimeterObjectiveFunctionResult.getFunctionalCost()).thenReturn(4.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost()).thenReturn(3.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost("test1")).thenReturn(1.);
        when(prePerimeterObjectiveFunctionResult.getVirtualCost("test2")).thenReturn(2.);
        final ObjectiveFunctionParameters objectiveFunctionParameters = Mockito.mock(ObjectiveFunctionParameters.class);
        when(raoParameters.getObjectiveFunctionParameters()).thenReturn(objectiveFunctionParameters);
        when(objectiveFunctionParameters.getType()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MIN_COST);
    }

    @Test
    void testLogCastorInitialSensitivityAnalysisResults() {
        try (
            MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            CastorReports.reportCastorInitialSensitivityAnalysisResults(
                reportNode,
                objectiveFunction,
                remedialActionActivationResult,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals("Initial sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", infoReports.getFirst().getMessage());
            assertEquals("[INFO] Initial sensitivity analysis: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})", businessLogs.getFirst().toString());
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }

    @Test
    void testLogCastorSystematicSensitivityAnalysisAfterPraResults() {
        try (
            MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterPraResults(
                reportNode,
                objectiveFunction,
                remedialActionActivationResult,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals(
                "Systematic sensitivity analysis after preventive remedial actions: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})",
                infoReports.getFirst().getMessage()
            );
            assertEquals(
                "[INFO] Systematic sensitivity analysis after preventive remedial actions: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})",
                businessLogs.getFirst().toString()
            );
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }

    @Test
    void testLogCastorSystematicSensitivityAnalysisAfterCraResults() {
        try (
            MockedStatic<MostLimitingElementsReports> mostLimitingElementsReportsMockedStatic = Mockito.mockStatic(MostLimitingElementsReports.class);
            MockedStatic<RaoUtil> raoUtilMockedStatic = Mockito.mockStatic(RaoUtil.class)
        ) {
            raoUtilMockedStatic.when(() -> RaoUtil.getFlowUnit(raoParameters)).thenReturn(Unit.MEGAWATT);

            final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
            CastorReports.reportCastorSystematicSensitivityAnalysisAfterCraResults(
                reportNode,
                objectiveFunction,
                remedialActionActivationResult,
                sensitivityAnalysisResult,
                raoParameters,
                numberOfLoggedLimitingElements
            );
            final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
            assertEquals(
                "Systematic sensitivity analysis after curative remedial actions before second preventive optimization: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})",
                infoReports.getFirst().getMessage()
            );
            assertEquals(
                "[INFO] Systematic sensitivity analysis after curative remedial actions before second preventive optimization: cost = 7.0 (functional: 4.0, virtual: 3.0 {test2=2.0, test1=1.0})",
                businessLogs.getFirst().toString()
            );
            mostLimitingElementsReportsMockedStatic.verify(() -> MostLimitingElementsReports.reportBusinessMostLimitingElements(any(), any(), any(), any(), any(), anyInt()), times(1));
        }
    }

    @Test
    void testReportUnprotectedAutoCnec() {
        final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        when(flowCnec.getId()).thenReturn("autoCnec");

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
        CastorReports.reportUnprotectedAutoCnec(reportNode, flowCnec);
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals(
            "\"FlowCnec autoCnec is not protected by any automaton and will be included in the preventive optimization perimeter\"",
            infoReports.getFirst().getMessage()
        );
        assertEquals(
            "[INFO] FlowCnec autoCnec is not protected by any automaton and will be included in the preventive optimization perimeter",
            businessLogs.getFirst().toString()
        );
    }

    @Test
    void testLogDcPreSolveProposalWithNetworkActions() {
        final NetworkAction networkAction1 = Mockito.mock(NetworkAction.class);
        final NetworkAction networkAction2 = Mockito.mock(NetworkAction.class);
        when(networkAction1.getId()).thenReturn("na1");
        when(networkAction2.getId()).thenReturn("na2");
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        CastorReports.reportDcPreSolveProposal(reportNode, new LinkedHashSet<>(List.of(networkAction1, networkAction2)));

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("DC pre-solve proposes network action(s): na1 + na2", traceReports.getFirst().getMessage());
        assertEquals("[INFO] DC pre-solve proposes network action(s): na1 + na2", technicalLogs.getFirst().toString());
    }

    @Test
    void testLogDcPreSolveProposalWithNoNetworkAction() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        CastorReports.reportDcPreSolveProposal(reportNode, Set.of());

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("DC pre-solve found no network action to propose.", traceReports.getFirst().getMessage());
        assertEquals("[INFO] DC pre-solve found no network action to propose.", technicalLogs.getFirst().toString());
    }

    @Test
    void testLogDcPreSolveCombinationRetained() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        CastorReports.reportDcPreSolveCombinationRetained(reportNode, true);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("DC pre-solve combination retained in the AC search result: true", traceReports.getFirst().getMessage());
        assertEquals("[INFO] DC pre-solve combination retained in the AC search result: true", technicalLogs.getFirst().toString());
    }

    @Test
    void testLogDcPreSolveCombinationNotRetained() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        CastorReports.reportDcPreSolveCombinationRetained(reportNode, false);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        assertEquals("DC pre-solve combination retained in the AC search result: false", traceReports.getFirst().getMessage());
        assertEquals("[INFO] DC pre-solve combination retained in the AC search result: false", technicalLogs.getFirst().toString());
    }
}
