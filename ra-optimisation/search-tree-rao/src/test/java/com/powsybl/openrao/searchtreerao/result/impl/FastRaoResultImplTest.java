/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.OptimizationStepsExecuted;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.powsybl.openrao.data.raoresult.api.ComputationStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FastRaoResultImplTest {

    private PrePerimeterResult initialResult;
    private PrePerimeterResult afterPraResult;
    private PrePerimeterResult afterAraResult;
    private PrePerimeterResult finalResult;
    private RaoResult filteredRaoResult;
    private Crac crac;

    @BeforeEach
    void setUp() {
        crac = ExhaustiveCracCreation.create();
        initialResult = Mockito.mock(PrePerimeterResult.class);
        afterPraResult = Mockito.mock(PrePerimeterResult.class);
        afterAraResult = Mockito.mock(PrePerimeterResult.class);
        finalResult = Mockito.mock(PrePerimeterResult.class);
        for (State state : crac.getStates()) {
            when(initialResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(afterPraResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(afterAraResult.getComputationStatus(state)).thenReturn(DEFAULT);
            when(finalResult.getComputationStatus(state)).thenReturn(DEFAULT);
        }
        filteredRaoResult = Mockito.mock(RaoResult.class);
        when(filteredRaoResult.getExecutionDetails()).thenReturn(OptimizationStepsExecuted.FIRST_PREVENTIVE_ONLY);
    }

    @Test
    void testGetComputationStatus() {
        when(initialResult.getComputationStatus()).thenReturn(DEFAULT);
        when(afterPraResult.getComputationStatus()).thenReturn(PARTIAL_FAILURE);
        when(afterAraResult.getComputationStatus()).thenReturn(DEFAULT);
        when(finalResult.getComputationStatus()).thenReturn(DEFAULT);
        FastRaoResultImpl result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
        ComputationStatus status = result.getComputationStatus();
        assert(status == PARTIAL_FAILURE);

        when(initialResult.getComputationStatus()).thenReturn(FAILURE);
        when(afterPraResult.getComputationStatus()).thenReturn(DEFAULT);
        when(afterAraResult.getComputationStatus()).thenReturn(PARTIAL_FAILURE);
        when(finalResult.getComputationStatus()).thenReturn(DEFAULT);
        result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
        status = result.getComputationStatus();
        assert(status == FAILURE);
    }

    @Test
    void TestGetAppropriateResult() {
        FastRaoResultImpl result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
        assertEquals(initialResult, result.getAppropriateResult(null));
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("preventive")));
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("outage")));
        assertEquals(afterAraResult, result.getAppropriateResult(crac.getInstant("auto")));
        assertEquals(finalResult, result.getAppropriateResult(crac.getInstant("curative")));
        assertThrows(OpenRaoException.class, () -> result.getAppropriateResult(crac.getInstant("blabla")));
    }

    @Test
    void TestGetAppropriateResultFlowCnec() {
        FlowCnec flowCnec = crac.getFlowCnec("cnec3autoId");
        FastRaoResultImpl result = new FastRaoResultImpl(
            initialResult, afterPraResult, afterAraResult, finalResult, filteredRaoResult, crac
        );
        assertEquals(afterPraResult, result.getAppropriateResult(crac.getInstant("preventive"), flowCnec));
        assertEquals(afterAraResult, result.getAppropriateResult(crac.getInstant("curative"), flowCnec));
        assertEquals(initialResult, result.getAppropriateResult(null, flowCnec));

    }
}