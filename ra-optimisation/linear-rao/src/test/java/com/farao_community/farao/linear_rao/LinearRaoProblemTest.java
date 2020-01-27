/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.mocks.MPSolverMock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class LinearRaoProblemTest {
    private MPSolver solver;
    private LinearRaoProblem linearRaoProblem;

    @Before
    public void setUp() {
        solver = new MPSolverMock();
        linearRaoProblem = spy(new LinearRaoProblem(solver));
    }

    @Test
    public void addCnec() {
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);

        MPVariable variable = linearRaoProblem.getSolver().lookupVariableOrNull("cnec-test-variable");
        assertEquals(-800, variable.lb(), 0.1);
        assertEquals(800, variable.ub(), 0.1);

        MPConstraint constraint = linearRaoProblem.getSolver().lookupConstraintOrNull("cnec-test-constraint");
        assertEquals(500, constraint.lb(), 0.1);
        assertEquals(500, constraint.ub(), 0.1);
        assertEquals(1, constraint.getCoefficient(variable), 0.1);

        assertEquals(1, linearRaoProblem.getSolver().numVariables());
        assertEquals(1, linearRaoProblem.getSolver().numConstraints());
    }

    @Test
    public void addRangeActionVariable() {
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);

        MPVariable positiveVariable = linearRaoProblem.getSolver().lookupVariableOrNull("positive-range-action-test-network-element-test-variable");
        assertEquals(0, positiveVariable.lb(), 0.1);
        assertEquals(15, positiveVariable.ub(), 0.1);

        MPVariable negativeVariable = linearRaoProblem.getSolver().lookupVariableOrNull("negative-range-action-test-network-element-test-variable");
        assertEquals(0, negativeVariable.lb(), 0.1);
        assertEquals(12, negativeVariable.ub(), 0.1);

        assertEquals(1, linearRaoProblem.getNegativeRangeActionVariables().size());
        assertEquals(1, linearRaoProblem.getPositiveRangeActionVariables().size());
    }

    @Test
    public void addRangeActionFlowOnBranch() {
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);
        linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);

        MPConstraint constraint = linearRaoProblem.getSolver().lookupConstraintOrNull("cnec-test-constraint");
        MPVariable positiveVariable = linearRaoProblem.getSolver().lookupVariableOrNull("positive-range-action-test-network-element-test-variable");
        MPVariable negativeVariable = linearRaoProblem.getSolver().lookupVariableOrNull("negative-range-action-test-network-element-test-variable");

        assertEquals(-0.2, constraint.getCoefficient(positiveVariable), 0.01);
        assertEquals(0.2, constraint.getCoefficient(negativeVariable), 0.01);
    }

    @Test
    public void addRangeActionFlowOnBranchWithCnecFailure() {
        linearRaoProblem.addRangeActionVariable("range-action-test", "network-element-test", 12, 15);
        try {
            linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);
        } catch (FaraoException e) {
            assertEquals("Flow variable on cnec-test has not been defined yet.", e.getMessage());
        }
    }

    @Test
    public void addRangeActionFlowOnBranchWithRangeActionFailure() {
        linearRaoProblem.addCnec("cnec-test", 500, -800, 800);
        try {
            linearRaoProblem.addRangeActionFlowOnBranch("cnec-test", "range-action-test", "network-element-test", 0.2);
        } catch (FaraoException e) {
            assertEquals("Range action variable for range-action-test on network-element-test has not been defined yet.", e.getMessage());
        }
    }
}
