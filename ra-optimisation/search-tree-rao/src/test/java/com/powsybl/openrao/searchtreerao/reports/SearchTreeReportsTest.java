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
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class SearchTreeReportsTest {
    private ReportNode reportNode;
    private Leaf leaf;
    private OptimizationPerimeter optimizationPerimeter;

    @BeforeEach
    void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        leaf = Mockito.mock(Leaf.class);
        optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);

        final State state1 = Mockito.mock(State.class);
        final State state2 = Mockito.mock(State.class);
        final RangeAction rangeAction1 = mock(RangeAction.class);
        final RangeAction rangeAction2 = mock(RangeAction.class);
        when(optimizationPerimeter.getRangeActionOptimizationStates()).thenReturn(Set.of(state1, state2));
        when(leaf.getActivatedRangeActions(state1)).thenReturn(Set.of(rangeAction1));
        when(leaf.getActivatedRangeActions(state2)).thenReturn(Set.of(rangeAction2));
        when(leaf.getSetPointVariation(rangeAction1, state1)).thenReturn(11.);
        when(leaf.getOptimizedSetpoint(rangeAction1, state1)).thenReturn(12.);
        when(leaf.getSetPointVariation(rangeAction2, state2)).thenReturn(21.);
        when(leaf.getOptimizedSetpoint(rangeAction2, state2)).thenReturn(22.);
        when(rangeAction1.getTotalCostForVariation(anyDouble())).thenReturn(100.);
        when(rangeAction2.getTotalCostForVariation(anyDouble())).thenReturn(200.);
        when(rangeAction1.getName()).thenReturn("RA1");
        when(rangeAction2.getName()).thenReturn("RA2");
    }

    @Test
    void testLogRangeActions() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        SearchTreeReports.reportRangeActions(reportNode, leaf, optimizationPerimeter);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        Assertions.assertThat(traceReports.getFirst().getMessage())
            .startsWith("Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
        Assertions.assertThat(technicalLogs.getFirst().toString())
            .startsWith("[INFO] Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
    }

    @Test
    void testLogBestLeafRangeActions() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        SearchTreeReports.reportBestLeafRangeActions(reportNode, leaf, optimizationPerimeter);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        Assertions.assertThat(traceReports.getFirst().getMessage())
            .startsWith("Best leaf: Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
        Assertions.assertThat(technicalLogs.getFirst().toString())
            .startsWith("[INFO] Best leaf: Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
    }

    @Test
    void testLogSearchDepthBestLeafRangeActions() {
        final List<ILoggingEvent> technicalLogs = ReportsTestUtils.getTechnicalLogs().list;

        SearchTreeReports.reportSearchDepthBestLeafRangeActions(reportNode, 2, leaf, optimizationPerimeter);

        final List<ReportNode> traceReports = ReportsTestUtils.getReportsWithSeverity(reportNode, TypedValue.TRACE_SEVERITY);
        Assertions.assertThat(traceReports.getFirst().getMessage())
            .startsWith("Search depth 2 best leaf: Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
        Assertions.assertThat(technicalLogs.getFirst().toString())
            .startsWith("[INFO] Search depth 2 best leaf: Range action(s): ")
            .containsAnyOf(
                "RA2: 22 (var: 21, cost 200), RA1: 12 (var: 11, cost 100)",
                "RA1: 12 (var: 11, cost 100), RA2: 22 (var: 21, cost 200)"
            );
    }
}
