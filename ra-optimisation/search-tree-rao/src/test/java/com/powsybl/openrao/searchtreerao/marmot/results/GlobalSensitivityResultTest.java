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
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalSensitivityResultTest {
    private FlowCnec flowCnecTimestamp1;
    private FlowCnec flowCnecTimestamp2;
    private FlowCnec flowCnecTimestamp3;
    private PstRangeAction pstRangeActionTimestamp1;
    private PstRangeAction pstRangeActionTimestamp2;
    private PstRangeAction pstRangeActionTimestamp3;
    private SensitivityResult sensitivityResultTimestamp1;
    private SensitivityResult sensitivityResultTimestamp2;
    private SensitivityResult sensitivityResultTimestamp3;

    @BeforeEach
    void setUp() {
        State stateTimestamp1 = TestsUtils.mockState(TestsUtils.TIMESTAMP_1);
        State stateTimestamp2 = TestsUtils.mockState(TestsUtils.TIMESTAMP_2);
        State stateTimestamp3 = TestsUtils.mockState(TestsUtils.TIMESTAMP_3);

        flowCnecTimestamp1 = TestsUtils.mockFlowCnec(stateTimestamp1);
        flowCnecTimestamp2 = TestsUtils.mockFlowCnec(stateTimestamp2);
        flowCnecTimestamp3 = TestsUtils.mockFlowCnec(stateTimestamp3);

        pstRangeActionTimestamp1 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp2 = Mockito.mock(PstRangeAction.class);
        pstRangeActionTimestamp3 = Mockito.mock(PstRangeAction.class);

        sensitivityResultTimestamp1 = TestsUtils.createMockedSensitivityResult(ComputationStatus.DEFAULT);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp1, flowCnecTimestamp1, pstRangeActionTimestamp1, 15.);

        sensitivityResultTimestamp2 = TestsUtils.createMockedSensitivityResult(ComputationStatus.PARTIAL_FAILURE);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp2, flowCnecTimestamp2, pstRangeActionTimestamp2, 30.);

        sensitivityResultTimestamp3 = TestsUtils.createMockedSensitivityResult(ComputationStatus.FAILURE);
        TestsUtils.mockSensitivityResult(sensitivityResultTimestamp3, flowCnecTimestamp3, pstRangeActionTimestamp3, 45.);
    }

    @Test
    void testSensitivityStatusWithFailure() {
        SensitivityResult globalSensitivityResult = new GlobalSensitivityResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1, TestsUtils.TIMESTAMP_2, sensitivityResultTimestamp2, TestsUtils.TIMESTAMP_3, sensitivityResultTimestamp3)));
        assertEquals(ComputationStatus.FAILURE, globalSensitivityResult.getSensitivityStatus());
        assertEquals(ComputationStatus.FAILURE, globalSensitivityResult.getSensitivityStatus());
    }

    @Test
    void testComputationStatusWithPartialFailure() {
        SensitivityResult globalSensitivityResult = new GlobalSensitivityResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1, TestsUtils.TIMESTAMP_2, sensitivityResultTimestamp2)));
        assertEquals(ComputationStatus.PARTIAL_FAILURE, globalSensitivityResult.getSensitivityStatus());
        assertEquals(ComputationStatus.PARTIAL_FAILURE, globalSensitivityResult.getSensitivityStatus());
    }

    @Test
    void testComputationStatusDefault() {
        SensitivityResult globalSensitivityResult = new GlobalSensitivityResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1)));
        assertEquals(ComputationStatus.DEFAULT, globalSensitivityResult.getSensitivityStatus());
        assertEquals(ComputationStatus.DEFAULT, globalSensitivityResult.getSensitivityStatus());
    }

    @Test
    void testSensitivityValue() {
        SensitivityResult globalSensitivityResult = new GlobalSensitivityResult(new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, sensitivityResultTimestamp1, TestsUtils.TIMESTAMP_2, sensitivityResultTimestamp2, TestsUtils.TIMESTAMP_3, sensitivityResultTimestamp3)));
        assertEquals(15., globalSensitivityResult.getSensitivityValue(flowCnecTimestamp1, TwoSides.ONE, pstRangeActionTimestamp1, Unit.MEGAWATT));
        assertEquals(30., globalSensitivityResult.getSensitivityValue(flowCnecTimestamp2, TwoSides.ONE, pstRangeActionTimestamp2, Unit.MEGAWATT));
        assertEquals(45., globalSensitivityResult.getSensitivityValue(flowCnecTimestamp3, TwoSides.ONE, pstRangeActionTimestamp3, Unit.MEGAWATT));
    }
}
