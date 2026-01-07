/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PrePerimeterSensitivityResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    void testBasicReturns() {
        FlowCnec cnec1 = Mockito.mock(FlowCnec.class);
        FlowCnec cnec2 = Mockito.mock(FlowCnec.class);

        PstRangeAction ra1 = Mockito.mock(PstRangeAction.class);
        RangeAction<?> ra2 = Mockito.mock(RangeAction.class);

        SensitivityVariableSet linearGlsk = Mockito.mock(SensitivityVariableSet.class);
        FlowResult flowResult = Mockito.mock(FlowResult.class);
        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        RangeActionSetpointResult rangeActionSetpointResult = Mockito.mock(RangeActionSetpointResult.class);
        ObjectiveFunctionResult objectiveFunctionResult = Mockito.mock(ObjectiveFunctionResult.class);

        PrePerimeterSensitivityResultImpl output = new PrePerimeterSensitivityResultImpl(flowResult, sensitivityResult, rangeActionSetpointResult, objectiveFunctionResult);

        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        assertEquals(ComputationStatus.DEFAULT, output.getSensitivityStatus());

        when(sensitivityResult.getSensitivityValue(cnec1, TWO, ra1, Unit.MEGAWATT)).thenReturn(0.5);
        when(sensitivityResult.getSensitivityValue(cnec2, ONE, ra1, Unit.AMPERE)).thenReturn(0.1);
        assertEquals(0.5, output.getSensitivityValue(cnec1, TWO, ra1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(0.1, output.getSensitivityValue(cnec2, ONE, ra1, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(sensitivityResult.getSensitivityValue(cnec2, ONE, linearGlsk, Unit.MEGAWATT)).thenReturn(51.);
        assertEquals(51., output.getSensitivityValue(cnec2, ONE, linearGlsk, Unit.MEGAWATT), DOUBLE_TOLERANCE);

        when(flowResult.getFlow(cnec1, TWO, Unit.MEGAWATT)).thenReturn(10.);
        when(flowResult.getFlow(cnec2, ONE, Unit.AMPERE)).thenReturn(117.);
        assertEquals(10., output.getFlow(cnec1, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(117., output.getFlow(cnec2, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getRelativeMargin(cnec1, TWO, Unit.MEGAWATT)).thenReturn(564.);
        when(flowResult.getRelativeMargin(cnec2, ONE, Unit.AMPERE)).thenReturn(-451.);
        assertEquals(564., output.getRelativeMargin(cnec1, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-451., output.getRelativeMargin(cnec2, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getRelativeMargin(cnec1, Unit.MEGAWATT)).thenReturn(564.);
        when(flowResult.getRelativeMargin(cnec1, Unit.AMPERE)).thenReturn(-451.);
        assertEquals(564., output.getRelativeMargin(cnec1, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-451., output.getRelativeMargin(cnec1, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getLoopFlow(cnec1, TWO, Unit.MEGAWATT)).thenReturn(5064.);
        when(flowResult.getLoopFlow(cnec2, ONE, Unit.AMPERE)).thenReturn(-4510.);
        assertEquals(5064., output.getLoopFlow(cnec1, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-4510., output.getLoopFlow(cnec2, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getCommercialFlow(cnec1, TWO, Unit.MEGAWATT)).thenReturn(50464.);
        when(flowResult.getCommercialFlow(cnec2, ONE, Unit.AMPERE)).thenReturn(-45104.);
        assertEquals(50464., output.getCommercialFlow(cnec1, TWO, Unit.MEGAWATT), DOUBLE_TOLERANCE);
        assertEquals(-45104., output.getCommercialFlow(cnec2, ONE, Unit.AMPERE), DOUBLE_TOLERANCE);

        when(flowResult.getPtdfZonalSum(cnec1, TWO)).thenReturn(0.4);
        when(flowResult.getPtdfZonalSum(cnec2, ONE)).thenReturn(0.75);
        assertEquals(0.4, output.getPtdfZonalSum(cnec1, TWO), DOUBLE_TOLERANCE);
        assertEquals(0.75, output.getPtdfZonalSum(cnec2, ONE), DOUBLE_TOLERANCE);

        when(flowResult.getPtdfZonalSums()).thenReturn(Map.of(cnec1, Map.of(TWO, 0.1), cnec2, Map.of(ONE, 0.2)));
        assertEquals(Map.of(cnec1, Map.of(TWO, 0.1), cnec2, Map.of(ONE, 0.2)), output.getPtdfZonalSums());

        when(rangeActionSetpointResult.getRangeActions()).thenReturn(Set.of(ra1, ra2));
        assertEquals(Set.of(ra1, ra2), output.getRangeActions());

        when(rangeActionSetpointResult.getTap(ra1)).thenReturn(3);
        assertEquals(3, output.getTap(ra1), DOUBLE_TOLERANCE);

        when(rangeActionSetpointResult.getSetpoint(ra1)).thenReturn(15.6);
        assertEquals(15.6, output.getSetpoint(ra1), DOUBLE_TOLERANCE);

        assertEquals(flowResult, output.getFlowResult());
        assertEquals(sensitivityResult, output.getSensitivityResult());
        assertEquals(0, output.getFunctionalCost(), DOUBLE_TOLERANCE);
        assertEquals(0, output.getVirtualCost(), DOUBLE_TOLERANCE);
        assertEquals(0, output.getVirtualCost("mock"), DOUBLE_TOLERANCE);
        assert output.getMostLimitingElements(10).isEmpty();
        assert output.getVirtualCostNames().isEmpty();
        assert output.getCostlyElements("mock", 10).isEmpty();
    }

}
