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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

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
    State stateCo1Outage;
    State stateCo1Curative;
    State stateCo2Curative;

    @Before
    public void setUp() {
        objectiveFunctionResult = mock(ObjectiveFunctionResult.class);
        flowResult = mock(FlowResult.class);
        statePreventive = mockState("preventive");
        stateCo1Outage = mockState("co1 - outage");
        stateCo1Curative = mockState("co1 - curative");
        stateCo2Curative = mockState("co2 - curative");

        cnec1 = mockCnec("ne1", stateCo1Curative, -10, 30, 10, 20, 0.1);
        cnec2 = mockCnec("ne2", statePreventive, 0, 20, -10, 30, 0.2);
        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 10, 20, 0, 0.3);
        cnec4 = mockCnec("ne4", stateCo1Curative, 20, 0, 30, -10, 0.4);
        cnec5 = mockCnec("ne5", stateCo1Outage, 30, -10, 0, 10, 0.5);
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
    public void testGetMostLimitingElementsResultsOnAllStates() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(5)).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        List<String> summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals("Limiting element #1: element ne1 at state co1 - curative with a margin of -10,00 MW", summary.get(0));
        assertEquals("Limiting element #2: element ne2 at state preventive with a margin of 0,00 MW", summary.get(1));
        assertEquals("Limiting element #3: element ne3 at state co2 - curative with a margin of 10,00 MW", summary.get(2));
        assertEquals("Limiting element #4: element ne4 at state co1 - curative with a margin of 20,00 MW", summary.get(3));
        assertEquals("Limiting element #5: element ne5 at state co1 - outage with a margin of 30,00 MW", summary.get(4));

        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(5)).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        summary = SearchTreeRaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals("Limiting element #1: element ne5 at state co1 - outage with a margin of -10,00 MW", summary.get(0));
        assertEquals("Limiting element #2: element ne4 at state co1 - curative with a margin of 0,00 MW", summary.get(1));
        assertEquals("Limiting element #3: element ne3 at state co2 - curative with a relative margin of 10,00 MW (PTDF 0,300000)", summary.get(2));
        assertEquals("Limiting element #4: element ne2 at state preventive with a relative margin of 20,00 MW (PTDF 0,200000)", summary.get(3));
        assertEquals("Limiting element #5: element ne1 at state co1 - curative with a relative margin of 30,00 MW (PTDF 0,100000)", summary.get(4));
    }
}
