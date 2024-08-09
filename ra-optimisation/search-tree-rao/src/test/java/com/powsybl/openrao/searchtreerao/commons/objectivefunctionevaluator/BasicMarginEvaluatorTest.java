/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class BasicMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final BasicMarginEvaluator basicMinMarginEvaluator = new BasicMarginEvaluator();
    private final BasicRelativeMarginEvaluator basicRelativeMarginEvaluator = new BasicRelativeMarginEvaluator();

    @Test
    void getMargin() {
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        double margin = basicMinMarginEvaluator.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void getRelativeMargin() {
        when(currentFlowResult.getRelativeMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        double margin = basicRelativeMarginEvaluator.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }
}
