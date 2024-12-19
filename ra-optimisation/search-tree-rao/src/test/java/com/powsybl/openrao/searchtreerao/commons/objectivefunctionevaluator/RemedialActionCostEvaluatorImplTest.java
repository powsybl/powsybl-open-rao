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
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.impl.RemedialActionActivationResultImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        rangeActionsOptimizationParameters.setPstPenaltyCost(0.01);
        rangeActionsOptimizationParameters.setInjectionRaPenaltyCost(0.02);
        rangeActionsOptimizationParameters.setHvdcPenaltyCost(0.5);

        PstRangeAction pstRangeAction1 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction1.getActivationCost()).thenReturn(Optional.empty());
        Mockito.when(pstRangeAction1.getVariationCost(VariationDirection.UP)).thenReturn(Optional.of(1d));
        Mockito.when(pstRangeAction1.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.empty());

        PstRangeAction pstRangeAction2 = Mockito.mock(PstRangeAction.class);
        Mockito.when(pstRangeAction2.getActivationCost()).thenReturn(Optional.of(10d));
        Mockito.when(pstRangeAction2.getVariationCost(VariationDirection.UP)).thenReturn(Optional.empty());
        Mockito.when(pstRangeAction2.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.empty());

        InjectionRangeAction injectionRangeAction1 = Mockito.mock(InjectionRangeAction.class);
        Mockito.when(injectionRangeAction1.getActivationCost()).thenReturn(Optional.of(5d));
        Mockito.when(injectionRangeAction1.getVariationCost(VariationDirection.UP)).thenReturn(Optional.of(150d));
        Mockito.when(injectionRangeAction1.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.of(200d));

        InjectionRangeAction injectionRangeAction2 = Mockito.mock(InjectionRangeAction.class);
        Mockito.when(injectionRangeAction2.getActivationCost()).thenReturn(Optional.of(0.25));
        Mockito.when(injectionRangeAction2.getVariationCost(VariationDirection.UP)).thenReturn(Optional.of(200d));
        Mockito.when(injectionRangeAction2.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.empty());

        HvdcRangeAction hvdcRangeAction1 = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdcRangeAction1.getActivationCost()).thenReturn(Optional.of(100d));
        Mockito.when(hvdcRangeAction1.getVariationCost(VariationDirection.UP)).thenReturn(Optional.of(10d));
        Mockito.when(hvdcRangeAction1.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.of(15d));

        HvdcRangeAction hvdcRangeAction2 = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(hvdcRangeAction2.getActivationCost()).thenReturn(Optional.of(200d));
        Mockito.when(hvdcRangeAction2.getVariationCost(VariationDirection.UP)).thenReturn(Optional.of(0.1));
        Mockito.when(hvdcRangeAction2.getVariationCost(VariationDirection.DOWN)).thenReturn(Optional.empty());

        NetworkAction topologyAction = Mockito.mock(NetworkAction.class);
        Mockito.when(topologyAction.getActivationCost()).thenReturn(Optional.of(20d));

        state = Mockito.mock(State.class);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        OptimizationPerimeter optimizationPerimeter = Mockito.mock(OptimizationPerimeter.class);
        Mockito.when(optimizationPerimeter.getMainOptimizationState()).thenReturn(state);
        Mockito.when(optimizationPerimeter.getMonitoredStates()).thenReturn(Set.of());

        remedialActionActivationResult = Mockito.mock(RemedialActionActivationResultImpl.class);
        Mockito.when(remedialActionActivationResult.getActivatedNetworkActions()).thenReturn(Set.of(topologyAction));
        Mockito.when(remedialActionActivationResult.getActivatedRangeActions(state)).thenReturn(Set.of(pstRangeAction1, pstRangeAction2, injectionRangeAction1, injectionRangeAction2, hvdcRangeAction1, hvdcRangeAction2));
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction1, state)).thenReturn(2);
        Mockito.when(remedialActionActivationResult.getTapVariation(pstRangeAction2, state)).thenReturn(-5);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction1, state)).thenReturn(35d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(injectionRangeAction2, state)).thenReturn(-75d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction1, state)).thenReturn(600d);
        Mockito.when(remedialActionActivationResult.getSetPointVariation(hvdcRangeAction2, state)).thenReturn(-300d);
    }

    @Test
    void testTotalRemedialActionCost() {
        RemedialActionCostEvaluator evaluator = new RemedialActionCostEvaluator(Set.of(state));
        assertEquals("remedial-action-cost-evaluator", evaluator.getName());
        assertEquals(11587.25, evaluator.evaluate(null, remedialActionActivationResult).getCost(Set.of()));
    }
}
