/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
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
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class AutomatonSimulatorReportsTest {
    private ReportNode reportNode;
    private State curative;
    private Set<NetworkAction> networkActions;
    private Map<RangeAction<?>, Double> rangeActions;

    @BeforeEach
    public void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        curative = Mockito.mock(State.class);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getName()).thenReturn(Optional.of("contingency"));
        when(curative.getContingency()).thenReturn(Optional.of(contingency));

        // Create Remedial actions
        NetworkAction fakeRA = Mockito.mock(NetworkAction.class);
        when(fakeRA.getName()).thenReturn("Open_fake_RA");
        networkActions = Set.of(fakeRA);
        rangeActions = new HashMap<>();
        RangeAction<?> fakePST1 = Mockito.mock(RangeAction.class);
        RangeAction<?> fakePST2 = Mockito.mock(RangeAction.class);
        when(fakePST1.getName()).thenReturn("PST_1");
        when(fakePST2.getName()).thenReturn("PST_2");
        rangeActions.put(fakePST1, -2.);
        rangeActions.put(fakePST2, 4.);

    }

    @Test
    void testLogFailedOptimizationSummaryPreventive() {
        final State preventive = Mockito.mock(State.class);

        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
        AutomatonSimulatorReports.reportFailedOptimizationSummary(reportNode, preventive, Collections.emptySet(), Collections.emptyMap());
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Scenario \"preventive\": no remedial actions activated", infoReports.getFirst().getMessage());
        assertEquals("[INFO] Scenario \"preventive\": no remedial actions activated", businessLogs.getFirst().toString());
    }

    @Test
    void testLogFailedOptimizationSummaryCurativeWithNetworkActions() {
        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
        AutomatonSimulatorReports.reportFailedOptimizationSummary(reportNode, curative, networkActions, Collections.emptyMap());
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Scenario \"contingency\": 1 network action(s) activated : Open_fake_RA", infoReports.getFirst().getMessage());
        assertEquals("[INFO] Scenario \"contingency\": 1 network action(s) activated : Open_fake_RA", businessLogs.getFirst().toString());
    }

    @Test
    void testLogFailedOptimizationSummaryCurativeWithRangeActions() {
        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
        AutomatonSimulatorReports.reportFailedOptimizationSummary(reportNode, curative, Collections.emptySet(), rangeActions);
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Scenario \"contingency\": 2 range action(s) activated : PST_2: 4, PST_1: -2", infoReports.getFirst().getMessage());
        assertEquals("[INFO] Scenario \"contingency\": 2 range action(s) activated : PST_2: 4, PST_1: -2", businessLogs.getFirst().toString());
    }

    @Test
    void testLogFailedOptimizationSummaryCurativeWithNetworkActionsAndRangeActions() {
        final List<ILoggingEvent> businessLogs = ReportsTestUtils.getBusinessLogs().list;
        AutomatonSimulatorReports.reportFailedOptimizationSummary(reportNode, curative, networkActions, rangeActions);
        final List<ReportNode> infoReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.INFO_SEVERITY);
        assertEquals("Scenario \"contingency\": 1 network action(s) and 2 range action(s) activated : Open_fake_RA and PST_2: 4, PST_1: -2", infoReports.getFirst().getMessage());
        assertEquals("[INFO] Scenario \"contingency\": 1 network action(s) and 2 range action(s) activated : Open_fake_RA and PST_2: 4, PST_1: -2", businessLogs.getFirst().toString());
    }
}
