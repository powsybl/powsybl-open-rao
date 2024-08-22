/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class MarginEvaluatorWithMarginDecreaseUnoptimizedCnecsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final FlowResult prePerimeterFlowResult = Mockito.mock(FlowResult.class);
    private final MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs marginEvaluatorWithUnoptimizedCnecs =
            new MarginEvaluatorWithMarginDecreaseUnoptimizedCnecs(
                    new BasicMarginEvaluator(),
                    Set.of("FR"),
                    prePerimeterFlowResult
            );

    @BeforeEach
    public void setUp() {
        when(flowCnec.getMonitoredSides()).thenReturn(Set.of(TwoSides.ONE));
    }

    @Test
    void getMarginInMegawattOnOptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("NL");
        when(currentFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(200.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void getMarginInAmpereOnOptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("NL");
        when(currentFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.AMPERE)).thenReturn(50.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.AMPERE);
        assertEquals(50., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void getMarginInMegawattOnConstrainedUnoptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("FR");
        when(currentFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(400.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    void getMarginInMegawattOnUnconstrainedUnoptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("FR");
        when(currentFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterFlowResult.getMargin(flowCnec, TwoSides.ONE, Unit.MEGAWATT)).thenReturn(100.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }
}
