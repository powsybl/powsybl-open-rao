/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.state_tree.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.state_tree.ContingencyScenario;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SearchTreeRaoLoggerTest {

    ObjectiveFunctionResult objectiveFunctionResult;
    FlowResult flowResult;
    FlowCnec cnec1;
    FlowCnec cnec2;
    FlowCnec cnec3;
    FlowCnec cnec4;
    FlowCnec cnec5;
    State statePreventive;
    State stateCo1Auto;
    State stateCo1Curative;
    State stateCo2Curative;
    String cnec1Id;
    String cnec2Id;
    String cnec3Id;
    String cnec4Id;
    String cnec5Id;
    OptimizationResult basecaseOptimResult;

    @Before
    public void setUp() {
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        flowResult = mock(FlowResult.class);
        basecaseOptimResult = mock(OptimizationResult.class);
        statePreventive = mockState("preventive", Instant.PREVENTIVE);
        stateCo1Auto = mockState("co1 - auto", Instant.AUTO);
        stateCo1Curative = mockState("co1 - curative", Instant.CURATIVE);
        stateCo2Curative = mockState("co2 - curative", Instant.CURATIVE);

        cnec1 = mockCnec("ne1", stateCo1Curative, -10, -10, 30, 300, 0.1);
        cnec1Id = "ne1 at state co1 - curative";

        cnec2 = mockCnec("ne2", statePreventive, 0, 0, -10, -10, 0.2);
        cnec2Id = "ne2 at state preventive";

        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 100, 10, 200, 0.3);
        cnec3Id = "ne3 at state co2 - curative";

        cnec4 = mockCnec("ne4", stateCo1Auto, 20, 200, 0, 0, 0.4);
        cnec4Id = "ne4 at state co1 - auto";

        cnec5 = mockCnec("ne5", stateCo1Curative, 30, 300, 20, 100, 0.5);
        cnec5Id = "ne5 at state co1 - curative";
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
        mockCnecFlowResult(flowResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        mockCnecFlowResult(basecaseOptimResult, cnec, marginMw, relMarginMw, marginA, relMarginA, ptdf);
        return cnec;
    }

    private void mockCnecFlowResult(FlowResult flowResult, FlowCnec cnec, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec)).thenReturn(ptdf);
    }

    @Test
    public void testGetSummaryFromObjFunctionResultOnAllStates() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        List<String> summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec1Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec2Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 3, cnec3Id, 10.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 4, cnec4Id, 20.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 5, cnec5Id, 30.), summary.get(4));

        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec1Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec2Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 3, cnec3Id, 100., .3), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 4, cnec4Id, 200., .4), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 5, cnec5Id, 300., .5), summary.get(4));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec2Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec4Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 3, cnec3Id, 10.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 4, cnec5Id, 20.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 5, cnec1Id, 30.), summary.get(4));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec2Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec4Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 3, cnec5Id, 100., .5), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 4, cnec3Id, 200., .3), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 5, cnec1Id, 300., .1), summary.get(4));
    }

    @Test
    public void testGetSummaryFromObjFunctionResultOnSomeStates() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        List<String> summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(0, summary.size());
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(1, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec2Id, 0.), summary.get(0));

        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(3, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec1Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec2Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 3, cnec5Id, 300., .5), summary.get(2));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(1, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec3Id, 10.), summary.get(0));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative, stateCo1Auto), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(2, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec4Id, 0.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 2, cnec3Id, 200., .3), summary.get(1));
    }

    @Test
    public void testGetSummaryFromScenarios() {
        Contingency contingency2 = mock(Contingency.class);
        when(stateCo2Curative.getContingency()).thenReturn(Optional.of(contingency2));

        Contingency contingency1 = mock(Contingency.class);
        when(stateCo1Auto.getContingency()).thenReturn(Optional.of(contingency1));
        when(stateCo1Curative.getContingency()).thenReturn(Optional.of(contingency1));

        BasecaseScenario basecaseScenario = new BasecaseScenario(statePreventive, Set.of(stateCo2Curative));
        Set<ContingencyScenario> contingencyScenarios = Set.of(new ContingencyScenario(stateCo1Auto.getContingency().get(), stateCo1Auto, stateCo1Curative));

        OptimizationResult co1AutoOptimResult = mock(OptimizationResult.class);
        OptimizationResult co1CurativeOptimResult = mock(OptimizationResult.class);
        Map<State, OptimizationResult> contingencyOptimizationResults = Map.of(stateCo1Auto, co1AutoOptimResult, stateCo1Curative, co1CurativeOptimResult);

        mockCnecFlowResult(co1AutoOptimResult, cnec1, 25, 40, 15, 11, .1);
        mockCnecFlowResult(co1AutoOptimResult, cnec4, 35, 50, -21, -21, .4);
        mockCnecFlowResult(co1AutoOptimResult, cnec5, -45, -45, 10, 12, .5);

        mockCnecFlowResult(co1CurativeOptimResult, cnec1, 2, 1, -8, -8, .1);
        mockCnecFlowResult(co1CurativeOptimResult, cnec5, -8, -8, 12, 100, .5);

        // Absolute MW
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        List<String> summary = SearchTreeRaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec5Id, -8.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec2Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 3, cnec1Id, 2.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 4, cnec3Id, 10.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 5, cnec4Id, 35.), summary.get(4));

        // Relative MW
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec5Id, -8.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec2Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW", 3, cnec1Id, 1.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW", 4, cnec4Id, 50.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW", 5, cnec3Id, 100.), summary.get(4));

        // Absolute A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec5, cnec1));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec4Id, -21.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec2Id, -10.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 3, cnec1Id, -8.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 4, cnec3Id, 10.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 5, cnec5Id, 12.), summary.get(4));

        // Relative A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec1, cnec5));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec4Id, -21.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec2Id, -10.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 3, cnec1Id, -8.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A", 4, cnec5Id, 100.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A", 5, cnec3Id, 200.), summary.get(4));
    }
}
