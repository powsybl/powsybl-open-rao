/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.extension.CastorCostResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CastorCostResultExtensionHelperTest {

    private Crac crac;
    private Instant preventiveInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    @BeforeEach
    void setUp() {
        crac = mock(Crac.class);
        preventiveInstant = mock(Instant.class);
        autoInstant = mock(Instant.class);
        curativeInstant = mock(Instant.class);

        when(crac.getPreventiveInstant()).thenReturn(preventiveInstant);
        when(crac.getSortedInstants()).thenReturn(List.of(preventiveInstant, autoInstant, curativeInstant));

        when(preventiveInstant.isAuto()).thenReturn(false);
        when(preventiveInstant.isCurative()).thenReturn(false);
        when(autoInstant.isAuto()).thenReturn(true);
        when(autoInstant.isCurative()).thenReturn(false);
        when(curativeInstant.isAuto()).thenReturn(false);
        when(curativeInstant.isCurative()).thenReturn(true);

        when(preventiveInstant.comesBefore(autoInstant)).thenReturn(true);
        when(preventiveInstant.comesBefore(curativeInstant)).thenReturn(true);
        when(autoInstant.comesBefore(curativeInstant)).thenReturn(true);
    }

    @Test
    void testConvertToExtensionSimple() {
        ObjectiveFunctionResult initialResult = mock(ObjectiveFunctionResult.class);
        when(initialResult.getFunctionalCost()).thenReturn(100.0);
        when(initialResult.getVirtualCostNames()).thenReturn(Set.of("v1"));
        when(initialResult.getVirtualCost("v1")).thenReturn(10.0);

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(initialResult, true, crac);

        assertEquals(100.0, result.getFunctionalCost(null));
        assertEquals(10.0, result.getVirtualCost(null, "v1"));
        assertEquals(100.0, result.getFunctionalCost(preventiveInstant));
        assertEquals(10.0, result.getVirtualCost(preventiveInstant, "v1"));
    }

    @Test
    void testConvertToExtensionWithPostPra() {
        ObjectiveFunctionResult initialResult = mock(ObjectiveFunctionResult.class);
        when(initialResult.getFunctionalCost()).thenReturn(100.0);

        ObjectiveFunctionResult postPraResult = mock(ObjectiveFunctionResult.class);
        when(postPraResult.getFunctionalCost()).thenReturn(80.0);
        when(postPraResult.getVirtualCostNames()).thenReturn(Set.of("v1"));
        when(postPraResult.getVirtualCost("v1")).thenReturn(5.0);

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(initialResult, postPraResult, true, crac);

        assertEquals(100.0, result.getFunctionalCost(null));
        assertEquals(80.0, result.getFunctionalCost(preventiveInstant));
        assertEquals(5.0, result.getVirtualCost(preventiveInstant, "v1"));
    }

    @Test
    void testConvertToExtensionComplexCostOptimization() {
        ObjectiveFunctionResult initialResult = createMockResult(200.0, Map.of("v1", 20.0));
        ObjectiveFunctionResult preventiveAndOutageOnlyResult = createMockResult(180.0, Map.of("v1", 18.0));
        ObjectiveFunctionResult postPraResult = createMockResult(150.0, Map.of("v1", 15.0));

        State stateAuto = mock(State.class);
        when(stateAuto.getInstant()).thenReturn(autoInstant);
        OptimizationResult optResAuto = mock(OptimizationResult.class);
        when(optResAuto.getFunctionalCost()).thenReturn(10.0);
        when(optResAuto.getVirtualCost("v1")).thenReturn(1.0);
        PrePerimeterResult preResAuto = mock(PrePerimeterResult.class);
        when(preResAuto.getFunctionalCost()).thenReturn(12.0);
        when(preResAuto.getVirtualCost("v1")).thenReturn(1.2);
        PostPerimeterResult postPerimAuto = new PostPerimeterResult(optResAuto, preResAuto);

        Map<State, PostPerimeterResult> postContingencyResults = Map.of(stateAuto, postPerimAuto);

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(
            initialResult, preventiveAndOutageOnlyResult, postPraResult, postContingencyResults, true, crac
        );

        // Initial
        assertEquals(200.0, result.getFunctionalCost(null));
        assertEquals(20.0, result.getVirtualCost(null, "v1"));

        // Preventive (costOptimization=true uses preventiveAndOutageOnlyResult)
        assertEquals(180.0, result.getFunctionalCost(preventiveInstant));
        assertEquals(15.0, result.getVirtualCost(preventiveInstant, "v1"));

        // Auto Instant
        // Functional: preventiveAndOutageOnly (180) + optResAuto (10) = 190
        assertEquals(190.0, result.getFunctionalCost(autoInstant));
        // Virtual: preventiveAndOutageOnly (18) + preResAuto (1.2) = 19.2
        assertEquals(19.2, result.getVirtualCost(autoInstant, "v1"));

        // Curative Instant
        // Functional: preventiveAndOutageOnly (180) + optResAuto (10) = 190
        assertEquals(190.0, result.getFunctionalCost(curativeInstant));
    }

    @Test
    void testConvertToExtensionComplexMaxOptimization() {
        ObjectiveFunctionResult initialResult = createMockResult(200.0, Map.of("v1", 20.0));
        ObjectiveFunctionResult preventiveAndOutageOnlyResult = createMockResult(180.0, Map.of("v1", 18.0));
        ObjectiveFunctionResult postPraResult = createMockResult(150.0, Map.of("v1", 15.0));

        State stateAuto = mock(State.class);
        when(stateAuto.getInstant()).thenReturn(autoInstant);
        OptimizationResult optResAuto = mock(OptimizationResult.class);
        when(optResAuto.getFunctionalCost()).thenReturn(10.0);
        when(optResAuto.getVirtualCost("v1")).thenReturn(1.0);
        PrePerimeterResult preResAuto = mock(PrePerimeterResult.class);
        when(preResAuto.getFunctionalCost()).thenReturn(12.0);
        when(preResAuto.getVirtualCost("v1")).thenReturn(1.2);
        PostPerimeterResult postPerimAuto = new PostPerimeterResult(optResAuto, preResAuto);

        Map<State, PostPerimeterResult> postContingencyResults = Map.of(stateAuto, postPerimAuto);

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(
            initialResult, preventiveAndOutageOnlyResult, postPraResult, postContingencyResults, false, crac
        );

        // Preventive (costOptimization=false uses postPraResult)
        assertEquals(150.0, result.getFunctionalCost(preventiveInstant));

        // Auto Instant
        // Functional: Math.max(preventiveAndOutageOnly (180), preResAuto (12)) = 180
        assertEquals(180.0, result.getFunctionalCost(autoInstant));

        // Virtual: "v1" uses Double::sum -> 18.0 + 1.2 = 19.2
        assertEquals(19.2, result.getVirtualCost(autoInstant, "v1"));
    }

    @Test
    void testSpecialVirtualCosts() {
        ObjectiveFunctionResult initialResult = createMockResult(0, Map.of());
        ObjectiveFunctionResult preventiveAndOutageOnlyResult = createMockResult(0, Map.of(
            "min-margin-violation-evaluator", 100.0,
            "sensitivity-failure-cost", 50.0,
            "other", 10.0
        ));
        ObjectiveFunctionResult postPraResult = createMockResult(0, Map.of(
            "min-margin-violation-evaluator", 100.0,
            "sensitivity-failure-cost", 50.0,
            "other", 10.0
        ));

        State stateAuto = mock(State.class);
        when(stateAuto.getInstant()).thenReturn(autoInstant);
        OptimizationResult optResAuto = mock(OptimizationResult.class);
        PrePerimeterResult preResAuto = mock(PrePerimeterResult.class);
        when(preResAuto.getVirtualCost("min-margin-violation-evaluator")).thenReturn(120.0);
        when(preResAuto.getVirtualCost("sensitivity-failure-cost")).thenReturn(40.0);
        when(preResAuto.getVirtualCost("other")).thenReturn(5.0);

        PostPerimeterResult postPerimAuto = new PostPerimeterResult(optResAuto, preResAuto);
        Map<State, PostPerimeterResult> postContingencyResults = Map.of(stateAuto, postPerimAuto);

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(
            initialResult, preventiveAndOutageOnlyResult, postPraResult, postContingencyResults, true, crac
        );

        // min-margin: Math.max(100, 120) = 120
        assertEquals(120.0, result.getVirtualCost(autoInstant, "min-margin-violation-evaluator"));
        // sensitivity: Math.max(50, 40) = 50
        assertEquals(50.0, result.getVirtualCost(autoInstant, "sensitivity-failure-cost"));
        // other: 10 + 5 = 15
        assertEquals(15.0, result.getVirtualCost(autoInstant, "other"));
    }

    @Test
    void testNaNHandlingInVirtualCosts() {
        ObjectiveFunctionResult initialResult = createMockResult(0, Map.of("v1", Double.NaN));
        ObjectiveFunctionResult postPraResult = createMockResult(0, Map.of("v1", Double.NaN));

        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(initialResult, postPraResult, true, crac);

        assertEquals(0.0, result.getVirtualCost(null, "v1"));
        assertEquals(0.0, result.getVirtualCost(preventiveInstant, "v1"));
    }

    @Test
    void testOutageInstantsAreIgnored() {
        Instant outageInstant = mock(Instant.class);
        when(outageInstant.isAuto()).thenReturn(false);
        when(outageInstant.isCurative()).thenReturn(false);
        when(outageInstant.isOutage()).thenReturn(true);
        when(crac.getSortedInstants()).thenReturn(List.of(preventiveInstant, outageInstant, autoInstant));

        ObjectiveFunctionResult initialResult = createMockResult(100, Map.of());
        CastorCostResult result = CastorCostResultExtensionHelper.convertToExtension(initialResult, true, crac);

        // Outage instant should not have result
        // CastorCostResult.getFunctionalCost(outageInstant) would return the previous instant's result (preventive)
        // because it uses getPreviousInstantWithResult
        assertEquals(100.0, result.getFunctionalCost(outageInstant));
    }

    private ObjectiveFunctionResult createMockResult(double functionalCost, Map<String, Double> virtualCosts) {
        ObjectiveFunctionResult res = mock(ObjectiveFunctionResult.class);
        when(res.getFunctionalCost()).thenReturn(functionalCost);
        when(res.getVirtualCostNames()).thenReturn(virtualCosts.keySet());
        virtualCosts.forEach((name, val) -> when(res.getVirtualCost(name)).thenReturn(val));
        return res;
    }
}
