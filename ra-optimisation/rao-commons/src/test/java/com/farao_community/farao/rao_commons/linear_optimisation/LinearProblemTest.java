/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPSolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MPSolver.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class LinearProblemTest {

    private static final double LB = -11.1;
    private static final double UB = 22.2;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private static final String CNEC_ID = "cnec_id";
    private static final String RANGE_ACTION_ID = "rangeaction_id";

    private LinearProblem linearProblem;
    private Cnec cnec;
    private RangeAction rangeAction;

    @Before
    public void setUp() {
        MPSolver solver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenAnswer(invocationOnMock -> Double.POSITIVE_INFINITY);
        linearProblem = new LinearProblem(solver);

        rangeAction = Mockito.mock(RangeAction.class);
        cnec = Mockito.mock(Cnec.class);

        Mockito.when(rangeAction.getId()).thenReturn(RANGE_ACTION_ID);
        Mockito.when(cnec.getId()).thenReturn(CNEC_ID);
    }

    @Test
    public void flowVariableTest() {
        assertNull(linearProblem.getFlowVariable(cnec));
        linearProblem.addFlowVariable(LB, UB, cnec);
        assertNotNull(linearProblem.getFlowVariable(cnec));
        assertEquals(LB, linearProblem.getFlowVariable(cnec).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getFlowVariable(cnec).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void flowConstraintTest() {
        assertNull(linearProblem.getFlowConstraint(cnec));
        linearProblem.addFlowConstraint(LB, UB, cnec);
        assertNotNull(linearProblem.getFlowConstraint(cnec));
        assertEquals(LB, linearProblem.getFlowConstraint(cnec).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getFlowConstraint(cnec).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionSetPointVariableTest() {
        assertNull(linearProblem.getRangeActionSetPointVariable(rangeAction));
        linearProblem.addRangeActionSetPointVariable(LB, UB, rangeAction);
        assertNotNull(linearProblem.getRangeActionSetPointVariable(rangeAction));
        assertEquals(LB, linearProblem.getRangeActionSetPointVariable(rangeAction).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getRangeActionSetPointVariable(rangeAction).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationVariableTest() {
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction));
        linearProblem.addAbsoluteRangeActionVariationVariable(LB, UB, rangeAction);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationConstraintTest() {
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE));
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, LinearProblem.AbsExtension.NEGATIVE);
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.NEGATIVE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearProblem.AbsExtension.POSITIVE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void minimumMarginConstraintTest() {
        assertNull(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertNull(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        linearProblem.addMinimumMarginConstraint(LB, UB, cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
        linearProblem.addMinimumMarginConstraint(LB, UB, cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD));
        assertNotNull(linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals(LB, linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void minimumMarginVariableTest() {
        assertNull(linearProblem.getMinimumMarginVariable());
        linearProblem.addMinimumMarginVariable(LB, UB);
        assertNotNull(linearProblem.getMinimumMarginVariable());
        assertEquals(LB, linearProblem.getMinimumMarginVariable().lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMinimumMarginVariable().ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void maxLoopFlowConstraintTest() {
        assertNull(linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.UPPER_BOUND));
        assertNull(linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.LOWER_BOUND));

        linearProblem.addMaxLoopFlowConstraint(LB, UB, cnec, LinearProblem.BoundExtension.UPPER_BOUND);
        linearProblem.addMaxLoopFlowConstraint(LB, UB, cnec, LinearProblem.BoundExtension.LOWER_BOUND);

        assertEquals(LB, linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.UPPER_BOUND).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getMaxLoopFlowConstraint(cnec, LinearProblem.BoundExtension.UPPER_BOUND).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void objectiveTest() {
        assertNotNull(linearProblem.getObjective());
    }
}
