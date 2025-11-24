/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.parameters.TreeParameters;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class VirtualCostReportsTest {
    private FlowCnec cnec;
    private ReportNode reportNode;
    private Leaf rootLeaf;
    private SearchTreeInput searchTreeInput;
    private SearchTreeParameters searchTreeParameters;
    private TreeParameters treeParameters;
    private List<ILoggingEvent> technicalLogs;
    private List<ILoggingEvent> businessLogs;

    @BeforeEach
    void setUp() {
        reportNode = ReportsTestUtils.getTestRootNode();
        technicalLogs = ReportsTestUtils.getTechnicalLogs().list;
        businessLogs = ReportsTestUtils.getBusinessLogs().list;
        setSearchTreeParameters();
        setSearchTreeInput();
        setFlowCnec();
        setRootLeaf();
    }

    private void setSearchTreeParameters() {
        treeParameters = Mockito.mock(TreeParameters.class);
        when(treeParameters.maximumSearchDepth()).thenReturn(1);
        when(treeParameters.leavesInParallel()).thenReturn(1);
        searchTreeParameters = Mockito.mock(SearchTreeParameters.class);
        when(searchTreeParameters.getObjectiveFunction()).thenReturn(ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN);
        when(searchTreeParameters.getTreeParameters()).thenReturn(treeParameters);
    }

    private void setSearchTreeInput() {
        rootLeaf = Mockito.mock(Leaf.class);
        searchTreeInput = Mockito.mock(SearchTreeInput.class);
        final OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        when(searchTreeInput.getOptimizationPerimeter()).thenReturn(optimizationPerimeter);
    }

    private void setFlowCnec() {
        final State state = Mockito.mock(State.class);
        when(state.getId()).thenReturn("state-id");
        final NetworkElement networkElement = Mockito.mock(NetworkElement.class);
        when(networkElement.getId()).thenReturn("ne-id");

        cnec = Mockito.mock(FlowCnec.class);
        when(cnec.getState()).thenReturn(state);
        when(cnec.getNetworkElement()).thenReturn(networkElement);
        when(cnec.getId()).thenReturn("cnec-id");
        when(cnec.getName()).thenReturn("cnec-name");
        when(cnec.getUpperBound(TwoSides.ONE, Unit.MEGAWATT)).thenReturn(Optional.of(1000.));
    }

    private void setRootLeaf() {
        when(rootLeaf.getCostlyElements(eq("loop-flow-cost"), anyInt())).thenReturn(List.of(cnec));
        when(rootLeaf.getIdentifier()).thenReturn("leaf-id");
        when(rootLeaf.getMargin(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(-135.);
        when(rootLeaf.getMargin(cnec, TwoSides.TWO, Unit.MEGAWATT)).thenReturn(-134.);
        when(rootLeaf.getFlow(cnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(1135.);
    }

    @Test
    void testGetCostlyElementsLogs() {
        VirtualCostReports.reportVirtualCostlyElements(reportNode, false, rootLeaf, "loop-flow-cost", Unit.MEGAWATT, true);
        final List<ReportNode> childrenReportNodes = reportNode.getChildren();
        assertEquals(1, childrenReportNodes.size());
        assertEquals(1, technicalLogs.size());
        assertEquals("Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", childrenReportNodes.getFirst().getMessage());
        assertEquals("[INFO] Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", technicalLogs.getFirst().toString());
    }

    @Test
    void testLogVirtualCostDetails() {
        when(treeParameters.stopCriterion()).thenReturn(TreeParameters.StopCriterion.AT_TARGET_OBJECTIVE_VALUE);
        when(treeParameters.targetObjectiveValue()).thenReturn(0.);
        // functional cost = -100 (secure)
        // virtual cost = 200
        // overall cost = 100 (unsecure)
        when(rootLeaf.isRoot()).thenReturn(true);
        when(rootLeaf.getCost()).thenReturn(100.);
        when(rootLeaf.getVirtualCost("loop-flow-cost")).thenReturn(200.);

        // Functional cost does not satisfy stop criterion
        VirtualCostReports.reportVirtualCostDetails(reportNode, false, rootLeaf, "loop-flow-cost", Unit.MEGAWATT, 0, searchTreeParameters, true);
        final List<ReportNode> childrenReportNodes = reportNode.getChildren();

        assertEquals(2, childrenReportNodes.size());
        assertEquals(2, businessLogs.size());
        assertEquals("Optimized leaf-id, stop criterion could have been reached without \"loop-flow-cost\" virtual cost", childrenReportNodes.get(0).getMessage());
        assertEquals("[INFO] Optimized leaf-id, stop criterion could have been reached without \"loop-flow-cost\" virtual cost", businessLogs.get(0).toString());
        assertEquals("Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", childrenReportNodes.get(1).getMessage());
        assertEquals("[INFO] Optimized leaf-id, limiting \"loop-flow-cost\" constraint #01: flow = 1135.00 MW, threshold = 1000.00 MW, margin = -135.00 MW, element ne-id at state state-id, CNEC ID = \"cnec-id\", CNEC name = \"cnec-name\"", businessLogs.get(1).toString());
    }

    @Test
    void testLogRangeActions() {
        SearchTreeReports.reportRangeActions(reportNode, rootLeaf, searchTreeInput.getOptimizationPerimeter());
        List<ReportNode> childrenReportNodes = reportNode.getChildren();

        ReportNode lastReportNode = childrenReportNodes.getLast();
        assertEquals("No range actions activated", lastReportNode.getMessage());
        assertEquals(TypedValue.TRACE_SEVERITY, lastReportNode.getValue("reportSeverity").orElseThrow());
        assertEquals("[INFO] No range actions activated", technicalLogs.getLast().toString());

        // apply 2 range actions
        final RangeAction<?> rangeAction1 = Mockito.mock(PstRangeAction.class);
        final RangeAction<?> rangeAction2 = Mockito.mock(PstRangeAction.class);
        when(rangeAction1.getName()).thenReturn("PST1");
        when(rangeAction2.getName()).thenReturn("PST2");
        final State optimizedState = Mockito.mock(State.class);
        when(searchTreeInput.getOptimizationPerimeter().getRangeActionOptimizationStates()).thenReturn(Set.of(optimizedState));
        when(rootLeaf.getActivatedRangeActions(optimizedState)).thenReturn(Set.of(rangeAction1, rangeAction2));

        SearchTreeReports.reportRangeActions(reportNode, rootLeaf, searchTreeInput.getOptimizationPerimeter());
        childrenReportNodes = reportNode.getChildren();

        lastReportNode = childrenReportNodes.getLast();
        assertEquals(TypedValue.TRACE_SEVERITY, lastReportNode.getValue("reportSeverity").orElseThrow());
        // PST can be logged in any order
        final String lastReportNodeMessage = lastReportNode.getMessage();
        assertTrue(lastReportNodeMessage.contains("Range action(s):"));
        assertTrue(lastReportNodeMessage.contains("PST1: 0"));
        assertTrue(lastReportNodeMessage.contains("PST2: 0"));
        final String lastLogMessage = technicalLogs.getLast().toString();
        assertTrue(lastLogMessage.contains("[INFO] Range action(s):"));
        assertTrue(lastLogMessage.contains("PST1: 0"));
        assertTrue(lastLogMessage.contains("PST2: 0"));
    }
}
