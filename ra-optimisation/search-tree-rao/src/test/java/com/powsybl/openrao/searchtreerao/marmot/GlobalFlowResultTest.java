/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.marmot.results.GlobalFlowResult;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalFlowResultTest {
    private final OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 17, 13, 33, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 18, 13, 33, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 19, 13, 33, 0, 0, ZoneOffset.UTC);
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private FlowResult flowResultTimestamp1;
    private FlowResult flowResultTimestamp2;
    private FlowResult flowResultTimestamp3;

    @BeforeEach
    void setUp() {
        State stateTimestamp1 = mockState(timestamp1);
        State stateTimestamp2 = mockState(timestamp2);
        State stateTimestamp3 = mockState(timestamp3);

        flowCnecTimestamp1 = mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = mockFlowCnec(stateTimestamp3);

        flowResultTimestamp1 = createMockedFlowResult(ComputationStatus.DEFAULT);
        mockFlowResult(flowResultTimestamp1, flowCnecTimestamp1, 100., 10.);

        flowResultTimestamp2 = createMockedFlowResult(ComputationStatus.PARTIAL_FAILURE);
        mockFlowResult(flowResultTimestamp2, flowCnecTimestamp2, 200., 20.);

        flowResultTimestamp3 = createMockedFlowResult(ComputationStatus.FAILURE);
        mockFlowResult(flowResultTimestamp3, flowCnecTimestamp3, 300., 30.);
    }

    @Test
    void testFlow() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(timestamp1, flowResultTimestamp1, timestamp2, flowResultTimestamp2, timestamp3, flowResultTimestamp3)));
        assertEquals(100., globalFlowResult.getFlow(flowCnecTimestamp1, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(200., globalFlowResult.getFlow(flowCnecTimestamp2, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(300., globalFlowResult.getFlow(flowCnecTimestamp3, TwoSides.ONE, Unit.MEGAWATT));
    }

    @Test
    void testMargin() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(timestamp1, flowResultTimestamp1, timestamp2, flowResultTimestamp2, timestamp3, flowResultTimestamp3)));
        assertEquals(10., globalFlowResult.getMargin(flowCnecTimestamp1, Unit.MEGAWATT));
        assertEquals(20., globalFlowResult.getMargin(flowCnecTimestamp2, Unit.MEGAWATT));
        assertEquals(30., globalFlowResult.getMargin(flowCnecTimestamp3, Unit.MEGAWATT));
    }

    @Test
    void testComputationStatusWithFailure() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(timestamp1, flowResultTimestamp1, timestamp2, flowResultTimestamp2, timestamp3, flowResultTimestamp3)));
        assertEquals(ComputationStatus.FAILURE, globalFlowResult.getComputationStatus());
    }

    @Test
    void testComputationStatusWithPartialFailure() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(timestamp1, flowResultTimestamp1, timestamp2, flowResultTimestamp2)));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, globalFlowResult.getComputationStatus());
    }

    @Test
    void testComputationStatusDefault() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(timestamp1, flowResultTimestamp1)));
        assertEquals(ComputationStatus.DEFAULT, globalFlowResult.getComputationStatus());
    }

    private static State mockState(OffsetDateTime timestamp) {
        State state = Mockito.mock(State.class);
        Mockito.when(state.getTimestamp()).thenReturn(Optional.of(timestamp));
        return state;
    }

    private static FlowCnec mockFlowCnec(State state) {
        FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
        Mockito.when(flowCnec.getState()).thenReturn(state);
        return flowCnec;
    }

    private static FlowResult createMockedFlowResult(ComputationStatus computationStatus) {
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        Mockito.when(flowResult.getComputationStatus()).thenReturn(computationStatus);
        return flowResult;
    }

    private static void mockFlowResult(FlowResult mockedFlowResult, FlowCnec flowCnec, double flow, double margin) {
        Mockito.when(mockedFlowResult.getFlow(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(flow);
        Mockito.when(mockedFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(margin);
    }
}
