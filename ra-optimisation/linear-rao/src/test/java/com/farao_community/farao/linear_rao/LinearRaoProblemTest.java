package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MPSolver.class)
public class LinearRaoProblemTest {
    private MPSolver solver;
    private LinearRaoProblem linearRaoProblem;

    @Before
    public void setUp() {
        solver = new MPSolverMock();
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenReturn(Double.MAX_VALUE);
        linearRaoProblem = new LinearRaoProblem(solver);
    }

    @Test
    public void addCnec() {
        String cnecId = "cnec-test";
        linearRaoProblem.addCnec(cnecId, 500);

        MPVariable variable = linearRaoProblem.getFlowVariable(cnecId);
        assertEquals(-LinearRaoProblem.infinity(), variable.lb(), 0.1);
        assertEquals(LinearRaoProblem.infinity(), variable.ub(), 0.1);

        MPConstraint constraint = linearRaoProblem.getFlowConstraint(cnecId);
        assertEquals(500, constraint.lb(), 0.1);
        assertEquals(500, constraint.ub(), 0.1);
        assertEquals(1, constraint.getCoefficient(variable), 0.1);

        assertEquals(1, linearRaoProblem.getFlowVariables().size());
        assertEquals(1, linearRaoProblem.getFlowConstraints().size());
    }

    @Test
    public void addRangeActionVariable() {
        String rangeActionId = "range-action-test";
        String networkElementId = "network-element-test";
        linearRaoProblem.addRangeActionVariable(rangeActionId, 12, 15);

        MPVariable positiveVariable = linearRaoProblem.getPositiveRangeActionVariable(rangeActionId);
        assertEquals(0, positiveVariable.lb(), 0.1);
        assertEquals(15, positiveVariable.ub(), 0.1);

        MPVariable negativeVariable = linearRaoProblem.getNegativeRangeActionVariable(rangeActionId);
        assertEquals(0, negativeVariable.lb(), 0.1);
        assertEquals(12, negativeVariable.ub(), 0.1);

        assertEquals(1, linearRaoProblem.getNegativeRangeActionVariables().size());
        assertEquals(1, linearRaoProblem.getPositiveRangeActionVariables().size());
    }

    @Test
    public void addRangeActionFlowOnBranch() {
        String cnecId = "cnec-test";
        String rangeActionId = "range-action-test";
        String networkElementId = "network-element-test";
        linearRaoProblem.addCnec(cnecId, 500);
        linearRaoProblem.addRangeActionVariable(rangeActionId, 12, 15);
        linearRaoProblem.updateFlowConstraintsWithRangeAction(cnecId, rangeActionId, 0.2);

        MPConstraint constraint = linearRaoProblem.getFlowConstraint(cnecId);
        MPVariable positiveVariable = linearRaoProblem.getPositiveRangeActionVariable(rangeActionId);
        MPVariable negativeVariable = linearRaoProblem.getNegativeRangeActionVariable(rangeActionId);

        assertEquals(-0.2, constraint.getCoefficient(positiveVariable), 0.01);
        assertEquals(0.2, constraint.getCoefficient(negativeVariable), 0.01);
    }

    @Test
    public void addRangeActionFlowOnBranchWithCnecFailure() {
        String cnecId = "cnec-test";
        String rangeActionId = "range-action-test";
        String networkElementId = "network-element-test";
        linearRaoProblem.addRangeActionVariable(rangeActionId, 12, 15);
        try {
            linearRaoProblem.updateFlowConstraintsWithRangeAction(cnecId, rangeActionId, 0.2);
        } catch (FaraoException e) {
            assertEquals("Flow variable on cnec-test has not been defined yet.", e.getMessage());
        }
    }

    @Test
    public void addRangeActionFlowOnBranchWithRangeActionFailure() {
        String cnecId = "cnec-test";
        String rangeActionId = "range-action-test";
        linearRaoProblem.addCnec(cnecId, 500);
        try {
            linearRaoProblem.updateFlowConstraintsWithRangeAction(cnecId, rangeActionId, 0.2);
        } catch (FaraoException e) {
            assertEquals("Range action variable for range-action-test has not been defined yet.", e.getMessage());
        }
    }

    @Test
    public void updateReferenceFlow() {
        String cnecId = "cnec-test";
        linearRaoProblem.addCnec(cnecId, 500);
        linearRaoProblem.updateReferenceFlow(cnecId, 400);

        MPVariable variable = linearRaoProblem.getFlowVariable(cnecId);
        assertEquals(-LinearRaoProblem.infinity(), variable.lb(), 0.1);
        assertEquals(LinearRaoProblem.infinity(), variable.ub(), 0.1);

        MPConstraint constraint = linearRaoProblem.getFlowConstraint(cnecId);
        assertEquals(400, constraint.lb(), 0.1);
        assertEquals(400, constraint.ub(), 0.1);
        assertEquals(1, constraint.getCoefficient(variable), 0.1);

        assertEquals(1, linearRaoProblem.getFlowVariables().size());
        assertEquals(1, linearRaoProblem.getFlowConstraints().size());
    }

    @Test
    public void updateRangeActionBounds() {
        String rangeActionId = "range-action-test";
        linearRaoProblem.addRangeActionVariable(rangeActionId, 12, 15);

        linearRaoProblem.updateRangeActionBounds(rangeActionId, 16, 11);
        MPVariable positiveVariable = linearRaoProblem.getPositiveRangeActionVariable(rangeActionId);
        assertEquals(0, positiveVariable.lb(), 0.1);
        assertEquals(11, positiveVariable.ub(), 0.1);
        MPVariable negativeVariable = linearRaoProblem.getNegativeRangeActionVariable(rangeActionId);
        assertEquals(0, negativeVariable.lb(), 0.1);
        assertEquals(16, negativeVariable.ub(), 0.1);

        linearRaoProblem.updateRangeActionBounds(rangeActionId, 10, 17);
        assertEquals(0, positiveVariable.lb(), 0.1);
        assertEquals(17, positiveVariable.ub(), 0.1);
        assertEquals(0, negativeVariable.lb(), 0.1);
        assertEquals(10, negativeVariable.ub(), 0.1);
    }
}
