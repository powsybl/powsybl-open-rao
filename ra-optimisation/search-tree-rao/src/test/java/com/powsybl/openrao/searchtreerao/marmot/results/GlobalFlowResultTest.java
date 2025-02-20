/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalFlowResultTest {
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private FlowResult flowResultTimestamp1;
    private FlowResult flowResultTimestamp2;
    private FlowResult flowResultTimestamp3;

    @BeforeEach
    void setUp() {
        State stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        State stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        State stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        flowCnecTimestamp1 = TestsUtils.mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = TestsUtils.mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = TestsUtils.mockFlowCnec(stateTimestamp3);

        flowResultTimestamp1 = TestsUtils.createMockedFlowResult(ComputationStatus.DEFAULT);
        TestsUtils.mockFlowResult(flowResultTimestamp1, flowCnecTimestamp1, 100., 10.);

        flowResultTimestamp2 = TestsUtils.createMockedFlowResult(ComputationStatus.PARTIAL_FAILURE);
        TestsUtils.mockFlowResult(flowResultTimestamp2, flowCnecTimestamp2, 200., 20.);

        flowResultTimestamp3 = TestsUtils.createMockedFlowResult(ComputationStatus.FAILURE);
        TestsUtils.mockFlowResult(flowResultTimestamp3, flowCnecTimestamp3, 300., 30.);
    }

    @Test
    void testFlow() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2, TestsUtils.TIMESTAMP_3, flowResultTimestamp3)));
        assertEquals(100., globalFlowResult.getFlow(flowCnecTimestamp1, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(200., globalFlowResult.getFlow(flowCnecTimestamp2, TwoSides.ONE, Unit.MEGAWATT));
        assertEquals(300., globalFlowResult.getFlow(flowCnecTimestamp3, TwoSides.ONE, Unit.MEGAWATT));
    }

    @Test
    void testMargin() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2, TestsUtils.TIMESTAMP_3, flowResultTimestamp3)));
        assertEquals(10., globalFlowResult.getMargin(flowCnecTimestamp1, Unit.MEGAWATT));
        assertEquals(20., globalFlowResult.getMargin(flowCnecTimestamp2, Unit.MEGAWATT));
        assertEquals(30., globalFlowResult.getMargin(flowCnecTimestamp3, Unit.MEGAWATT));
    }

    @Test
    void testComputationStatusWithFailure() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2, TestsUtils.TIMESTAMP_3, flowResultTimestamp3)));
        assertEquals(ComputationStatus.FAILURE, globalFlowResult.getComputationStatus());
    }

    @Test
    void testComputationStatusWithPartialFailure() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1, TestsUtils.TIMESTAMP_2, flowResultTimestamp2)));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, globalFlowResult.getComputationStatus());
    }

    @Test
    void testComputationStatusDefault() {
        FlowResult globalFlowResult = new GlobalFlowResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, flowResultTimestamp1)));
        assertEquals(ComputationStatus.DEFAULT, globalFlowResult.getComputationStatus());
    }
}
