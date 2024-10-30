/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class RemedialActionActivationResultImplTest {
    private State state;
    private PstRangeAction pstRangeAction;
    private NetworkAction topologicalAction1;
    private NetworkAction topologicalAction2;
    private RangeAction<?> injectionRangeAction1;
    private RangeAction<?> injectionRangeAction2;
    private RangeAction<?> hvdcRangeAction;
    private RemedialActionActivationResultImpl remedialActionActivationResult;

    @BeforeEach
    void setUp() {
        Crac crac = CommonCracCreation.create();

        state = crac.getState(crac.getContingency("Contingency FR1 FR3"), crac.getInstant(InstantKind.CURATIVE));

        pstRangeAction = crac.newPstRangeAction()
            .withId("pst-range-action")
            .withNetworkElement("pst")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-1, -6.22, 0, 0d, 1, 6.22))
            .add();

        topologicalAction1 = crac.newNetworkAction()
            .withId("topological-action-1")
            .newSwitchAction()
            .withNetworkElement("line-1")
            .withActionType(ActionType.OPEN)
            .add()
            .add();

        topologicalAction2 = crac.newNetworkAction()
            .withId("topological-action-2")
            .newSwitchAction()
            .withNetworkElement("line-2")
            .withActionType(ActionType.CLOSE)
            .add()
            .add();

        injectionRangeAction1 = crac.newInjectionRangeAction()
            .withId("injection-range-action-1")
            .withInitialSetpoint(50d)
            .withNetworkElementAndKey(1d, "generator")
            .newRange()
            .withMin(0d)
            .withMax(100d)
            .add()
            .add();

        injectionRangeAction2 = crac.newInjectionRangeAction()
            .withId("injection-range-action-2")
            .withInitialSetpoint(25d)
            .withNetworkElementAndKey(1d, "load")
            .newRange()
            .withMin(10d)
            .withMax(75d)
            .add()
            .add();

        hvdcRangeAction = crac.newHvdcRangeAction()
            .withId("hvdc-range-action")
            .withInitialSetpoint(0d)
            .withNetworkElement("hvdc")
            .newRange()
            .withMin(-1000d)
            .withMax(1000d)
            .add()
            .add();

        PrePerimeterResult prePerimeterResult = Mockito.mock(PrePerimeterResult.class);
        Mockito.when(prePerimeterResult.getTap(pstRangeAction)).thenReturn(0);
        Mockito.when(prePerimeterResult.getSetpoint(pstRangeAction)).thenReturn(0d);
        Mockito.when(prePerimeterResult.getSetpoint(injectionRangeAction1)).thenReturn(60d);
        Mockito.when(prePerimeterResult.getSetpoint(injectionRangeAction2)).thenReturn(25d);
        Mockito.when(prePerimeterResult.getSetpoint(hvdcRangeAction)).thenReturn(300d);

        NetworkActionsResultImpl networkActionsResult = new NetworkActionsResultImpl(Set.of(topologicalAction1));

        RangeActionActivationResultImpl rangeActionActivationResult1 = new RangeActionActivationResultImpl(new RangeActionSetpointResultImpl(Map.of(pstRangeAction, 0d, injectionRangeAction1, 60d, injectionRangeAction2, 25d, hvdcRangeAction, 300d)));
        rangeActionActivationResult1.putResult(pstRangeAction, state, -6.22);
        rangeActionActivationResult1.putResult(injectionRangeAction1, state, 75d);
        rangeActionActivationResult1.putResult(injectionRangeAction2, state, 25d);
        rangeActionActivationResult1.putResult(hvdcRangeAction, state, 800d);

        remedialActionActivationResult = new RemedialActionActivationResultImpl(prePerimeterResult, rangeActionActivationResult1, networkActionsResult);
    }

    @Test
    void testNetworkActionsActivation() {
        assertTrue(remedialActionActivationResult.isActivated(topologicalAction1));
        assertFalse(remedialActionActivationResult.isActivated(topologicalAction2));
        assertEquals(Set.of(topologicalAction1), remedialActionActivationResult.getActivatedNetworkActions());
    }

    @Test
    void testRangeActionsActivation() {
        assertEquals(Set.of(pstRangeAction, injectionRangeAction1, injectionRangeAction2, hvdcRangeAction), remedialActionActivationResult.getRangeActions());
        assertEquals(Set.of(pstRangeAction, injectionRangeAction1, hvdcRangeAction), remedialActionActivationResult.getActivatedRangeActions(state));
        assertEquals(-1, remedialActionActivationResult.getOptimizedTap(pstRangeAction, state));
        assertEquals(-6.22, remedialActionActivationResult.getOptimizedSetpoint(pstRangeAction, state));
        assertEquals(75d, remedialActionActivationResult.getOptimizedSetpoint(injectionRangeAction1, state));
        assertEquals(25d, remedialActionActivationResult.getOptimizedSetpoint(injectionRangeAction2, state));
        assertEquals(800d, remedialActionActivationResult.getOptimizedSetpoint(hvdcRangeAction, state));
        assertEquals(Map.of(pstRangeAction, -1), remedialActionActivationResult.getOptimizedTapsOnState(state));
        assertEquals(Map.of(pstRangeAction, -6.22, injectionRangeAction1, 75d, injectionRangeAction2, 25d, hvdcRangeAction, 800d), remedialActionActivationResult.getOptimizedSetpointsOnState(state));
    }

    @Test
    void testRangeActionsVariation() {
        assertEquals(-1, remedialActionActivationResult.getTapVariation(pstRangeAction, state));
        assertEquals(-6.22, remedialActionActivationResult.getSetPointVariation(pstRangeAction, state));
        assertEquals(15d, remedialActionActivationResult.getSetPointVariation(injectionRangeAction1, state));
        assertEquals(0d, remedialActionActivationResult.getSetPointVariation(injectionRangeAction2, state));
        assertEquals(500d, remedialActionActivationResult.getSetPointVariation(hvdcRangeAction, state));
    }
}
