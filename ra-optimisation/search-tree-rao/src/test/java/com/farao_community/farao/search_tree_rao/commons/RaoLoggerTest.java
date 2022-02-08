/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLogger;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.commons.logs.RaoBusinessLogs;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.ObjectiveFunctionResult;
import com.farao_community.farao.search_tree_rao.result.api.OptimizationResult;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.castor.algorithm.ContingencyScenario;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static com.farao_community.farao.commons.Unit.AMPERE;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class RaoLoggerTest {

    private ObjectiveFunctionResult objectiveFunctionResult;
    private FlowResult flowResult;
    private FlowCnec cnec1;
    private FlowCnec cnec2;
    private FlowCnec cnec3;
    private FlowCnec cnec4;
    private FlowCnec cnec5;
    private State statePreventive;
    private State stateCo1Auto;
    private State stateCo1Curative;
    private State stateCo2Curative;
    private OptimizationResult basecaseOptimResult;

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
        cnec2 = mockCnec("ne2", statePreventive, 0, 0, -10, -10, 0.2);
        cnec3 = mockCnec("ne3", stateCo2Curative, 10, 100, 10, 200, 0.3);
        cnec4 = mockCnec("ne4", stateCo1Auto, 20, 200, 0, 0, 0.4);
        cnec5 = mockCnec("ne5", stateCo1Curative, 30, 300, 20, 100, 0.5);
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
        return cnec;
    }

    private void mockCnecFlowResult(FlowResult flowResult, FlowCnec cnec, double marginMw, double relMarginMw, double marginA, double relMarginA, double ptdf) {
        when(flowResult.getMargin(cnec, Unit.MEGAWATT)).thenReturn(marginMw);
        when(flowResult.getRelativeMargin(cnec, Unit.MEGAWATT)).thenReturn(relMarginMw);
        when(flowResult.getMargin(cnec, Unit.AMPERE)).thenReturn(marginA);
        when(flowResult.getRelativeMargin(cnec, Unit.AMPERE)).thenReturn(relMarginA);
        when(flowResult.getPtdfZonalSum(cnec)).thenReturn(ptdf);
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
        return format(Locale.ENGLISH, "Limiting element #%d:%s margin = %.2f %s%s, element %s, CNEC ID = \"%s\"", order, relativeMargin, margin, unit, ptdfString, descriptor, cnec.getId());
    }

    @Test
    public void testGetSummaryFromObjFunctionResultOnAllStates() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        List<String> summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), summary.get(1));
        assertEquals(absoluteMarginLog(3, 10, MEGAWATT, cnec3), summary.get(2));
        assertEquals(absoluteMarginLog(4, 20, MEGAWATT, cnec4), summary.get(3));
        assertEquals(absoluteMarginLog(5, 30, MEGAWATT, cnec5), summary.get(4));

        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), summary.get(1));
        assertEquals(relativeMarginLog(3, 100, .3, MEGAWATT, cnec3), summary.get(2));
        assertEquals(relativeMarginLog(4, 200, .4, MEGAWATT, cnec4), summary.get(3));
        assertEquals(relativeMarginLog(5, 300, .5, MEGAWATT, cnec5), summary.get(4));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, null, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), summary.get(1));
        assertEquals(absoluteMarginLog(3, 10, AMPERE, cnec3), summary.get(2));
        assertEquals(absoluteMarginLog(4, 20, AMPERE, cnec5), summary.get(3));
        assertEquals(absoluteMarginLog(5, 30, AMPERE, cnec1), summary.get(4));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Auto, stateCo1Curative, stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -10, AMPERE, cnec2), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, AMPERE, cnec4), summary.get(1));
        assertEquals(relativeMarginLog(3, 100, .5, AMPERE, cnec5), summary.get(2));
        assertEquals(relativeMarginLog(4, 200, .3, AMPERE, cnec3), summary.get(3));
        assertEquals(relativeMarginLog(5, 300, .1, AMPERE, cnec1), summary.get(4));
    }

    @Test
    public void testGetSummaryFromObjFunctionResultOnSomeStates() {
        // Absolute MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        List<String> summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(0, summary.size());
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(1, summary.size());
        assertEquals(absoluteMarginLog(1, 0, MEGAWATT, cnec2), summary.get(0));

        // Relative MW
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec2, cnec3, cnec4, cnec5));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(statePreventive, stateCo1Curative), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(3, summary.size());
        assertEquals(absoluteMarginLog(1, -10, MEGAWATT, cnec1), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), summary.get(1));
        assertEquals(relativeMarginLog(3, 300, .5, MEGAWATT, cnec5), summary.get(2));

        // Absolute A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec3, cnec5, cnec1));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative), RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(1, summary.size());
        assertEquals(absoluteMarginLog(1, 10, AMPERE, cnec3), summary.get(0));

        // Relative A
        when(objectiveFunctionResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec4, cnec5, cnec3, cnec1));
        summary = RaoLogger.getMostLimitingElementsResults(objectiveFunctionResult, flowResult, Set.of(stateCo2Curative, stateCo1Auto), RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(2, summary.size());
        assertEquals(absoluteMarginLog(1, 0, AMPERE, cnec4), summary.get(0));
        assertEquals(relativeMarginLog(2, 200, .3, AMPERE, cnec3), summary.get(1));
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
        List<String> summary = RaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), summary.get(1));
        assertEquals(absoluteMarginLog(3, 2, MEGAWATT, cnec1), summary.get(2));
        assertEquals(absoluteMarginLog(4, 10, MEGAWATT, cnec3), summary.get(3));
        assertEquals(absoluteMarginLog(5, 35, MEGAWATT, cnec4), summary.get(4));

        // Relative MW
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec4, cnec3, cnec2, cnec1));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1, cnec4));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec5, cnec1));
        summary = RaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -8, MEGAWATT, cnec5), summary.get(0));
        assertEquals(absoluteMarginLog(2, 0, MEGAWATT, cnec2), summary.get(1));
        assertEquals(relativeMarginLog(3, 1, null, MEGAWATT, cnec1), summary.get(2));
        assertEquals(relativeMarginLog(4, 50, null, MEGAWATT, cnec4), summary.get(3));
        assertEquals(relativeMarginLog(5, 100, null, MEGAWATT, cnec3), summary.get(4));

        // Absolute A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec2, cnec5, cnec1, cnec3, cnec4));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec5, cnec1));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        summary = RaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), summary.get(0));
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), summary.get(1));
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), summary.get(2));
        assertEquals(absoluteMarginLog(4, 10, AMPERE, cnec3), summary.get(3));
        assertEquals(absoluteMarginLog(5, 12, AMPERE, cnec5), summary.get(4));

        // Relative A
        when(basecaseOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec3, cnec5, cnec1, cnec2));
        when(co1AutoOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec4, cnec1, cnec5));
        when(co1CurativeOptimResult.getMostLimitingElements(anyInt())).thenReturn(List.of(cnec1, cnec5));
        summary = RaoLogger.getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, RaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE, 5);
        assertEquals(5, summary.size());
        assertEquals(absoluteMarginLog(1, -21, AMPERE, cnec4), summary.get(0));
        assertEquals(absoluteMarginLog(2, -10, AMPERE, cnec2), summary.get(1));
        assertEquals(absoluteMarginLog(3, -8, AMPERE, cnec1), summary.get(2));
        assertEquals(relativeMarginLog(4, 100, null, AMPERE, cnec5), summary.get(3));
        assertEquals(relativeMarginLog(5, 200, null, AMPERE, cnec3), summary.get(4));
    }

    @Test
    public void testFormatDouble() {
        assertEquals("10.00", RaoLogger.formatDouble(10.));
        assertEquals("-53.63", RaoLogger.formatDouble(-53.634));
        assertEquals("-53.64", RaoLogger.formatDouble(-53.635));
        assertEquals("-infinity", RaoLogger.formatDouble(-Double.MAX_VALUE));
        assertEquals("+infinity", RaoLogger.formatDouble(Double.MAX_VALUE));
        assertEquals("-infinity", RaoLogger.formatDouble(-179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00));
        assertEquals("+infinity", RaoLogger.formatDouble(179769313486231570000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.00));
    }

    private ListAppender<ILoggingEvent> registerLogs(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    @Test
    public void testLogOptimizationSummary() {
        State preventive = Mockito.mock(State.class);
        when(preventive.getInstant()).thenReturn(Instant.PREVENTIVE);

        State curative = Mockito.mock(State.class);
        when(curative.getInstant()).thenReturn(Instant.CURATIVE);
        Contingency contingency = Mockito.mock(Contingency.class);
        when(contingency.getName()).thenReturn("contingency");
        when(curative.getContingency()).thenReturn(Optional.of(contingency));

        when(objectiveFunctionResult.getCost()).thenReturn(-100.);
        when(objectiveFunctionResult.getFunctionalCost()).thenReturn(-150.);
        when(objectiveFunctionResult.getVirtualCost()).thenReturn(50.);

        FaraoLogger logger = FaraoLoggerProvider.BUSINESS_LOGS;
        List<ILoggingEvent> logsList = registerLogs(RaoBusinessLogs.class).list;

        RaoLogger.logOptimizationSummary(logger, preventive, 0, 0, 1., 2., objectiveFunctionResult);
        assertEquals("[INFO] Scenario \"preventive\": initial cost = 3.00 (functional: 1.00, virtual: 2.00), no preventive remedial actions activated, cost after PRA = -100.00 (functional: -150.00, virtual: 50.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(logger, curative, 1, 0, -100., 40., objectiveFunctionResult);
        assertEquals("[INFO] Scenario \"contingency\": initial cost = -60.00 (functional: -100.00, virtual: 40.00), 1 curative network action(s) activated, cost after CRA = -100.00 (functional: -150.00, virtual: 50.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(logger, curative, 0, 2, 1., null, objectiveFunctionResult);
        assertEquals("[INFO] Scenario \"contingency\": 2 curative range action(s) activated, cost after CRA = -100.00 (functional: -150.00, virtual: 50.00)", logsList.get(logsList.size() - 1).toString());

        RaoLogger.logOptimizationSummary(logger, curative, 3, 2, null, 200., objectiveFunctionResult);
        assertEquals("[INFO] Scenario \"contingency\": 3 curative network action(s) and 2 curative range action(s) activated, cost after CRA = -100.00 (functional: -150.00, virtual: 50.00)", logsList.get(logsList.size() - 1).toString());
    }
}
