/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.CracImpl;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.castor.algorithm.StateTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.openrao.commons.Unit.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PreventiveAndCurativesRaoResultImplTest {
    private static final Map<InstantKind, Double> FLOW_PER_INSTANT = Map.of(InstantKind.OUTAGE, 10d, InstantKind.AUTO, 20d, InstantKind.CURATIVE, 30d);
    private static final Map<InstantKind, Double> FLOW_PER_OPTIMIZED_INSTANT = Map.of(InstantKind.PREVENTIVE, 100d, InstantKind.AUTO, 200d, InstantKind.CURATIVE, 300d);

    private static final double DOUBLE_TOLERANCE = 1e-3;
    private Crac crac;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;
    private List<Set<Instant>> optimizationInstantsPerContingency;

    private PrePerimeterResult initialResult;
    private OptimizationResult prevResult;
    private PrePerimeterResult postPrevPrePerimResult;
    private PostPerimeterResult postPrevResult;
    private OptimizationResult autoResult3;
    private PrePerimeterResult postAutoPrePerimResult3;
    private PostPerimeterResult postAutoResult3;
    private OptimizationResult autoResult4;
    private PrePerimeterResult postAutoPrePerimResult4;
    private PostPerimeterResult postAutoResult4;
    private OptimizationResult curativeResult2;
    private PrePerimeterResult postCurativePrePerimResult2;
    private PostPerimeterResult postCurativeResult2;
    private OptimizationResult curativeResult3;
    private PrePerimeterResult postCurativePrePerimResult3;
    private PostPerimeterResult postCurativeResult3;
    private Map<State, PostPerimeterResult> postContingencyResults = new HashMap<>();

    private PreventiveAndCurativesRaoResultImpl output;

    private void initCrac() {
        crac = new CracImpl("crac");
        // Instants
        crac.newInstant("preventive", InstantKind.PREVENTIVE)
            .newInstant("outage", InstantKind.OUTAGE)
            .newInstant("auto", InstantKind.AUTO)
            .newInstant("curative", InstantKind.CURATIVE);
        for (int i = 1; i <= 4; i++) {
            crac.newContingency()
                .withId("contingency-" + i)
                .withName("CO" + i)
                .withContingencyElement("element-" + i, ContingencyElementType.LINE)
                .add();
            crac.newFlowCnec()
                .withId("cnec-" + i + "out")
                .withInstant("outage")
                .withContingency("contingency-" + i)
                .withNetworkElement("line-" + i)
                .withOptimized(true)
                .withMonitored(false)
                .newThreshold()
                .withSide(ONE)
                .withUnit(MEGAWATT)
                .withMin(0d) // so margin = flow for simplicity
                .add()
                .add();
            crac.newFlowCnec()
                .withId("cnec-" + i + "auto")
                .withInstant("auto")
                .withContingency("contingency-" + i)
                .withNetworkElement("line-" + i)
                .withOptimized(true)
                .withMonitored(false)
                .newThreshold()
                .withSide(ONE)
                .withUnit(MEGAWATT)
                .withMin(0d) // so margin = flow for simplicity
                .add()
                .add();
            crac.newFlowCnec()
                .withId("cnec-" + i + "cur")
                .withInstant("curative")
                .withContingency("contingency-" + i)
                .withNetworkElement("line-" + i)
                .withOptimized(true)
                .withMonitored(false)
                .newThreshold()
                .withSide(ONE)
                .withUnit(MEGAWATT)
                .withMin(0d) // so margin = flow for simplicity
                .add()
                .add();
        }
        crac.newPstRangeAction()
            .withId("pst")
            .withNetworkElement("pst-elt")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
            .newTapRange()
            .withMinTap(-1)
            .withMaxTap(1)
            .withRangeType(RangeType.ABSOLUTE)
            .add()
            .newOnInstantUsageRule()
            .withInstant("preventive")
            .add()
            .add();
    }

    @BeforeEach
    public void setUp() {
        initCrac();
        preventiveInstant = crac.getInstant("preventive");
        autoInstant = crac.getInstant("auto");
        curativeInstant = crac.getInstant("curative");

        /**
         * Optimized instants:
         * -----------------------------------------------------
         * |                   * PREVENTIVE *                  |
         * -     --     -     --     ---------------------------
         * |   AUTO 1   |   AUTO 2   | * AUTO 3 * | * AUTO 4 * |
         * -     --     ---------------------------     --     -
         * |   CURA 1   | * CURA 2 * | * CURA 3 * |   CURA 4   |
         * -----------------------------------------------------
         */

        optimizationInstantsPerContingency = List.of(
            Set.of(preventiveInstant),
            Set.of(preventiveInstant, curativeInstant),
            Set.of(preventiveInstant, autoInstant, curativeInstant),
            Set.of(preventiveInstant, autoInstant));

        // only prepare results for perimeters that were optimized (like it would be in a normal run)
        // The result class will fill in the "holes".
        prepareInitialResult();
        preparePreventiveResult();
        prepareAutoResult3();
        prepareAutoResult4();
        prepareCurativeResult2();
        prepareCurativeResult3();

        StateTree stateTree = generateStateTree();

        output = new PreventiveAndCurativesRaoResultImpl(stateTree, initialResult, postPrevResult, postContingencyResults, crac, new RaoParameters());
    }

    private void prepareInitialResult() {
        initialResult = Mockito.mock(PrePerimeterResult.class);
        crac.getFlowCnecs().forEach(cnec -> {
            double flow = -1 * (FLOW_PER_INSTANT.get(cnec.getState().getInstant().getKind()) + Double.parseDouble(cnec.getId().charAt(5) + ""));
            when(initialResult.getFlow(cnec, ONE, MEGAWATT)).thenReturn(flow);
            when(initialResult.getMargin(cnec, ONE, MEGAWATT)).thenReturn(flow);
            when(initialResult.getLoopFlow(cnec, ONE, MEGAWATT)).thenThrow(new OpenRaoException("No commercial flow"));
        });
        when(initialResult.getFunctionalCost()).thenReturn(34.); //cnec4 at curative
        when(initialResult.getVirtualCost("sensitivity-failure-cost")).thenReturn(34.1);
    }

    private void preparePreventiveResult() {
        prevResult = Mockito.mock(OptimizationResult.class);
        postPrevPrePerimResult = Mockito.mock(PrePerimeterResult.class);
        postPrevResult = new PostPerimeterResult(prevResult, postPrevPrePerimResult);
        prepareResultsForState(prevResult, postPrevPrePerimResult, crac.getPreventiveState());
    }

    private void prepareAutoResult3() {
        autoResult3 = Mockito.mock(OptimizationResult.class);
        postAutoPrePerimResult3 = Mockito.mock(PrePerimeterResult.class);
        postAutoResult3 = new PostPerimeterResult(autoResult3, postAutoPrePerimResult3);
        prepareResultsForState(autoResult3, postAutoPrePerimResult3, crac.getState("contingency-3", autoInstant));
        postContingencyResults.put(crac.getState("contingency-3", autoInstant), postAutoResult3);
    }

    private void prepareAutoResult4() {
        autoResult4 = Mockito.mock(OptimizationResult.class);
        postAutoPrePerimResult4 = Mockito.mock(PrePerimeterResult.class);
        postAutoResult4 = new PostPerimeterResult(autoResult4, postAutoPrePerimResult4);
        prepareResultsForState(autoResult4, postAutoPrePerimResult4, crac.getState("contingency-4", autoInstant));
        postContingencyResults.put(crac.getState("contingency-4", autoInstant), postAutoResult4);
    }

    private void prepareCurativeResult2() {
        curativeResult2 = Mockito.mock(OptimizationResult.class);
        postCurativePrePerimResult2 = Mockito.mock(PrePerimeterResult.class);
        postCurativeResult2 = new PostPerimeterResult(curativeResult2, postCurativePrePerimResult2);
        prepareResultsForState(curativeResult2, postCurativePrePerimResult2, crac.getState("contingency-2", curativeInstant));
        postContingencyResults.put(crac.getState("contingency-2", curativeInstant), postCurativeResult2);
    }

    private void prepareCurativeResult3() {
        curativeResult3 = Mockito.mock(OptimizationResult.class);
        postCurativePrePerimResult3 = Mockito.mock(PrePerimeterResult.class);
        postCurativeResult3 = new PostPerimeterResult(curativeResult3, postCurativePrePerimResult3);
        prepareResultsForState(curativeResult3, postCurativePrePerimResult3, crac.getState("contingency-3", curativeInstant));
        postContingencyResults.put(crac.getState("contingency-3", curativeInstant), postCurativeResult3);
    }

    private void prepareResultsForState(OptimizationResult optimizationResult, PrePerimeterResult prePerimeterResult, State state) {
        AtomicReference<Double> lowestPerimeterFlow = new AtomicReference<>(Double.MAX_VALUE);
        AtomicReference<Double> lowestPostPerimeterFlow = new AtomicReference<>(Double.MAX_VALUE);
        Instant instant = state.getInstant();
        crac.getFlowCnecs().stream()
            .filter(cnec -> instant.isPreventive() || cnec.getState().getContingency().equals(state.getContingency()))
            .forEach(cnec -> {
                double signum = shouldBeSecured(cnec, instant) ? 1 : -1;
                // flow = +/- abc with
                // +/- depends of if cnec can be optimized later
                // a depends on most recent optimization (0 for init, 1 for prev, 2 for auto, 3 for cur)
                // b depends on instant of cnec
                // c depends on contingency
                double flow = signum * (
                    FLOW_PER_OPTIMIZED_INSTANT.get(getMostRecentOptimInstant(cnec, instant).getKind()) +
                    FLOW_PER_INSTANT.get(cnec.getState().getInstant().getKind()) +
                    Double.parseDouble(cnec.getId().charAt(5) + ""));
                if (isCnecOptimizedDuringInstant(cnec, instant)) {
                    addFlowAndMarginResults(optimizationResult, cnec, flow, instant);
                    lowestPerimeterFlow.set(Math.min(lowestPerimeterFlow.get(), flow));
                }
                if (!instant.comesAfter(cnec.getState().getInstant())) {
                    addFlowAndMarginResults(prePerimeterResult, cnec, flow, instant);
                    lowestPostPerimeterFlow.set(Math.min(lowestPostPerimeterFlow.get(), flow));
                }
            });
        when(optimizationResult.getFunctionalCost()).thenReturn(-lowestPerimeterFlow.get());
        when(prePerimeterResult.getFunctionalCost()).thenReturn(-lowestPostPerimeterFlow.get());
        when(optimizationResult.getVirtualCost("sensitivity-failure-cost")).thenReturn(-lowestPerimeterFlow.get() + 0.1);
        when(prePerimeterResult.getVirtualCost("sensitivity-failure-cost")).thenReturn(-lowestPostPerimeterFlow.get() + 0.1);
        if (!state.isPreventive()) {
            Set<String> contingencies = Set.of(state.getContingency().get().getId());
            when(optimizationResult.getContingencies()).thenReturn(contingencies);
        }
        PstRangeAction pst = crac.getPstRangeAction("pst");
        when(optimizationResult.getActivatedRangeActions(state)).thenReturn(Set.of(pst));
        when(optimizationResult.getOptimizedTapsOnState(state)).thenReturn(Map.of(pst, (int) (FLOW_PER_OPTIMIZED_INSTANT.get(instant.getKind()) / 100.)));
        when(optimizationResult.getOptimizedTap(pst, state)).thenReturn((int) (FLOW_PER_OPTIMIZED_INSTANT.get(instant.getKind()) / 100.));

        Map<RangeAction<?>, Double> optimizedSetpointsOnState = new HashMap<>();
        optimizedSetpointsOnState.put(pst, FLOW_PER_OPTIMIZED_INSTANT.get(instant.getKind()));
        when(optimizationResult.getOptimizedSetpointsOnState(state)).thenReturn(optimizedSetpointsOnState);
        when(optimizationResult.getOptimizedSetpoint(pst, state)).thenReturn(FLOW_PER_OPTIMIZED_INSTANT.get(instant.getKind()));

    }

    private void addFlowAndMarginResults(FlowResult flowResult, FlowCnec cnec, double flow, Instant instant) {
        when(flowResult.getFlow(cnec, ONE, MEGAWATT)).thenReturn(flow);
        for (Instant i : crac.getSortedInstants()) {
            if (!i.comesBefore(instant)) {
                when(flowResult.getFlow(cnec, ONE, MEGAWATT, i)).thenReturn(flow);
            }
        }
        when(flowResult.getMargin(cnec, ONE, MEGAWATT)).thenReturn(flow);
        when(flowResult.getMargin(cnec, MEGAWATT)).thenReturn(flow);
    }

    private boolean isCnecOptimizedDuringInstant(FlowCnec cnec, Instant instant) {
        return shouldBeSecured(cnec, instant) && getMostRecentOptimInstant(cnec, instant).equals(instant);
    }

    private Instant getMostRecentOptimInstant(FlowCnec cnec, Instant instant) {
        return optimizationInstantsPerContingency.get(Integer.parseInt(cnec.getId().charAt(5) + "") - 1).stream()
            .filter(i -> !i.comesAfter(instant))
            .max(Instant::compareTo)
            .orElse(null);
    }

    private boolean shouldBeSecured(FlowCnec cnec, Instant instant) {
        return optimizationInstantsPerContingency.get(Integer.parseInt(cnec.getId().charAt(5) + "") - 1).stream()
            .noneMatch(i -> i.comesAfter(instant) && !i.comesAfter(cnec.getState().getInstant()));
    }

    private StateTree generateStateTree() {
        StateTree stateTree = Mockito.mock(StateTree.class);
        Set<ContingencyScenario> contingencyScenarios = new HashSet<>();
        for (int i = 1; i <= 4; i++) {
            Set<Instant> optimizationInstants = optimizationInstantsPerContingency.get(i - 1);
            ContingencyScenario contingencyScenario = Mockito.mock(ContingencyScenario.class);
            when(contingencyScenario.getContingency()).thenReturn(crac.getContingency("contingency-" + i));
            if (optimizationInstants.contains(autoInstant)) {
                when(contingencyScenario.getAutomatonState()).thenReturn(Optional.of(crac.getState("contingency-" + i, autoInstant)));
            } else {
                when(contingencyScenario.getAutomatonState()).thenReturn(Optional.empty());
            }
            if (optimizationInstants.contains(curativeInstant)) {
                Perimeter curativePerimeter = Mockito.mock(Perimeter.class);
                when(curativePerimeter.getRaOptimisationState()).thenReturn(crac.getState("contingency-" + i, curativeInstant));
            }
            contingencyScenarios.add(contingencyScenario);
        }
        when(stateTree.getContingencyScenarios()).thenReturn(contingencyScenarios);
        return stateTree;
    }

    @Test
    public void testResult() {
        checkFunctionalCosts();
        checkVirtualCosts();
        checkFlows();
        checkOptimizationResults();
        checkOptimizedPsts();
    }

    private void checkOptimizedPsts() {
        PstRangeAction pst = crac.getPstRangeAction("pst");
        Map<RangeAction<?>, Double> preventiveMap = output.getOptimizedSetPointsOnState(crac.getPreventiveState());
        assertEquals(1, preventiveMap.size());
        assertEquals(FLOW_PER_OPTIMIZED_INSTANT.get(InstantKind.PREVENTIVE), preventiveMap.get(pst));
        assertEquals(FLOW_PER_OPTIMIZED_INSTANT.get(InstantKind.PREVENTIVE), output.getOptimizedSetPointOnState(crac.getPreventiveState(), pst));
        assertEquals(1, output.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        Map<PstRangeAction, Integer> optimizedTapsPreventive = output.getOptimizedTapsOnState(crac.getPreventiveState());
        assertEquals(1, optimizedTapsPreventive.size());
        assertEquals(1, optimizedTapsPreventive.get(pst));
        assertEquals(output.getActivatedRangeActionsDuringState(crac.getPreventiveState()), Set.of(pst));

        // optimized state
        State auto3state = crac.getState("contingency-3", crac.getInstant(InstantKind.AUTO));
        Map<RangeAction<?>, Double> auto3Map = output.getOptimizedSetPointsOnState(auto3state);
        assertEquals(1, auto3Map.size());
        assertEquals(FLOW_PER_OPTIMIZED_INSTANT.get(InstantKind.AUTO), auto3Map.get(pst));
        assertEquals(FLOW_PER_OPTIMIZED_INSTANT.get(InstantKind.AUTO), output.getOptimizedSetPointOnState(auto3state, pst));
        assertEquals(1, output.getOptimizedTapOnState(crac.getPreventiveState(), pst));
        Map<PstRangeAction, Integer> optimizedTapsauto3state = output.getOptimizedTapsOnState(auto3state);
        assertEquals(1, optimizedTapsauto3state.size());
        assertEquals(2, optimizedTapsauto3state.get(pst));
        assertEquals(output.getActivatedRangeActionsDuringState(auto3state), Set.of(pst));

        // not optimized state
        State cur4state = crac.getState("contingency-4", crac.getInstant(InstantKind.CURATIVE));
        Map<RangeAction<?>, Double> cur4Map = output.getOptimizedSetPointsOnState(cur4state);
        assert cur4Map.isEmpty();
        assertThrows(OpenRaoException.class, () -> output.getOptimizedSetPointOnState(cur4state, pst));
        Map<PstRangeAction, Integer> optimizedTapscur4state = output.getOptimizedTapsOnState(cur4state);
        assert optimizedTapscur4state.isEmpty();
        assert output.getActivatedRangeActionsDuringState(cur4state).isEmpty();
    }

    private void checkFunctionalCosts() {
        assertEquals(34., output.getFunctionalCost(null), DOUBLE_TOLERANCE);
        assertEquals(134., output.getFunctionalCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(233., output.getFunctionalCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(-111., output.getFunctionalCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    private void checkVirtualCosts() {
        assertEquals(34.1, output.getVirtualCost(null), DOUBLE_TOLERANCE);
        assertEquals(134.1, output.getVirtualCost(preventiveInstant), DOUBLE_TOLERANCE);
        assertEquals(233.1, output.getVirtualCost(autoInstant), DOUBLE_TOLERANCE);
        assertEquals(0., output.getVirtualCost(curativeInstant), DOUBLE_TOLERANCE);
    }

    private void checkFlows() {
        for (FlowCnec cnec : crac.getFlowCnecs().stream().sorted(Comparator.comparing(Identifiable::getId)).toList()) {
            for (Instant instant : List.of(preventiveInstant, autoInstant, curativeInstant)) {
                if (!instant.comesAfter(cnec.getState().getInstant())) {
                    double signum = shouldBeSecured(cnec, instant) ? 1 : -1;
                    double expectedFlow = signum * (
                        FLOW_PER_OPTIMIZED_INSTANT.get(getMostRecentOptimInstant(cnec, instant).getKind()) +
                            FLOW_PER_INSTANT.get(cnec.getState().getInstant().getKind()) +
                            Double.parseDouble(cnec.getId().charAt(5) + ""));
                    try {
                        assertEquals(expectedFlow, output.getFlow(instant, cnec, ONE, MEGAWATT), DOUBLE_TOLERANCE);
                    } catch (AssertionFailedError e) {
                        System.out.println("Error for flow on " + cnec.getId() + " at " + instant);
                        throw e;
                    }
                }
            }
        }
    }

    private void checkOptimizationResults() {
        crac.getStates().stream()
            .filter(state -> !state.getInstant().isOutage())
            .forEach(state -> assertNotNull(output.getOptimizationResult(state.getInstant(), state)));
    }

    @Test
    public void testGlobalComputationStatusWhenFinalPreventiveFails() {
        when(prevResult.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.FAILURE, output.getComputationStatus());
    }

    @Test
    public void testGlobalComputationStatusWhenFinalPreventivePartiallyFails() {
        when(prevResult.getComputationStatus()).thenReturn(ComputationStatus.PARTIAL_FAILURE);
        assertEquals(ComputationStatus.PARTIAL_FAILURE, output.getComputationStatus());
    }

    @Test
    public void testGlobalComputationStatusWhenAContingencyFails() {
        when(autoResult4.getComputationStatus()).thenReturn(ComputationStatus.FAILURE);
        assertEquals(ComputationStatus.PARTIAL_FAILURE, output.getComputationStatus());
    }
}
