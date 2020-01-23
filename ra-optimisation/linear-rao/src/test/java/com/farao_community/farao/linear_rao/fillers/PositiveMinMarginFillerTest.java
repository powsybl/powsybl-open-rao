/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PositiveMinMarginFillerTest extends FillerTest {

    private PositiveMinMarginFiller positiveMinMarginFiller;

    @Before
    public void setUp() {
        init();
        coreProblemFiller = new CoreProblemFiller();
        positiveMinMarginFiller = new PositiveMinMarginFiller();
    }

    @Test
    public void fillWithRangeAction() {
        initRangeAction();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable variableNegative = linearRaoProblem.getNegativePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variableNegative);
        assertEquals(0, variableNegative.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - minAlpha), variableNegative.ub(), 0.01);

        MPVariable variablePositive = linearRaoProblem.getPositivePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variablePositive);
        assertEquals(0, variablePositive.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - maxAlpha), variablePositive.ub(), 0.01);
    }

    @Test
    public void fillWithCnec() throws SynchronizationException {
        initCnec();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1.getId());
        assertNotNull(flowVariable);
        assertEquals(-Double.MAX_VALUE, flowVariable.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable.ub(), 0.01);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1, flowConstraint.lb(), 0.1);
        assertEquals(referenceFlow1, flowConstraint.ub(), 0.1);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);

        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2.getId());
        assertNotNull(flowVariable2);
        assertEquals(-Double.MAX_VALUE, flowVariable2.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable2.ub(), 0.01);

        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2, flowConstraint2.lb(), 0.1);
        assertEquals(referenceFlow2, flowConstraint2.ub(), 0.1);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), 0.1);
    }

    @Test
    public void fillRaAndCnec() throws SynchronizationException {
        initBoth();
        coreProblemFiller.fill(linearRaoProblem, linearRaoData);
        positiveMinMarginFiller.fill(linearRaoProblem, linearRaoData);

        MPVariable variableNegative = linearRaoProblem.getNegativePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variableNegative);
        assertEquals(0, variableNegative.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - minAlpha), variableNegative.ub(), 0.01);

        MPVariable variablePositive = linearRaoProblem.getPositivePstShiftVariable(rangeAction.getId(), networkElement.getId());
        assertNotNull(variablePositive);
        assertEquals(0, variablePositive.lb(), 0.01);
        assertEquals(Math.abs(currentAlpha - maxAlpha), variablePositive.ub(), 0.01);

        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec1.getId());
        assertNotNull(flowVariable);
        assertEquals(-Double.MAX_VALUE, flowVariable.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable.ub(), 0.01);

        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec1.getId());
        assertNotNull(flowConstraint);
        assertEquals(referenceFlow1, flowConstraint.lb(), 0.1);
        assertEquals(referenceFlow1, flowConstraint.ub(), 0.1);
        assertEquals(1, flowConstraint.getCoefficient(flowVariable), 0.1);

        MPVariable flowVariable2 = linearRaoProblem.getFlowVariable(cnec2.getId());
        assertNotNull(flowVariable2);
        assertEquals(-Double.MAX_VALUE, flowVariable2.lb(), 0.01);
        assertEquals(Double.MAX_VALUE, flowVariable2.ub(), 0.01);

        MPConstraint flowConstraint2 = linearRaoProblem.getFlowConstraint(cnec2.getId());
        assertNotNull(flowConstraint2);
        assertEquals(referenceFlow2, flowConstraint2.lb(), 0.1);
        assertEquals(referenceFlow2, flowConstraint2.ub(), 0.1);
        assertEquals(1, flowConstraint2.getCoefficient(flowVariable2), 0.1);
    }
}
