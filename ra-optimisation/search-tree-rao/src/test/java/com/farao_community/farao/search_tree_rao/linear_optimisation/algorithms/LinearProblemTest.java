/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPSolver;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemBuilder;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.mocks.MPSolverMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem.VariationDirectionExtension.*;
import static com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem.VariationReferenceExtension.*;
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
    private State state;
    private PstRangeAction rangeAction;

    @Before
    public void setUp() {
        FaraoMPSolver solver = new MPSolverMock();
        linearProblem = new LinearProblemBuilder().withSolver(solver).build();

        rangeAction = Mockito.mock(PstRangeAction.class);
        cnec = Mockito.mock(FlowCnec.class);
        state = Mockito.mock(State.class);

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
        assertNull(linearProblem.getRangeActionSetpointVariable(rangeAction, state));
        linearProblem.addRangeActionSetpointVariable(LB, UB, rangeAction, state);
        assertNotNull(linearProblem.getRangeActionSetpointVariable(rangeAction, state));
        assertEquals(LB, linearProblem.getRangeActionSetpointVariable(rangeAction, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getRangeActionSetpointVariable(rangeAction, state).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationVariableTest() {
        assertNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state));
        linearProblem.addAbsoluteRangeActionVariationVariable(LB, UB, rangeAction, state);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction, state).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationConstraintTest() {
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE));
        assertNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE));
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, state, LinearProblem.AbsExtension.NEGATIVE);
        linearProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, state, LinearProblem.AbsExtension.POSITIVE);
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE));
        assertEquals(LB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.NEGATIVE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, state, LinearProblem.AbsExtension.POSITIVE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void pstTapVariationIntegerAndBinaryVariablesTest() {
        assertNull(linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD));
        assertNull(linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD));
        assertNull(linearProblem.getPstTapVariationBinary(rangeAction, state, UPWARD));
        assertNull(linearProblem.getPstTapVariationBinary(rangeAction, state, DOWNWARD));

        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, state, UPWARD);
        linearProblem.addPstTapVariationVariable(LB, UB, rangeAction, state, DOWNWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, state, UPWARD);
        linearProblem.addPstTapVariationBinary(rangeAction, state, DOWNWARD);

        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, state, UPWARD));
        assertNotNull(linearProblem.getPstTapVariationBinary(rangeAction, state, DOWNWARD));
        assertEquals(LB, linearProblem.getPstTapVariationVariable(rangeAction, state, UPWARD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstTapVariationVariable(rangeAction, state, DOWNWARD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void pstTapConstraintsTest() {
        assertNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, UPWARD));
        assertNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, DOWNWARD));
        assertNull(linearProblem.getUpOrDownPstVariationConstraint(rangeAction, state));
        assertNull(linearProblem.getTapToAngleConversionConstraint(rangeAction, state));

        linearProblem.addIsVariationInDirectionConstraint(-Double.MAX_VALUE, 0, rangeAction, state, PREVIOUS_ITERATION, UPWARD);
        linearProblem.addIsVariationInDirectionConstraint(-Double.MAX_VALUE, 0, rangeAction, state, PREVIOUS_ITERATION, DOWNWARD);
        linearProblem.addUpOrDownPstVariationConstraint(rangeAction, state);
        linearProblem.addTapToAngleConversionConstraint(LB, UB, rangeAction, state);

        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, UPWARD));
        assertNotNull(linearProblem.getIsVariationInDirectionConstraint(rangeAction, state, PREVIOUS_ITERATION, DOWNWARD));
        assertNotNull(linearProblem.getUpOrDownPstVariationConstraint(rangeAction, state));
        assertNotNull(linearProblem.getTapToAngleConversionConstraint(rangeAction, state));
    }

    @Test
    public void pstGroupVariablesAndConstraintsTest() {
        assertNull(linearProblem.getPstGroupTapVariable(GROUP_ID, state));
        assertNull(linearProblem.getPstGroupTapConstraint(rangeAction, state));

        linearProblem.addPstGroupTapVariable(LB, UB, GROUP_ID, state);
        linearProblem.addPstGroupTapConstraint(LB, UB, rangeAction, state);

        assertNotNull(linearProblem.getPstGroupTapVariable(GROUP_ID, state));
        assertNotNull(linearProblem.getPstGroupTapConstraint(rangeAction, state));
        assertEquals(LB, linearProblem.getPstGroupTapVariable(GROUP_ID, state).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearProblem.getPstGroupTapConstraint(rangeAction, state).ub(), DOUBLE_TOLERANCE);
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
    public void minimumRelativeMarginSignBinaryVariableTest() {
        assertNull(linearProblem.getMinimumRelativeMarginSignBinaryVariable());
        linearProblem.addMinimumRelativeMarginSignBinaryVariable();
        assertNotNull(linearProblem.getMinimumRelativeMarginSignBinaryVariable());
    }

    @Test
    public void minimumRelMarginSignDefinitionConstraintTest() {
        assertNull(linearProblem.getMinimumRelMarginSignDefinitionConstraint());
        linearProblem.addMinimumRelMarginSignDefinitionConstraint(LB, UB);
        assertNotNull(linearProblem.getMinimumRelMarginSignDefinitionConstraint());
    }

    @Test
    public void minimumRelMarginSetToZeroConstraintTest() {
        assertNull(linearProblem.getMinimumRelMarginSetToZeroConstraint());
        linearProblem.addMinimumRelMarginSetToZeroConstraint(LB, UB);
        assertNotNull(linearProblem.getMinimumRelMarginSetToZeroConstraint());
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
