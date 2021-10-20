/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPSolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem.VariationExtension.DOWNWARD;
import static com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem.VariationExtension.UPWARD;
import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblemTest {

    private static final double LB = -11.1;
    private static final double UB = 22.2;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private static final String CNEC_ID = "cnec_id";
    private static final String RANGE_ACTION_ID = "rangeaction_id";
    private static final String GROUP_ID = "group_id";

    private LinearProblem linearProblem;
    private FlowCnec cnec;
    private PstRangeAction rangeAction;

    @Before
    public void setUp() {
        MPSolver solver = new MPSolverMock();
        linearProblem = new LinearProblem(Collections.emptyList(), solver);

        rangeAction = Mockito.mock(PstRangeAction.class);
        cnec = Mockito.mock(FlowCnec.class);

        Mockito.when(rangeAction.getId()).thenReturn(RANGE_ACTION_ID);
        Mockito.when(rangeAction.getGroupId()).thenReturn(Optional.of(GROUP_ID));
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
        assertNull(linearProblem.getRangeActionSetpointVariable(rangeAction));
        linearProblem.addRangeActionSetpointVariable(LB, UB, rangeAction);
        assertNotNull(linearProblem.getRangeActionSetpointVariable(rangeAction));
        assertEquals(LB, linearProblem.getRangeActionSetpointVariable(rangeAction).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getRangeActionSetpointVariable(rangeAction).ub(), DOUBLE_TOLERANCE);
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
    public void pstTapVariationIntegerAndBinaryVariablesTest() {
        assertNull(linearProblem.getPstTapVariationVariable(rangeAction, UPWARD));
        assertNull(linearProblem.getPstTapVariationVariable(rangeAction, DOWNWARD));
        assertNull(linearProblem.getPstTapVariationBinary(rangeAction, UPWARD));
        assertNull(linearProblem.getPstTapVariationBinary(rangeAction, DOWNWARD));

        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, UPWARD);
        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, DOWNWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, UPWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, DOWNWARD);

        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, DOWNWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, DOWNWARD));
        assertEquals(LB, linearProblem.getPstTapVariationVariable(rangeAction, UPWARD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstTapVariationVariable(rangeAction, DOWNWARD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void pstTapConstraintsTest() {
        assertNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, UPWARD));
        assertNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, DOWNWARD));
        assertNull(linearProblem.getUpOrDownPstVariationConstraint(rangeAction));
        assertNull(linearProblem.getTapToAngleConversionConstraint(rangeAction));

        linearProblem.addIsVariationInDirectionConstraint(rangeAction, UPWARD);
        linearProblem.addIsVariationInDirectionConstraint(rangeAction, DOWNWARD);
        linearProblem.addUpOrDownPstVariationConstraint(rangeAction);
        linearProblem.addTapToAngleConversionConstraint(LB, UB, rangeAction);

        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, UPWARD));
        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, DOWNWARD));
        assertNotNull(linearProblem.getUpOrDownPstVariationConstraint(rangeAction));
        assertNotNull(linearProblem.getTapToAngleConversionConstraint(rangeAction));
    }

    @Test
    public void pstGroupVariablesAndConstraintsTest() {
        assertNull(linearProblem.getPstGroupTapVariable(GROUP_ID));
        assertNull(linearProblem.getPstGroupTapConstraint(rangeAction));

        linearProblem.addPstGroupTapVariable(LB, UB, GROUP_ID);
        linearProblem.addPstGroupTapConstraint(LB, UB, rangeAction);

        assertNotNull(linearProblem.getPstGroupTapVariable(GROUP_ID));
        assertNotNull(linearProblem.getPstGroupTapConstraint(rangeAction));
        assertEquals(LB, linearProblem.getPstGroupTapVariable(GROUP_ID).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstGroupTapConstraint(rangeAction).ub(), DOUBLE_TOLERANCE);
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
