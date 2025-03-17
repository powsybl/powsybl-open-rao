/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.marmot.TestsUtils;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class GlobalRaoResultImplTest {
    private FlowCnec flowCnec;
    private RaoResult raoResultTimestamp1;
    private RaoResult raoResultTimestamp2;
    private RaoResult raoResultTimestamp3;
    private GlobalRaoResultImpl globalRaoResult;

    @BeforeEach
    void setUp() {
        flowCnec = Mockito.mock(FlowCnec.class);

        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);
        Mockito.when(objectiveFunctionResult.getFunctionalCost()).thenReturn(900.);
        Mockito.when(objectiveFunctionResult.getVirtualCost()).thenReturn(100.);
        Mockito.when(objectiveFunctionResult.getVirtualCostNames()).thenReturn(Set.of("virtual"));
        Mockito.when(objectiveFunctionResult.getVirtualCost("virtual")).thenReturn(100.);
        Mockito.when(objectiveFunctionResult.getMostLimitingElements(1)).thenReturn(List.of(flowCnec));
        Mockito.when(objectiveFunctionResult.getCostlyElements("virtual", 1)).thenReturn(List.of());

        raoResultTimestamp1 = Mockito.mock(RaoResult.class);
        raoResultTimestamp2 = Mockito.mock(RaoResult.class);
        raoResultTimestamp3 = Mockito.mock(RaoResult.class);

        globalRaoResult = new GlobalRaoResultImpl(objectiveFunctionResult, new TemporalDataImpl<>(Map.of(TestsUtils.TIMESTAMP_1, raoResultTimestamp1, TestsUtils.TIMESTAMP_2, raoResultTimestamp2, TestsUtils.TIMESTAMP_3, raoResultTimestamp3)));
    }

    @Test
    void testCost() {
        assertEquals(1000., globalRaoResult.getGlobalCost());
        assertEquals(900., globalRaoResult.getGlobalFunctionalCost());
        assertEquals(100., globalRaoResult.getGlobalVirtualCost());
        assertEquals(100., globalRaoResult.getGlobalVirtualCost("virtual"));
        assertEquals(Set.of("virtual"), globalRaoResult.getVirtualCostNames());
        assertEquals(List.of(TestsUtils.TIMESTAMP_1, TestsUtils.TIMESTAMP_2, TestsUtils.TIMESTAMP_3), globalRaoResult.getTimestamps());
    }
}
