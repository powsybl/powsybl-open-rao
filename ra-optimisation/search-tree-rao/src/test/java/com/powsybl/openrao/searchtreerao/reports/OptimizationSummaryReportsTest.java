/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class OptimizationSummaryReportsTest {

    private ObjectiveFunctionResult objectiveFunctionResult;
    private ReportNode reportNode;
    private State preventive;
    private State curative;
    private ObjectiveFunctionResult initialObjectiveFunctionResult;
    private Set<NetworkAction> networkActions;
    private Map<RangeAction<?>, Double> rangeActions;

    @BeforeEach
    public void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        preventive = Mockito.mock(State.class);
        Instant preventiveInstant = Mockito.mock(Instant.class);
        when(preventiveInstant.toString()).thenReturn("preventive");
        when(preventive.getInstant()).thenReturn(preventiveInstant);
        curative = Mockito.mock(State.class);
        Instant curativeInstant = Mockito.mock(Instant.class);
        when(curativeInstant.toString()).thenReturn("curative");
        when(curative.getInstant()).thenReturn(curativeInstant);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getName()).thenReturn(Optional.of("contingency"));
        when(curative.getContingency()).thenReturn(Optional.of(contingency));
        initialObjectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        NetworkAction fakeRA = Mockito.mock(NetworkAction.class);
        when(fakeRA.getName()).thenReturn("Open_fake_RA");
        networkActions = Set.of(fakeRA);

        RangeAction<?> fakePST1 = Mockito.mock(RangeAction.class);
        RangeAction<?> fakePST2 = Mockito.mock(RangeAction.class);
        when(fakePST1.getName()).thenReturn("PST_1");
        when(fakePST2.getName()).thenReturn("PST_2");
        rangeActions = new HashMap<>();
        rangeActions.put(fakePST1, -2.);
        rangeActions.put(fakePST2, 4.);
    }

    private void setupObjectiveFunctionResultsWithoutVirtualCost(final double initialCost,
                                                                 final double initialFunctionalCost,
                                                                 final double initialVirtualCost,
                                                                 final double initialSensiFallbackCost,
                                                                 final double finalCost,
                                                                 final double finalFunctionalCost,
                                                                 final double finalVirtualCost,
                                                                 final double finalMnecViolationCost,
                                                                 final double finalLoopflowViolationCost) {
        // initial objective
        when(initialObjectiveFunctionResult.getCost()).thenReturn(initialCost);
        when(initialObjectiveFunctionResult.getFunctionalCost()).thenReturn(initialFunctionalCost);
        when(initialObjectiveFunctionResult.getVirtualCost()).thenReturn(initialVirtualCost);
        when(initialObjectiveFunctionResult.getVirtualCost("sensi-fallback-cost")).thenReturn(initialSensiFallbackCost);
        // final objective
        when(objectiveFunctionResult.getCost()).thenReturn(finalCost);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(finalFunctionalCost);
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(finalVirtualCost);
        when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("mnec-violation-cost", "loopflow-violation-cost"));
        when(objectiveFunctionResult.getVirtualCost("mnec-violation-cost")).thenReturn(finalMnecViolationCost);
        when(objectiveFunctionResult.getVirtualCost("loopflow-violation-cost")).thenReturn(finalLoopflowViolationCost);
    }

    @Test
    void testLogOptimizationSummary1() {
        setupObjectiveFunctionResultsWithoutVirtualCost(
            -200., -210.3, 10.3, 10.3,
            -100., -150., 50., 42.2, 7.8);
        when(initialObjectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("sensi-fallback-cost"));

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;

        OptimizationSummaryReports.reportOptimizationSummary(reportNode, preventive, networkActions, rangeActions, initialObjectiveFunctionResult, objectiveFunctionResult);

        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        final String message = "Scenario \"preventive\": initial cost = -200.0 (functional: -210.3, virtual: 10.3 {sensi-fallback-cost=10.3})," +
            " 1 network action(s) and 2 range action(s) activated : Open_fake_RA and PST_2: 4, PST_1: -2," +
            " cost after preventive optimization = -100.0 (functional: -150.0, virtual: 50.0 {mnec-violation-cost=42.2, loopflow-violation-cost=7.8})";
        assertEquals(message, infoReports.getFirst().getMessage());
        assertEquals("[INFO] " + message, businessLogs.getFirst().toString());

    }

    private void setupObjectiveFunctionResultsWithoutVirtualCost() {
        setupObjectiveFunctionResultsWithoutVirtualCost(
            -200., -200., 0., 0.,
            -100., -100., 0., 0., 0.);
    }

    @Test
    void testLogOptimizationSummaryCurative() {
        setupObjectiveFunctionResultsWithoutVirtualCost();

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;

        OptimizationSummaryReports.reportOptimizationSummary(reportNode, curative, Collections.emptySet(), rangeActions, initialObjectiveFunctionResult, objectiveFunctionResult);

        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        final String message = "Scenario \"contingency\": initial cost = -200.0 (functional: -200.0, virtual: 0.0)," +
            " 2 range action(s) activated : PST_2: 4, PST_1: -2, cost after curative optimization = -100.0 (functional: -100.0, virtual: 0.0)";
        assertEquals(message, infoReports.getFirst().getMessage());
        assertEquals("[INFO] " + message, businessLogs.getFirst().toString());
    }

    @Test
    void testLogOptimizationSummary3() {
        setupObjectiveFunctionResultsWithoutVirtualCost();

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;

        OptimizationSummaryReports.reportOptimizationSummary(reportNode, preventive, Collections.emptySet(), Collections.emptyMap(), initialObjectiveFunctionResult, objectiveFunctionResult);

        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        final String message = "Scenario \"preventive\": initial cost = -200.0 (functional: -200.0, virtual: 0.0)," +
            " no remedial actions activated, cost after preventive optimization = -100.0 (functional: -100.0, virtual: 0.0)";
        assertEquals(message, infoReports.getFirst().getMessage());
        assertEquals("[INFO] " + message, businessLogs.getFirst().toString());
    }

    @Test
    void testLogOptimizationSummary4() {
        setupObjectiveFunctionResultsWithoutVirtualCost();

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;

        OptimizationSummaryReports.reportOptimizationSummary(reportNode, preventive, networkActions, Collections.emptyMap(), initialObjectiveFunctionResult, objectiveFunctionResult);

        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        final String message = "Scenario \"preventive\": initial cost = -200.0 (functional: -200.0, virtual: 0.0)," +
            " 1 network action(s) activated : Open_fake_RA, cost after preventive optimization = -100.0 (functional: -100.0, virtual: 0.0)";
        assertEquals(message, infoReports.getFirst().getMessage());
        assertEquals("[INFO] " + message, businessLogs.getFirst().toString());
    }

    @Test
    void testLogOptimizationSummary5() {
        setupObjectiveFunctionResultsWithoutVirtualCost();

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;

        OptimizationSummaryReports.reportOptimizationSummary(reportNode, preventive, Collections.emptySet(), Collections.emptyMap(), null, objectiveFunctionResult);

        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        final String message = "Scenario \"preventive\":" +
            " no remedial actions activated, cost after preventive optimization = -100.0 (functional: -100.0, virtual: 0.0)";
        assertEquals(message, infoReports.getFirst().getMessage());
        assertEquals("[INFO] " + message, businessLogs.getFirst().toString());
    }

    @Test
    void testLogOptimizationSummaryThrowsException() {
        assertThrows(
            java.lang.NullPointerException.class,
            () -> OptimizationSummaryReports.reportOptimizationSummary(reportNode, preventive, Collections.emptySet(), Collections.emptyMap(), initialObjectiveFunctionResult, null));
    }
}
