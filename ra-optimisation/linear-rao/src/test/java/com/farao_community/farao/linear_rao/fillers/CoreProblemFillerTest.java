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
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RemedialActionElementResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.sensitivity.SensitivityComputationResults;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
public class CoreProblemFillerTest extends FillerTest {

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller(linearRaoProblem, linearRaoData);
    }

    @Test
    public void fillWithRangeAction() {
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

        MPVariable variableRangeNegative = linearRaoProblem.getNegativeRangeActionVariable(rangeAction.getId());
        assertNotNull(variableRangeNegative);
        assertEquals(0, variableRangeNegative.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - minAlpha), variableRangeNegative.ub(), 0.01);

        MPVariable variableRangePositive = linearRaoProblem.getPositiveRangeActionVariable(rangeAction.getId());
        assertNotNull(variableRangePositive);
        assertEquals(0, variableRangePositive.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - maxAlpha), variableRangePositive.ub(), 0.01);

        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1.getId());
        assertNotNull(flowVariable);
        assertEquals(-LinearRaoProblem.infinity(), flowVariable.lb(), 0.01);
        assertEquals(LinearRaoProblem.infinity(), flowVariable.ub(), 0.01);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1, flowConstraint.lb(), 0.1);
        assertEquals(referenceFlow1, flowConstraint.ub(), 0.1);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);
        assertEquals(cnec1toRangeSensitivity, flowConstraint.getCoefficient(variableRangeNegative), 0.1);
        assertEquals(-cnec1toRangeSensitivity, flowConstraint.getCoefficient(variableRangePositive), 0.1);

        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2.getId());
        assertNotNull(flowVariable2);
        assertEquals(-LinearRaoProblem.infinity(), flowVariable2.lb(), 0.01);
        assertEquals(LinearRaoProblem.infinity(), flowVariable2.ub(), 0.01);

        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2, flowConstraint2.lb(), 0.1);
        assertEquals(referenceFlow2, flowConstraint2.ub(), 0.1);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), 0.1);
        assertEquals(cnec2toRangeSensitivity, flowConstraint2.getCoefficient(variableRangeNegative), 0.1);
        assertEquals(-cnec2toRangeSensitivity, flowConstraint2.getCoefficient(variableRangePositive), 0.1);
    }

    @Test
    public void updateFiller() {
        final String rangeActionId = "range-action-id";
        final String networkElementId = "network-element-id";
        final int minTap = -10;
        final int maxTap = 16;
        final int currentTap = 5;
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

        final double preOptimAngle = 0.1;
        final double angleChange = 0.3;
        PstElementResult pstElementResult = new PstElementResult("range-action-id", preOptimAngle, 3, preOptimAngle + angleChange, 6);
        List<RemedialActionElementResult> remedialActionElementResultList = new ArrayList<>();
        remedialActionElementResultList.add(pstElementResult);
        RemedialActionResult remedialActionResult = new RemedialActionResult("rem-action-res", "rem-action-res-name", true, remedialActionElementResultList);
        List<RemedialActionResult> remedialActionResultList = new ArrayList<>();
        remedialActionResultList.add(remedialActionResult);

        Map<Cnec, Double> sensitivities = new HashMap<>();
        sensitivities.put(cnec1, cnec1toRangeSensitivity1);
        sensitivities.put(cnec2, cnec2toRangeSensitivity1);

        cnecs.add(cnec1);
        cnecs.add(cnec2);
        RangeActionMock rangeAction = new RangeActionMock(rangeActionId, networkElementId, currentTap, minTap, maxTap, sensitivities);
        when(crac.getRangeAction(Mockito.any())).thenReturn(rangeAction);
        rangeActions.add(rangeAction);

        coreProblemFiller.fill();

        MPVariable variableRangeNegative = linearRaoProblem.getNegativeRangeActionVariable(rangeAction.getId());
        MPVariable variableRangePositive = linearRaoProblem.getPositiveRangeActionVariable(rangeAction.getId());

        double maxNegativeVariation = variableRangeNegative.ub();
        double maxPositiveVariation = variableRangePositive.ub();

        Map<Cnec, Double> sensitivities2 = new HashMap<>();
        sensitivities2.put(cnec1, cnec1toRangeSensitivity2);
        sensitivities2.put(cnec2, cnec2toRangeSensitivity2);
        rangeAction.setSensitivityValues(sensitivities2);
        coreProblemFiller.update(linearRaoProblem, linearRaoData, remedialActionResultList);
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

    }
}
