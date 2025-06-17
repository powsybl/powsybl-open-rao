/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class MnecViolationCostEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.1;

    private FlowCnec mnec1;
    private FlowCnec mnec2;
    private FlowCnec pureCnec;
    private FlowResult initialFlowResult;
    private FlowResult currentFlowResult;
    private MnecViolationCostEvaluator evaluator1;
    private MnecViolationCostEvaluator evaluator2;

    @BeforeEach
    public void setUp() {
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());
        mnec1 = Mockito.mock(FlowCnec.class);
        when(mnec1.isOptimized()).thenReturn(true);
        when(mnec1.isMonitored()).thenReturn(true);
        when(mnec1.getState()).thenReturn(state);
        mnec2 = Mockito.mock(FlowCnec.class);
        when(mnec2.isOptimized()).thenReturn(false);
        when(mnec2.isMonitored()).thenReturn(true);
        when(mnec2.getState()).thenReturn(state);
        pureCnec = Mockito.mock(FlowCnec.class);
        when(pureCnec.isOptimized()).thenReturn(true);
        when(pureCnec.isMonitored()).thenReturn(false);
        when(pureCnec.getState()).thenReturn(state);

        initialFlowResult = Mockito.mock(FlowResult.class);
        currentFlowResult = Mockito.mock(FlowResult.class);

        MnecParametersExtension mnecExtension1 = new MnecParametersExtension();
        mnecExtension1.setAcceptableMarginDecrease(50);
        mnecExtension1.setViolationCost(10);
        mnecExtension1.setConstraintAdjustmentCoefficient(1);

        MnecParametersExtension mnecExtension2 = new MnecParametersExtension();
        mnecExtension2.setAcceptableMarginDecrease(20);
        mnecExtension2.setViolationCost(2);
        mnecExtension2.setConstraintAdjustmentCoefficient(1);

        evaluator1 = new MnecViolationCostEvaluator(
            Set.of(mnec1, pureCnec),
            Unit.MEGAWATT,
            initialFlowResult,
            mnecExtension1
        );
        evaluator2 = new MnecViolationCostEvaluator(
            Set.of(mnec1, pureCnec),
            Unit.MEGAWATT,
            initialFlowResult,
            mnecExtension2
        );
    }

    private MnecViolationCostEvaluator createEvaluatorWithCosts(double violationCost, Unit unit) {
        when(initialFlowResult.getMargin(mnec1, unit)).thenReturn(-200.);
        when(currentFlowResult.getMargin(mnec1, unit)).thenReturn(-270.);
        when(initialFlowResult.getMargin(mnec2, unit)).thenReturn(-200.);
        when(currentFlowResult.getMargin(mnec2, unit)).thenReturn(-400.);

        MnecParametersExtension mnec = new MnecParametersExtension();
        mnec.setAcceptableMarginDecrease(50);
        mnec.setViolationCost(violationCost);
        mnec.setConstraintAdjustmentCoefficient(1);

        return new MnecViolationCostEvaluator(
            Set.of(mnec1, mnec2, pureCnec),
            unit,
            initialFlowResult,
            mnec
        );
    }

    @Test
    void getName() {
        assertEquals("mnec-cost", evaluator1.getName());
    }

    @Test
    void getElementsInViolation() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(10, Unit.MEGAWATT);

        List<FlowCnec> costlyElements = evaluator.evaluate(currentFlowResult, null).getCostlyElements(Set.of());
        assertEquals(2, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
        assertSame(mnec1, costlyElements.get(1));
    }

    @Test
    void testVirtualCostComputationInMW() {
        testCost(-100, 0, 0, 0);
        testCost(-100, -50, 0, 0);
        testCost(-100, -150, 0, 60);
        testCost(-100, -200, 500, 160);
        testCost(-100, -250, 1000, 260);
        testCost(30, 0, 0, 0);
        testCost(30, -20, 0, 40);
        testCost(30, -50, 300, 100);
        testCost(200, 200, 0, 0);
        testCost(200, 100, 0, 0);
        testCost(200, 0, 0, 0);
        testCost(200, -10, 100, 20);
    }

    private void testCost(double initMargin, double newMargin, double expectedCostWithEval1, double expectedCostWithEval2) {
        when(initialFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(initMargin);
        when(currentFlowResult.getMargin(mnec1, Unit.MEGAWATT)).thenReturn(newMargin);

        assertEquals(
            expectedCostWithEval1,
            evaluator1.evaluate(currentFlowResult, null).getCost(Set.of()),
            DOUBLE_TOLERANCE
        );

        assertEquals(
            expectedCostWithEval2,
            evaluator2.evaluate(currentFlowResult, null).getCost(Set.of()),
            DOUBLE_TOLERANCE
        );
    }

    @Test
    void testAmperes() {
        MnecViolationCostEvaluator evaluator = createEvaluatorWithCosts(10, Unit.AMPERE);

        List<FlowCnec> costlyElements = evaluator.evaluate(currentFlowResult, null).getCostlyElements(Set.of());
        assertEquals(2, costlyElements.size());
        assertSame(mnec2, costlyElements.get(0));
        assertSame(mnec1, costlyElements.get(1));
    }
}
