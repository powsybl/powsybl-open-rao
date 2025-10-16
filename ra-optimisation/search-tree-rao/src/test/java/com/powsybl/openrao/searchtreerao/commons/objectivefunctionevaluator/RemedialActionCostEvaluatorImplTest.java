/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RemedialActionCostEvaluatorImplTest {
    private State state;
    private RemedialActionActivationResult remedialActionActivationResult;

    @BeforeEach
    void setUp() {
        RangeActionsOptimizationParameters rangeActionsOptimizationParameters = new RangeActionsOptimizationParameters();
        rangeActionsOptimizationParameters.setPstRAMinImpactThreshold(0.01);
        rangeActionsOptimizationParameters.setInjectionRAMinImpactThreshold(0.02);
        rangeActionsOptimizationParameters.setHvdcRAMinImpactThreshold(0.5);

        PstRangeAction pstRangeAction1 = Mockito.mock(PstRangeAction.class);
        double pst1ActivationCost = 0.;
        double pst1CostUp = 1.;
        double pst1CostDown = 0.;

        PstRangeAction pstRangeAction2 = Mockito.mock(PstRangeAction.class);
        double pst2ActivationCost = 10.;
        double pst2CostUp = 0.;
        double pst2CostDown = 0.;

        InjectionRangeAction injectionRangeAction1 = Mockito.mock(InjectionRangeAction.class);
        double injection1ActivationCost = 5.;
        double injection1CostUp = 150.;
        double injection1CostDown = 200.;

        InjectionRangeAction injectionRangeAction2 = Mockito.mock(InjectionRangeAction.class);
        double injection2ActivationCost = 0.25;
        double injection2CostUp = 200.;
        double injection2CostDown = 0.;

        HvdcRangeAction hvdcRangeAction1 = Mockito.mock(HvdcRangeAction.class);
        double hvdc1ActivationCost = 100.;
        double hvdc1CostUp = 10.;
        double hvdc1CostDown = 15.;

        HvdcRangeAction hvdcRangeAction2 = Mockito.mock(HvdcRangeAction.class);
        double hvdc2ActivationCost = 200.;
        double hvdc2CostUp = 0.1;
        double hvdc2CostDown = 0.;

        NetworkAction topologyAction = Mockito.mock(NetworkAction.class);
        Mockito.when(topologyAction.getActivationCost()).thenReturn(Optional.of(20d));

        state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(state);
        Mockito.when(optimizationPerimeter.getMonitoredStates()).thenReturn(Set.of());

        remedialActionActivationResult = Mockito.mock(RemedialActionActivationResultImpl.class);
        Mockito.when(remedialActionActivationResult.getActivatedNetworkActionsPerState()).thenReturn(Map.of(state, Set.of(topologyAction)));
        Mockito.when(remedialActionActivationResult.getActivatedRangeActions(state)).thenReturn(Set.of(pstRangeAction1, pstRangeAction2, injectionRangeAction1, injectionRangeAction2, hvdcRangeAction1, hvdcRangeAction2));
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction1, state)).thenReturn(2);
        Mockito.when(pstRangeAction1.getTotalCostForVariation(2.)).thenReturn(pst1ActivationCost + 2 * pst1CostUp);
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction2, state)).thenReturn(-5);
        Mockito.when(pstRangeAction2.getTotalCostForVariation(-5.)).thenReturn(pst2ActivationCost + 5 * pst2CostDown);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction1, state)).thenReturn(35d);
        Mockito.when(injectionRangeAction1.getTotalCostForVariation(35.)).thenReturn(injection1ActivationCost + 35 * injection1CostUp);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction2, state)).thenReturn(-75d);
        Mockito.when(injectionRangeAction2.getTotalCostForVariation(-75.)).thenReturn(injection2ActivationCost + 75 * injection2CostDown);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction1, state)).thenReturn(600d);
        Mockito.when(hvdcRangeAction1.getTotalCostForVariation(600.)).thenReturn(hvdc1ActivationCost + 600 * hvdc1CostUp);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction2, state)).thenReturn(-300d);
        Mockito.when(hvdcRangeAction2.getTotalCostForVariation(-300.)).thenReturn(hvdc2ActivationCost + 300 * hvdc2CostDown);
    }

    @Test
    void testTotalRemedialActionCost() {
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state));
        assertEquals("remedial-action-cost-evaluator", evaluator.getName());
        assertEquals(11587.25, evaluator.evaluate(null, remedialActionActivationResult).getCost(Set.of(), Set.of()));
    }
}
