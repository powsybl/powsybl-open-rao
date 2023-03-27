/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class BasicMarginEvaluatorTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private final FlowCnec flowCnec = Mockito.mock(FlowCnec.class);
    private final FlowResult currentFlowResult = Mockito.mock(FlowResult.class);
    private final FlowResult prePerimeterFlowResult = Mockito.mock(FlowResult.class);
    private final RangeActionActivationResult rangeActionActivationResult = Mockito.mock(RangeActionActivationResult.class);
    private final SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
    private final BasicMarginEvaluator basicMinMarginEvaluator = new BasicMarginEvaluator();
    private final BasicRelativeMarginEvaluator basicRelativeMarginEvaluator = new BasicRelativeMarginEvaluator();

    @Test
    public void getMargin() {
        when(currentFlowResult.getMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        double margin = basicMinMarginEvaluator.getMargin(currentFlowResult, flowCnec, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }

    @Test
    public void getRelativeMargin() {
        when(currentFlowResult.getRelativeMargin(flowCnec, Unit.MEGAWATT)).thenReturn(200.);
        double margin = basicRelativeMarginEvaluator.getMargin(currentFlowResult, flowCnec, rangeActionActivationResult, sensitivityResult, Unit.MEGAWATT);
        assertEquals(200., margin, DOUBLE_TOLERANCE);
    }
}
