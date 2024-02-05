/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.*;

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

        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getFunctionalCost(optInstant));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant));
        assertThrows(OpenRaoException.class, failedRaoResultImpl::getVirtualCostNames);
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant, ""));

        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.wasActivatedBeforeState(state, networkAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, networkAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getActivatedNetworkActionsDuringState(state));

        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, rangeAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getPreOptimizationTapOnState(state, pstRangeAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getOptimizedTapOnState(state, pstRangeAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getPreOptimizationSetPointOnState(state, rangeAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getOptimizedSetPointOnState(state, rangeAction));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getActivatedRangeActionsDuringState(state));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getOptimizedTapsOnState(state));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getOptimizedSetPointsOnState(state));
        assertThrows(OpenRaoException.class, failedRaoResultImpl::getOptimizationStepsExecuted);
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
        assertThrows(OpenRaoException.class, failedRaoResultImpl::isSecure);
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.isSecure(optInstant, PhysicalParameter.FLOW));
    }

    @Test
    void testAngleAndVoltageCnec() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        AngleCnec angleCnec = mock(AngleCnec.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getMargin(optInstant, angleCnec, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getMargin(optInstant, voltageCnec, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getVoltage(optInstant, voltageCnec, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getAngle(optInstant, angleCnec, MEGAWATT));
    }

    @Test
    void testgetFlowAndMargin() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        FlowCnec flowCnec = mock(FlowCnec.class);
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getCommercialFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getLoopFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getPtdfZonalSum(optInstant, flowCnec, Side.LEFT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getMargin(optInstant, flowCnec, MEGAWATT));
        assertThrows(OpenRaoException.class, () -> failedRaoResultImpl.getRelativeMargin(optInstant, flowCnec, MEGAWATT));
    }
}
