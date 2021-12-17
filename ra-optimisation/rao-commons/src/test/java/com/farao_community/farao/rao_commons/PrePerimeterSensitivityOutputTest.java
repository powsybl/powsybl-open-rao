/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PrePerimeterSensitivityOutputTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testBasicReturns() {
        FlowCnec cnec1 = Mockito.mock(FlowCnec.class);
        FlowCnec cnec2 = Mockito.mock(FlowCnec.class);

        PstRangeAction ra1 = Mockito.mock(PstRangeAction.class);
        RangeAction ra2 = Mockito.mock(RangeAction.class);

        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        RangeActionResult rangeActionResult = Mockito.mock(RangeActionResult.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        PrePerimeterSensitivityOutput output = new PrePerimeterSensitivityOutput(flowResult, sensitivityResult, rangeActionResult, objectiveFunctionResult);

        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getSensitivityStatus());
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.FALLBACK);
        assertEquals(ComputationStatus.FALLBACK, output.getSensitivityStatus());

        when(sensitivityResult.getSensitivityValue(cnec1, ra1, Unit.MEGAWATT)).thenReturn(0.5);
        when(sensitivityResult.getSensitivityValue(cnec2, ra1, Unit.AMPERE)).thenReturn(0.1);
        assertEquals(0.5, output.getSensitivityValue(cnec1, ra1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0.1, output.getSensitivityValue(cnec2, ra1, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(sensitivityResult.getSensitivityValue(cnec2, linearGlsk, Unit.MEGAWATT)).thenReturn(51.);
        assertEquals(51., output.getSensitivityValue(cnec2, linearGlsk, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        when(flowResult.getFlow(cnec1, Unit.MEGAWATT)).thenReturn(10.);
        when(flowResult.getFlow(cnec2, Unit.AMPERE)).thenReturn(117.);
        assertEquals(10., output.getFlow(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(117., output.getFlow(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(564.);
        when(flowResult.getRelativeMargin(cnec2, Unit.AMPERE)).thenReturn(-451.);
        assertEquals(564., output.getRelativeMargin(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-451., output.getRelativeMargin(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getLoopFlow(cnec1, Unit.MEGAWATT)).thenReturn(5064.);
        when(flowResult.getLoopFlow(cnec2, Unit.AMPERE)).thenReturn(-4510.);
        assertEquals(5064., output.getLoopFlow(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-4510., output.getLoopFlow(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getCommercialFlow(cnec1, Unit.MEGAWATT)).thenReturn(50464.);
        when(flowResult.getCommercialFlow(cnec2, Unit.AMPERE)).thenReturn(-45104.);
        assertEquals(50464., output.getCommercialFlow(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-45104., output.getCommercialFlow(cnec2, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getPtdfZonalSum(cnec1)).thenReturn(0.4);
        when(flowResult.getPtdfZonalSum(cnec2)).thenReturn(0.75);
        assertEquals(0.4, output.getPtdfZonalSum(cnec1), DOUBLE_TOLERANCE);
        assertEquals(0.75, output.getPtdfZonalSum(cnec2), DOUBLE_TOLERANCE);

        when(flowResult.getPtdfZonalSums()).thenReturn(Map.of(cnec1, 0.1, cnec2, 0.2));
        assertEquals(Map.of(cnec1, 0.1, cnec2, 0.2), output.getPtdfZonalSums());

        when(rangeActionResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));
        assertEquals(Set.of(ra1, ra2), output.getRangeActions());

        when(rangeActionResult.getOptimizedTap(ra1)).thenReturn(3);
        assertEquals(3, output.getOptimizedTap(ra1), DOUBLE_TOLERANCE);

        when(rangeActionResult.getOptimizedSetPoint(ra1)).thenReturn(15.6);
        assertEquals(15.6, output.getOptimizedSetPoint(ra1), DOUBLE_TOLERANCE);

        when(rangeActionResult.getOptimizedTaps()).thenReturn(Map.of(ra1, 1));
        assertEquals(Map.of(ra1, 1), output.getOptimizedTaps());

        when(rangeActionResult.getOptimizedSetPoints()).thenReturn(Map.of(ra1, 5.3, ra2, 6.7));
        assertEquals(Map.of(ra1, 5.3, ra2, 6.7), output.getOptimizedSetPoints());

        assertEquals(flowResult, output.getBranchResult());
        assertEquals(sensitivityResult, output.getSensitivityResult());
        assertEquals(0, output.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, output.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(0, output.getVirtualCost("mock"), DOUBLE_TOLERANCE);
        assert output.getMostLimitingElements(10).isEmpty();
        assert output.getVirtualCostNames().isEmpty();
        assert output.getCostlyElements("mock", 10).isEmpty();
    }

}
