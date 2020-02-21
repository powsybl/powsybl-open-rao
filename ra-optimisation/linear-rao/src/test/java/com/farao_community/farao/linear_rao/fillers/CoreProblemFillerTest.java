/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.linear_rao.mocks.CnecMock;
import com.farao_community.farao.linear_rao.mocks.RangeActionMock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.SensitivityComputationResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class CoreProblemFillerTest extends FillerTest {

    private static final double DOUBLE_TOLERANCE = 0.2;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData);
    }

    @Test
    public void fillTest() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final double minAlpha = -0.5;
        final double maxAlpha = 0.8;
        final double currentAlpha = 0.2;
        final double referenceFlow1 = 500.;
        final double referenceFlow2 = 300.;
        final double cnec1toRangeSensitivity = 0.2;
        final double cnec2toRangeSensitivity = 0.5;

        Cnec cnec1 = new CnecMock("cnec1-id", 0, 800);
        Cnec cnec2 = new CnecMock("cnec2-id", 0, 800);
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow1);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow2);

        SensitivityComputationResults sensitivityComputationResults = mock(SensitivityComputationResults.class);
        when(linearRaoData.getSensitivityComputationResults(preventiveState)).thenReturn(sensitivityComputationResults);
        Map<Cnec, Double> sensitivities = new HashMap<>();
        sensitivities.put(cnec1, cnec1toRangeSensitivity);
        sensitivities.put(cnec2, cnec2toRangeSensitivity);

        cnecs.add(cnec1);
        cnecs.add(cnec2);
        RangeAction rangeAction = new RangeActionMock(rangeActionId, networkElementId, currentAlpha, minAlpha, maxAlpha, sensitivities);
        rangeActions.add(rangeAction);

        coreProblemFiller.fill();

        // check range action setpoint variable
        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
        assertNotNull(setPointVariable);
        //assertEquals(currentAlpha - Math.abs(minAlpha), setPointVariable.lb(), DOUBLE_TOLERANCE);
        //assertEquals(currentAlpha + maxAlpha, setPointVariable.ub(), DOUBLE_TOLERANCE);

        // check range action absolute variation variable
        MPVariable absoluteVariationVariable = linearRaoProblem.getAboluteRangeActionVariationVariable(rangeAction);
        assertNotNull(absoluteVariationVariable);
        assertEquals(0, absoluteVariationVariable.lb(), 0.01);
        assertEquals(Double.POSITIVE_INFINITY, absoluteVariationVariable.ub(), DOUBLE_TOLERANCE);

        // check flow variable for cnec1
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1);
        assertNotNull(flowVariable);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec1
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1);
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.lb(), DOUBLE_TOLERANCE);
        assertEquals(referenceFlow1 - currentAlpha * cnec1toRangeSensitivity , flowConstraint.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(-cnec1toRangeSensitivity, flowConstraint.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);

        // check flow variable for cnec2
        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2);
        assertNotNull(flowVariable2);
        assertEquals(-Double.POSITIVE_INFINITY, flowVariable2.lb(), DOUBLE_TOLERANCE);
        assertEquals(Double.POSITIVE_INFINITY, flowVariable2.ub(), DOUBLE_TOLERANCE);

        // check flow constraint for cnec2
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2);
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.lb(), DOUBLE_TOLERANCE);
        assertEquals(referenceFlow2 - currentAlpha * cnec2toRangeSensitivity, flowConstraint2.ub(), DOUBLE_TOLERANCE);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), DOUBLE_TOLERANCE);
        assertEquals(-cnec2toRangeSensitivity, flowConstraint2.getCoefficient(setPointVariable), DOUBLE_TOLERANCE);
    }

    @Test
    public void updateFiller() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final double minAngle = -2.6;
        final double maxAngle = 4.2;
        final double initialAngle = 0.2;
        final double referenceFlow11 = 500.;
        final double referenceFlow12 = 600.;
        final double referenceFlow21 = 300.;
        final double referenceFlow22 = 250.;
        final double cnec1toRangeSensitivity1 = 0.2;
        final double cnec1toRangeSensitivity2 = 0.3;
        final double cnec2toRangeSensitivity1 = 0.5;
        final double cnec2toRangeSensitivity2 = 0.4;
        Cnec cnec1 = new CnecMock("cnec1-id", 0, 800);
        Cnec cnec2 = new CnecMock("cnec2-id", 0, 800);
        when(linearRaoData.getReferenceFlow(cnec1)).thenReturn(referenceFlow11, referenceFlow12);
        when(linearRaoData.getReferenceFlow(cnec2)).thenReturn(referenceFlow21, referenceFlow22);

        final double angleChange = 0.3;
        List<String> activatedRemedialActionIds = Collections.singletonList(rangeActionId);

        Map<Cnec, Double> sensitivities = new HashMap<>();
        sensitivities.put(cnec1, cnec1toRangeSensitivity1);
        sensitivities.put(cnec2, cnec2toRangeSensitivity1);

        cnecs.add(cnec1);
        cnecs.add(cnec2);
        RangeActionMock rangeAction = new RangeActionMock(rangeActionId, networkElementId, initialAngle, minAngle, maxAngle, sensitivities);
        when(crac.getRangeAction(Mockito.any())).thenReturn(rangeAction);
        rangeActions.add(rangeAction);
/**
        coreProblemFiller.fill();

        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
        MPVariable variableRangePositive = linearRaoProblem.getPositiveRangeActionVariable(rangeAction.getId());

        double maxNegativeVariation = variableRangeNegative.ub();
        double maxPositiveVariation = variableRangePositive.ub();

        Map<Cnec, Double> sensitivities2 = new HashMap<>();
        sensitivities2.put(cnec1, cnec1toRangeSensitivity2);
        sensitivities2.put(cnec2, cnec2toRangeSensitivity2);
        rangeAction.setSensitivityValues(sensitivities2);
        rangeAction.setCurrentValue(initialAngle + angleChange);
        coreProblemFiller.update(activatedRemedialActionIds);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);

        assertEquals(0.3, flowConstraint.getCoefficient(variableRangeNegative), 0.1);
        assertEquals(-0.3, flowConstraint.getCoefficient(variableRangePositive), 0.1);
        assertEquals(0.4, flowConstraint2.getCoefficient(variableRangeNegative), 0.1);
        assertEquals(-0.4, flowConstraint2.getCoefficient(variableRangePositive), 0.1);

        assertEquals(600., flowConstraint.lb(), 0.1);
        assertEquals(600., flowConstraint.ub(), 0.1);
        assertEquals(250., flowConstraint2.lb(), 0.1);
        assertEquals(250., flowConstraint2.ub(), 0.1);

        assertEquals(maxNegativeVariation + angleChange, variableRangeNegative.ub(), 0.01);
        assertEquals(maxPositiveVariation - angleChange, variableRangePositive.ub(), 0.01);
 */
    }
}