/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.rao_api.results.FlowResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class MarginEvaluatorWithUnoptimizedCnecsTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final FlowResult prePerimeterFlowResult = Mockito.mock(FlowResult.class);
    private final MarginEvaluatorWithUnoptimizedCnecs marginEvaluatorWithUnoptimizedCnecs =
            new MarginEvaluatorWithUnoptimizedCnecs(
                    FlowResult::getMargin,
                    Set.of("FR"),
                    prePerimeterFlowResult
            );

    @Test
    public void getMarginInMegawattOnOptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("NL");
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInAmpereOnOptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("NL");
        when(currentFlowResult.getMargin(flowCnec, Unit.AMPERE)).thenReturn(50.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.AMPERE);
        assertEquals(50., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInMegawattOnConstrainedUnoptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("FR");
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(400.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getMarginInMegawattOnUnconstrainedUnoptimizedCnec() {
        when(flowCnec.getOperator()).thenReturn("FR");
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        when(prePerimeterFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(100.);

        double margin = marginEvaluatorWithUnoptimizedCnecs.getMargin(currentFlowResult, flowCnec, Unit.MEGAWATT);
        assertEquals(Double.MAX_VALUE, margin, DOUBLE_TOLERANCE);
    }
}
