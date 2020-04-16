package com.farao_community.farao.linear_rao.optimisation;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPSolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
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
public class LinearRaoProblemTest {

    private static final double LB = -11.1;
    private static final double UB = 22.2;
    private static final double DOUBLE_TOLERANCE = 0.1;

    private static final String CNEC_ID = "cnec_id";
    private static final String RANGE_ACTION_ID = "rangeaction_id";

    private LinearRaoProblem linearRaoProblem;
    private Cnec cnec;
    private RangeAction rangeAction;

    @Before
    public void setUp() {
        MPSolver solver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenReturn(Double.POSITIVE_INFINITY);
        linearRaoProblem = new LinearRaoProblem(solver);

        rangeAction = Mockito.mock(RangeAction.class);
        cnec = Mockito.mock(Cnec.class);

        Mockito.when(rangeAction.getId()).thenReturn(RANGE_ACTION_ID);
        Mockito.when(cnec.getId()).thenReturn(CNEC_ID);
    }

    @Test
    public void flowVariableTest() {
        assertNull(linearRaoProblem.getFlowVariable(cnec));
        linearRaoProblem.addFlowVariable(LB, UB, cnec);
        assertNotNull(linearRaoProblem.getFlowVariable(cnec));
        assertEquals(LB, linearRaoProblem.getFlowVariable(cnec).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getFlowVariable(cnec).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void flowConstraintTest() {
        assertNull(linearRaoProblem.getFlowConstraint(cnec));
        linearRaoProblem.addFlowConstraint(LB, UB, cnec);
        assertNotNull(linearRaoProblem.getFlowConstraint(cnec));
        assertEquals(LB, linearRaoProblem.getFlowConstraint(cnec).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getFlowConstraint(cnec).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionSetPointVariableTest() {
        assertNull(linearRaoProblem.getRangeActionSetPointVariable(rangeAction));
        linearRaoProblem.addRangeActionSetPointVariable(LB, UB, rangeAction);
        assertNotNull(linearRaoProblem.getRangeActionSetPointVariable(rangeAction));
        assertEquals(LB, linearRaoProblem.getRangeActionSetPointVariable(rangeAction).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getRangeActionSetPointVariable(rangeAction).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationVariableTest() {
        assertNull(linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction));
        linearRaoProblem.addAbsoluteRangeActionVariationVariable(LB, UB, rangeAction);
        assertNotNull(linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction));
        assertEquals(LB, linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void rangeActionAbsoluteVariationConstraintTest() {
        assertNull(linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE));
        assertNull(linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE));
        linearRaoProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE);
        linearRaoProblem.addAbsoluteRangeActionVariationConstraint(LB, UB, rangeAction, LinearRaoProblem.AbsExtension.POSITIVE);
        assertNotNull(linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE));
        assertNotNull(linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE));
        assertEquals(LB, linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void minimumMarginConstraintTest() {
        assertNull(linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD));
        assertNull(linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD));
        linearRaoProblem.addMinimumMarginConstraint(LB, UB, cnec, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD);
        linearRaoProblem.addMinimumMarginConstraint(LB, UB, cnec, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD);
        assertNotNull(linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD));
        assertNotNull(linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD));
        assertEquals(LB, linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD).lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getMinimumMarginConstraint(cnec, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD).ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void minimumMarginVariableTest() {
        assertNull(linearRaoProblem.getMinimumMarginVariable());
        linearRaoProblem.addMinimumMarginVariable(LB, UB);
        assertNotNull(linearRaoProblem.getMinimumMarginVariable());
        assertEquals(LB, linearRaoProblem.getMinimumMarginVariable().lb(), DOUBLE_TOLERANCE);
        assertEquals(UB, linearRaoProblem.getMinimumMarginVariable().ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void maxLoopFlowConstraintTest() {
        assertNull(linearRaoProblem.getMaxLoopFlowConstraintPositiveViolation(cnec));
        linearRaoProblem.addMaxLoopFlowConstraintPositiveViolation(LB, UB, cnec);
        assertNotNull(linearRaoProblem.getMaxLoopFlowConstraintPositiveViolation(cnec));
        assertNull(linearRaoProblem.getMaxLoopFlowConstraintNegativeViolation(cnec));
        linearRaoProblem.addMaxLoopFlowConstraintNegativeViolation(LB, UB, cnec);
        assertNotNull(linearRaoProblem.getMaxLoopFlowConstraintNegativeViolation(cnec));
    }

    @Test
    public void objectiveTest() {
        assertNotNull(linearRaoProblem.getObjective());
    }
}
