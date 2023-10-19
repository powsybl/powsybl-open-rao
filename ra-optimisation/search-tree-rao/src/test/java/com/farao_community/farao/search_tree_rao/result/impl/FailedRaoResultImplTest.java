/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class FailedRaoResultImplTest {
    @Test
    void testBasicReturns() {
        Instant optInstant = mock(Instant.class);
        State state = mock(State.class);
        PstRangeAction pstRangeAction = mock(PstRangeAction.class);
        RangeAction rangeAction = mock(RangeAction.class);
        NetworkAction networkAction = mock(NetworkAction.class);

        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();

        assertEquals(ComputationStatus.FAILURE, failedRaoResultImpl.getComputationStatus());
        assertEquals(ComputationStatus.FAILURE, failedRaoResultImpl.getComputationStatus(state));

        FaraoException exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFunctionalCost(optInstant));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, failedRaoResultImpl::getVirtualCostNames);
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant, ""));
        assertEquals("", exception.getMessage());

        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.wasActivatedBeforeState(state, networkAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, networkAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedNetworkActionsDuringState(state));
        assertEquals("", exception.getMessage());

        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, rangeAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationTapOnState(state, pstRangeAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapOnState(state, pstRangeAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationSetPointOnState(state, rangeAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointOnState(state, rangeAction));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedRangeActionsDuringState(state));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapsOnState(state));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointsOnState(state));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, failedRaoResultImpl::getOptimizationStepsExecuted);
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testAngleAndVoltageCnec() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        AngleCnec angleCnec = mock(AngleCnec.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        FaraoException exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, angleCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, voltageCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVoltage(optInstant, voltageCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getAngle(optInstant, angleCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
    }

    @Test
    void testgetFlowAndMargin() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        FlowCnec flowCnec = mock(FlowCnec.class);
        FaraoException exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getCommercialFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getLoopFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPtdfZonalSum(optInstant, flowCnec, Side.LEFT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, flowCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
        exception = assertThrows(FaraoException.class, () -> failedRaoResultImpl.getRelativeMargin(optInstant, flowCnec, MEGAWATT));
        assertEquals("", exception.getMessage());
    }
}
