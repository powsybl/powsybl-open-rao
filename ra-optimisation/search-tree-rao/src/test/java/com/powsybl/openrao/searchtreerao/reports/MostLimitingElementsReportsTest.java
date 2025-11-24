/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.openrao.commons.Unit.AMPERE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class MostLimitingElementsReportsTest {

    private ObjectiveFunctionResult objectiveFunctionResult;
    private FlowResult flowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private FlowCnec cnec5;
    private FlowCnec cnec6;
    private State statePreventive;
    private State stateCo1Auto;
    private State stateCo1Curative;
    private State stateCo2Curative;
    private OptimizationResult basecaseOptimResult;
    private ReportNode reportNode;

    @BeforeEach
    public void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        flowResult = mock(FlowResult.class);
        basecaseOptimResult = mock(OptimizationResult.class);
        Instant preventiveInstant = mock(Instant.class);
        when(preventiveInstant.isPreventive()).thenReturn(true);
        when(preventiveInstant.getKind()).thenReturn(InstantKind.PREVENTIVE);
        Instant autoInstant = mock(Instant.class);
        when(autoInstant.getKind()).thenReturn(InstantKind.AUTO);
        Instant curativeInstant = mock(Instant.class);
        when(curativeInstant.getKind()).thenReturn(InstantKind.CURATIVE);
        when(curativeInstant.getOrder()).thenReturn(3);
        statePreventive = mockState("preventive", preventiveInstant);
        stateCo1Auto = mockState("co1 - auto", autoInstant);
        stateCo1Curative = mockState("co1 - curative", curativeInstant);
        stateCo2Curative = mockState("co2 - curative", curativeInstant);
        when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);

        cnec1 = mockCnec("ne1", stateCo1Curative, -10, -10, 30, 300, 0.1);
        cnec2 = mockCnec("ne2", statePreventive, 0, 0, -10, -10, 0.2);
        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 100, 10, 200, 0.3);
        cnec4 = mockCnec("ne4", stateCo1Auto, 20, 200, 0, 0, 0.4);
        cnec5 = mockCnec("ne5", stateCo1Curative, 30, 300, 20, 100, 0.5);
        cnec6 = mockCnec("ne6", stateCo1Curative, -0.0003, -0.0003, -0.002, -0.002, 0.5);
    }

    private State mockState(String stateId, Instant instant) {
        State state = mock(State.class);
        when(state.getId()).thenReturn(stateId);
        when(state.getInstant()).thenReturn(instant);
        return state;
    }

    private FlowCnec mockCnec(String neName, State state, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        NetworkElement ne = mock(NetworkElement.class);
        when(ne.getName()).thenReturn(neName);
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getNetworkElement()).thenReturn(ne);
        when(cnec.getState()).thenReturn(state);
        String cnecId = neName + " @ " + state.getId();
        when(cnec.getId()).thenReturn(cnecId);
        mockCnecFlowResult(flowResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        mockCnecFlowResult(basecaseOptimResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        when(cnec.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE));
        return cnec;
    }

    private void mockCnecFlowResult(FlowResult flowResult, FlowCnec cnec, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec, TwoSides.ONE)).thenReturn(ptdf);
    }

    private String absoluteMarginLog(int order, double margin, Unit unit, FlowCnec cnec) {
        return marginLog(order, margin, false, null, unit, cnec);
    }

    private String relativeMarginLog(int order, double margin, Double ptdf, Unit unit, FlowCnec cnec) {
        return marginLog(order, margin, true, ptdf, unit, cnec);
    }

    private String marginLog(int order, double margin, boolean relative, Double ptdf, Unit unit, FlowCnec cnec) {
        String relativeMargin = relative ? " relative" : "";
        String ptdfString = (ptdf != null) ? format(" (PTDF %f)", ptdf) : "";
        String descriptor = format("%s at state %s", cnec.getNetworkElement().getName(), cnec.getState().getId());
        return format(Locale.ENGLISH, "[INFO] Limiting element #%02d:%s margin = %s %s%s, element %s, CNEC ID = \"%s\"", order, relativeMargin, margin, unit, ptdfString, descriptor, cnec.getId());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnAllStatesAbsoluteMW() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.MEGAWATT, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(5, traceReports.size());
        assertEquals(6, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -10, MEGAWATT, cnec1).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, MEGAWATT, cnec2).endsWith(traceReports.get(1).getMessage()));
        assertTrue(absoluteMarginLog(3, 10, MEGAWATT, cnec3).endsWith(traceReports.get(2).getMessage()));
        assertTrue(absoluteMarginLog(4, 20, MEGAWATT, cnec4).endsWith(traceReports.get(3).getMessage()));
        assertTrue(absoluteMarginLog(5, 30, MEGAWATT, cnec5).endsWith(traceReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), technicalLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), technicalLogs.list.get(2).toString());
        assertEquals(absoluteMarginLog(3, 10, MEGAWATT, cnec3), technicalLogs.list.get(3).toString());
        assertEquals(absoluteMarginLog(4, 20, MEGAWATT, cnec4), technicalLogs.list.get(4).toString());
        assertEquals(absoluteMarginLog(5, 30, MEGAWATT, cnec5), technicalLogs.list.get(5).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnAllStatesRelativeMW() {
        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.MEGAWATT, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(5, traceReports.size());
        assertEquals(6, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -10, MEGAWATT, cnec1).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, MEGAWATT, cnec2).endsWith(traceReports.get(1).getMessage()));
        assertTrue(relativeMarginLog(3, 100, .3, MEGAWATT, cnec3).endsWith(traceReports.get(2).getMessage()));
        assertTrue(relativeMarginLog(4, 200, .4, MEGAWATT, cnec4).endsWith(traceReports.get(3).getMessage()));
        assertTrue(relativeMarginLog(5, 300, .5, MEGAWATT, cnec5).endsWith(traceReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), technicalLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), technicalLogs.list.get(2).toString());
        assertEquals(relativeMarginLog(3, 100, .3, MEGAWATT, cnec3), technicalLogs.list.get(3).toString());
        assertEquals(relativeMarginLog(4, 200, .4, MEGAWATT, cnec4), technicalLogs.list.get(4).toString());
        assertEquals(relativeMarginLog(5, 300, .5, MEGAWATT, cnec5), technicalLogs.list.get(5).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnAllStatesAbsoluteAmpere() {
        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(5, traceReports.size());
        assertEquals(6, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -10, AMPERE, cnec2).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, AMPERE, cnec4).endsWith(traceReports.get(1).getMessage()));
        assertTrue(absoluteMarginLog(3, 10, AMPERE, cnec3).endsWith(traceReports.get(2).getMessage()));
        assertTrue(absoluteMarginLog(4, 20, AMPERE, cnec5).endsWith(traceReports.get(3).getMessage()));
        assertTrue(absoluteMarginLog(5, 30, AMPERE, cnec1).endsWith(traceReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), technicalLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), technicalLogs.list.get(2).toString());
        assertEquals(absoluteMarginLog(3, 10, AMPERE, cnec3), technicalLogs.list.get(3).toString());
        assertEquals(absoluteMarginLog(4, 20, AMPERE, cnec5), technicalLogs.list.get(4).toString());
        assertEquals(absoluteMarginLog(5, 30, AMPERE, cnec1), technicalLogs.list.get(5).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnAllStatesRelativeAmpere() {
        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(5, traceReports.size());
        assertEquals(6, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -10, AMPERE, cnec2).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, AMPERE, cnec4).endsWith(traceReports.get(1).getMessage()));
        assertTrue(relativeMarginLog(3, 100, .5, AMPERE, cnec5).endsWith(traceReports.get(2).getMessage()));
        assertTrue(relativeMarginLog(4, 200, .3, AMPERE, cnec3).endsWith(traceReports.get(3).getMessage()));
        assertTrue(relativeMarginLog(5, 300, .1, AMPERE, cnec1).endsWith(traceReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), technicalLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), technicalLogs.list.get(2).toString());
        assertEquals(relativeMarginLog(3, 100, .5, AMPERE, cnec5), technicalLogs.list.get(3).toString());
        assertEquals(relativeMarginLog(4, 200, .3, AMPERE, cnec3), technicalLogs.list.get(4).toString());
        assertEquals(relativeMarginLog(5, 300, .1, AMPERE, cnec1), technicalLogs.list.get(5).toString());
    }

    @Test
    void testGetMostLimitingElementsForNarrowMarginAbsoluteMW() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec6));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.MEGAWATT, 1);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -0.0003, MEGAWATT, cnec6).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -0.0003, MEGAWATT, cnec6), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetMostLimitingElementsForNarrowMarginRelativeMW() {
        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec6));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.MEGAWATT, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -0.0003, MEGAWATT, cnec6).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -0.0003, MEGAWATT, cnec6), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetMostLimitingElementsForNarrowMarginAbsoluteAmpere() {
        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec6));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, null, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -0.002, AMPERE, cnec6).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -0.002, AMPERE, cnec6), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetMostLimitingElementsForNarrowMarginRelativeAmpere() {
        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec6));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -0.002, AMPERE, cnec6).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -0.002, AMPERE, cnec6), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnSomeStatesAbsoluteMW() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(statePreventive), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.MEGAWATT, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, 0, MEGAWATT, cnec2).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, 0, MEGAWATT, cnec2), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnSomeStatesRelativeMW() {
        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.MEGAWATT, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(3, traceReports.size());
        assertEquals(4, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, -10, MEGAWATT, cnec1).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, MEGAWATT, cnec2).endsWith(traceReports.get(1).getMessage()));
        assertTrue(relativeMarginLog(3, 300, .5, MEGAWATT, cnec5).endsWith(traceReports.get(2).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), technicalLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), technicalLogs.list.get(2).toString());
        assertEquals(relativeMarginLog(3, 300, .5, MEGAWATT, cnec5), technicalLogs.list.get(3).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnSomeStatesAbsoluteAmpere() {
        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(stateCo2Curative), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(1, traceReports.size());
        assertEquals(2, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, 10, AMPERE, cnec3).endsWith(traceReports.getFirst().getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, 10, AMPERE, cnec3), technicalLogs.list.get(1).toString());
    }

    @Test
    void testGetSummaryFromObjFunctionResultOnSomeStatesRelativeAmpere() {
        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        final ListAppender<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs();

        MostLimitingElementsReports.reportTechnicalMostLimitingElements(reportNode, objectiveFunctionResult, flowResult, Set.of(stateCo2Curative, stateCo1Auto), ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.AMPERE, 5);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY).getFirst().getChildren();
        assertEquals(2, traceReports.size());
        assertEquals(3, technicalLogs.list.size());
        assertTrue(absoluteMarginLog(1, 0, AMPERE, cnec4).endsWith(traceReports.getFirst().getMessage()));
        assertTrue(relativeMarginLog(2, 200, .3, AMPERE, cnec3).endsWith(traceReports.get(1).getMessage()));
        assertEquals("[INFO] Most limiting elements:", technicalLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, 0, AMPERE, cnec4), technicalLogs.list.get(1).toString());
        assertEquals(relativeMarginLog(2, 200, .3, AMPERE, cnec3), technicalLogs.list.get(2).toString());
    }

    @Test
    void testGetSummaryFromScenarios() {
        Contingency contingency2 = mock(Contingency.class);
        when(stateCo2Curative.getContingency()).thenReturn(Optional.of(contingency2));

        Contingency contingency1 = mock(Contingency.class);
        when(stateCo1Auto.getContingency()).thenReturn(Optional.of(contingency1));
        when(stateCo1Curative.getContingency()).thenReturn(Optional.of(contingency1));

        Perimeter preventivePerimeter = new Perimeter(statePreventive, Set.of(stateCo2Curative));
        Perimeter curativePerimeter = new Perimeter(stateCo1Curative, null);
        Set<ContingencyScenario> contingencyScenarios = Set.of(
            ContingencyScenario.create()
                .withContingency(stateCo1Auto.getContingency().get())
                .withAutomatonState(stateCo1Auto)
                .withCurativePerimeter(curativePerimeter)
                .build());

        OptimizationResult co1AutoOptimResult = mock(OptimizationResult.class);
        PostPerimeterResult postCo1AutoResult = mock(PostPerimeterResult.class);
        when(postCo1AutoResult.optimizationResult()).thenReturn(co1AutoOptimResult);
        OptimizationResult co1CurativeOptimResult = mock(OptimizationResult.class);
        PostPerimeterResult postCo1CurativeResult = mock(PostPerimeterResult.class);
        when(postCo1CurativeResult.optimizationResult()).thenReturn(co1CurativeOptimResult);
        Map<State, PostPerimeterResult> contingencyOptimizationResults = Map.of(stateCo1Auto, postCo1AutoResult, stateCo1Curative, postCo1CurativeResult);

        mockCnecFlowResult(co1AutoOptimResult, cnec1, 25, 40, 15, 11, .1);
        mockCnecFlowResult(co1AutoOptimResult, cnec4, 35, 50, -21, -21, .4);
        mockCnecFlowResult(co1AutoOptimResult, cnec5, -45, -45, 10, 12, .5);

        mockCnecFlowResult(co1CurativeOptimResult, cnec1, 2, 1, -8, -8, .1);
        mockCnecFlowResult(co1CurativeOptimResult, cnec5, -8, -8, 12, 100, .5);

        ListAppender<ILoggingEvent> businessLogs;
        List<ReportNode> infoReports;

        // Absolute MW
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        businessLogs = ReportsTestUtils.getBusinessLogs();

        MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.MEGAWATT, 5);

        infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY).getFirst().getChildren();
        assertEquals(5, infoReports.size());
        assertEquals(6, businessLogs.list.size());
        assertTrue(absoluteMarginLog(1, -8, MEGAWATT, cnec5).endsWith(infoReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, MEGAWATT, cnec2).endsWith(infoReports.get(1).getMessage()));
        assertTrue(absoluteMarginLog(3, 2, MEGAWATT, cnec1).endsWith(infoReports.get(2).getMessage()));
        assertTrue(absoluteMarginLog(4, 10, MEGAWATT, cnec3).endsWith(infoReports.get(3).getMessage()));
        assertTrue(absoluteMarginLog(5, 35, MEGAWATT, cnec4).endsWith(infoReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", businessLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), businessLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), businessLogs.list.get(2).toString());
        assertEquals(absoluteMarginLog(3, 2, MEGAWATT, cnec1), businessLogs.list.get(3).toString());
        assertEquals(absoluteMarginLog(4, 10, MEGAWATT, cnec3), businessLogs.list.get(4).toString());
        assertEquals(absoluteMarginLog(5, 35, MEGAWATT, cnec4), businessLogs.list.get(5).toString());

        // Relative MW
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        businessLogs = ReportsTestUtils.getBusinessLogs();
        reportNode = ReportsTestUtils.getTestRootNode();

        MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.MEGAWATT, 5);

        infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY).getFirst().getChildren();
        assertEquals(5, infoReports.size());
        assertEquals(6, businessLogs.list.size());
        assertTrue(absoluteMarginLog(1, -8, MEGAWATT, cnec5).endsWith(infoReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, 0, MEGAWATT, cnec2).endsWith(infoReports.get(1).getMessage()));
        assertTrue(relativeMarginLog(3, 1, null, MEGAWATT, cnec1).endsWith(infoReports.get(2).getMessage()));
        assertTrue(relativeMarginLog(4, 50, null, MEGAWATT, cnec4).endsWith(infoReports.get(3).getMessage()));
        assertTrue(relativeMarginLog(5, 100, null, MEGAWATT, cnec3).endsWith(infoReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", businessLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), businessLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), businessLogs.list.get(2).toString());
        assertEquals(relativeMarginLog(3, 1, null, MEGAWATT, cnec1), businessLogs.list.get(3).toString());
        assertEquals(relativeMarginLog(4, 50, null, MEGAWATT, cnec4), businessLogs.list.get(4).toString());
        assertEquals(relativeMarginLog(5, 100, null, MEGAWATT, cnec3), businessLogs.list.get(5).toString());

        // Absolute A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec5, cnec1));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        businessLogs = ReportsTestUtils.getBusinessLogs();
        reportNode = ReportsTestUtils.getTestRootNode();

        MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN, Unit.AMPERE, 5);

        infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY).getFirst().getChildren();
        assertEquals(5, infoReports.size());
        assertEquals(6, businessLogs.list.size());
        assertTrue(absoluteMarginLog(1, -21, AMPERE, cnec4).endsWith(infoReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, -10, AMPERE, cnec2).endsWith(infoReports.get(1).getMessage()));
        assertTrue(absoluteMarginLog(3, -8, AMPERE, cnec1).endsWith(infoReports.get(2).getMessage()));
        assertTrue(absoluteMarginLog(4, 10, AMPERE, cnec3).endsWith(infoReports.get(3).getMessage()));
        assertTrue(absoluteMarginLog(5, 12, AMPERE, cnec5).endsWith(infoReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", businessLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), businessLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), businessLogs.list.get(2).toString());
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), businessLogs.list.get(3).toString());
        assertEquals(absoluteMarginLog(4, 10, AMPERE, cnec3), businessLogs.list.get(4).toString());
        assertEquals(absoluteMarginLog(5, 12, AMPERE, cnec5), businessLogs.list.get(5).toString());

        // Relative A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec1, cnec5));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        businessLogs = ReportsTestUtils.getBusinessLogs();
        reportNode = ReportsTestUtils.getTestRootNode();

        MostLimitingElementsReports.reportBusinessMostLimitingElements(reportNode, preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN, Unit.AMPERE, 5);

        infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY).getFirst().getChildren();
        assertEquals(5, infoReports.size());
        assertEquals(6, businessLogs.list.size());
        assertTrue(absoluteMarginLog(1, -21, AMPERE, cnec4).endsWith(infoReports.getFirst().getMessage()));
        assertTrue(absoluteMarginLog(2, -10, AMPERE, cnec2).endsWith(infoReports.get(1).getMessage()));
        assertTrue(absoluteMarginLog(3, -8, AMPERE, cnec1).endsWith(infoReports.get(2).getMessage()));
        assertTrue(relativeMarginLog(4, 100, null, AMPERE, cnec5).endsWith(infoReports.get(3).getMessage()));
        assertTrue(relativeMarginLog(5, 200, null, AMPERE, cnec3).endsWith(infoReports.get(4).getMessage()));
        assertEquals("[INFO] Most limiting elements:", businessLogs.list.getFirst().toString());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), businessLogs.list.get(1).toString());
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), businessLogs.list.get(2).toString());
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), businessLogs.list.get(3).toString());
        assertEquals(relativeMarginLog(4, 100, null, AMPERE, cnec5), businessLogs.list.get(4).toString());
        assertEquals(relativeMarginLog(5, 200, null, AMPERE, cnec3), businessLogs.list.get(5).toString());
    }
}
