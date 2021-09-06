/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
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

    @Before
    public void setUp() {
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        flowResult = mock(FlowResult.class);
        statePreventive = mockState("preventive");
        stateCo1Auto = mockState("co1 - auto");
        stateCo1Curative = mockState("co1 - curative");
        stateCo2Curative = mockState("co2 - curative");

        cnec1 = mockCnec("ne1", stateCo1Curative, -10, 30, 10, 20, 0.1);
        cnec1Id = "ne1 at state co1 - curative";

        cnec2 = mockCnec("ne2", statePreventive, 0, 20, -10, 30, 0.2);
        cnec2Id = "ne2 at state preventive";

        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 10, 20, 0, 0.3);
        cnec3Id = "ne3 at state co2 - curative";

        cnec4 = mockCnec("ne4", stateCo1Auto, 20, 0, 30, -10, 0.4);
        cnec4Id = "ne4 at state co1 - auto";

        cnec5 = mockCnec("ne5", stateCo1Curative, 30, -10, 0, 10, 0.5);
        cnec5Id = "ne5 at state co1 - curative";
    }

    private State mockState(String stateId) {
        State state = mock(State.class);
        when(state.getId()).thenReturn(stateId);
        return state;
    }

    private FlowCnec mockCnec(String neName, State state, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        NetworkElement ne = mock(NetworkElement.class);
        when(ne.getName()).thenReturn(neName);
        FlowCnec cnec = mock(FlowCnec.class);
        when(cnec.getNetworkElement()).thenReturn(ne);
        when(cnec.getState()).thenReturn(state);
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec)).thenReturn(ptdf);
        return cnec;
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
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec5Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 2, cnec4Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 3, cnec3Id, 10., .3), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 4, cnec2Id, 20., .2), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 5, cnec1Id, 30., .1), summary.get(4));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec2Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec5Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 3, cnec1Id, 10.), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 4, cnec3Id, 20.), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 5, cnec4Id, 30.), summary.get(4));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec4Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec3Id, 0.), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 3, cnec5Id, 10., .5), summary.get(2));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 4, cnec1Id, 20., .1), summary.get(3));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f A (PTDF %f)", 5, cnec2Id, 30., .2), summary.get(4));
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
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(3, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f MW", 1, cnec5Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 2, cnec2Id, 20., .2), summary.get(1));
        assertEquals(format("Limiting element #%d: element %s with a relative margin of %.2f MW (PTDF %f)", 3, cnec1Id, 30., .1), summary.get(2));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(1, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec3Id, 20.), summary.get(0));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative, stateCo1Auto), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(2, summary.size());
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 1, cnec4Id, -10.), summary.get(0));
        assertEquals(format("Limiting element #%d: element %s with a margin of %.2f A", 2, cnec3Id, 0.), summary.get(1));
    }
}
