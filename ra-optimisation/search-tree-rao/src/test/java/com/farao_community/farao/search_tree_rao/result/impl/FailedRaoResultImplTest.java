/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.result.impl;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.Instant;
import com.powsybl.open_rao.data.crac_api.cnec.AngleCnec;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnec;
import com.powsybl.open_rao.data.crac_api.network_action.NetworkAction;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.rao_result_api.ComputationStatus;
import com.powsybl.open_rao.data.rao_result_api.OptimizationStepsExecuted;
import org.junit.jupiter.api.Test;

import static com.powsybl.open_rao.commons.Unit.MEGAWATT;
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

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFunctionalCost(optInstant));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant));
        assertThrows(FaraoException.class, failedRaoResultImpl::getVirtualCostNames);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVirtualCost(optInstant, ""));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.wasActivatedBeforeState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, networkAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedNetworkActionsDuringState(state));

        assertThrows(FaraoException.class, () -> failedRaoResultImpl.isActivatedDuringState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapOnState(state, pstRangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPreOptimizationSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointOnState(state, rangeAction));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getActivatedRangeActionsDuringState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedTapsOnState(state));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getOptimizedSetPointsOnState(state));
        assertThrows(FaraoException.class, failedRaoResultImpl::getOptimizationStepsExecuted);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.setOptimizationStepsExecuted(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY));
    }

    @Test
    void testAngleAndVoltageCnec() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        AngleCnec angleCnec = mock(AngleCnec.class);
        VoltageCnec voltageCnec = mock(VoltageCnec.class);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, angleCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, voltageCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getVoltage(optInstant, voltageCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getAngle(optInstant, angleCnec, MEGAWATT));
    }

    @Test
    void testgetFlowAndMargin() {
        Instant optInstant = mock(Instant.class);
        FailedRaoResultImpl failedRaoResultImpl = new FailedRaoResultImpl();
        FlowCnec flowCnec = mock(FlowCnec.class);
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getCommercialFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getLoopFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getPtdfZonalSum(optInstant, flowCnec, Side.LEFT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getFlow(optInstant, flowCnec, Side.LEFT, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getMargin(optInstant, flowCnec, MEGAWATT));
        assertThrows(FaraoException.class, () -> failedRaoResultImpl.getRelativeMargin(optInstant, flowCnec, MEGAWATT));
    }
}
