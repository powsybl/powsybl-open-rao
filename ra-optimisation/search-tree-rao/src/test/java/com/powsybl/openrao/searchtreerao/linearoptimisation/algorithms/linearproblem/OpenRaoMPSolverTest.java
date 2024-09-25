/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-international.com>}
 */
class OpenRaoMPSolverTest {
    static final double DOUBLE_TOLERANCE = 1e-4;

    private OpenRaoMPSolver openRaoMPSolver;
    private MPSolver mpSolver;

    @BeforeEach
    void setUp() {
        openRaoMPSolver = new OpenRaoMPSolver("test", RangeActionsOptimizationParameters.Solver.SCIP);
        mpSolver = openRaoMPSolver.getMpSolver();
    }

    @Test
    void basicTest() {
        assertNotNull(openRaoMPSolver.getObjective());
        assertEquals(RangeActionsOptimizationParameters.Solver.SCIP, openRaoMPSolver.getSolver());
        assertEquals(MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING, openRaoMPSolver.getMpSolver().problemType());

        openRaoMPSolver = new OpenRaoMPSolver("rao_test_prob", RangeActionsOptimizationParameters.Solver.CBC);
        assertEquals(RangeActionsOptimizationParameters.Solver.CBC, openRaoMPSolver.getSolver());
        assertEquals(MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING, openRaoMPSolver.getMpSolver().problemType());
    }

    @Test
    void testAddAndRemoveVariable() {
        String varName = "var1";
        assertEquals(0, openRaoMPSolver.numVariables());

        // Check exception on get before adding variable
        Exception e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.getVariable(varName));
        assertEquals("Variable var1 has not been created yet", e.getMessage());

        // Add variable
        OpenRaoMPVariable var1 = openRaoMPSolver.makeNumVar(-5, 3.6, varName);

        // Check exception when re-adding
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.makeNumVar(-5, 3.6, varName));
        assertEquals("Variable var1 already exists", e.getMessage());

        // Check OpenRaoMPVariable
        assertEquals(varName, var1.name());
        assertTrue(openRaoMPSolver.hasVariable(varName));
        assertEquals(var1, openRaoMPSolver.getVariable(varName));
        assertEquals(1, openRaoMPSolver.numVariables());

        // Check OR-Tools object
        MPVariable orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertNotNull(orToolsVar1);

        checkVarBounds(var1, orToolsVar1, -5, 3.6);

        // Change lb/ub & check
        var1.setLb(-100.);
        var1.setUb(150.7);
        checkVarBounds(var1, orToolsVar1, -100., 150.7);

        var1.setBounds(-98.5, -97.6);
        checkVarBounds(var1, orToolsVar1, -98.5, -97.6);
    }

    private void checkVarBounds(OpenRaoMPVariable raoVar, MPVariable ortoolsVar, double expectedLb, double expectedUb) {
        // OpenRAO object
        assertEquals(expectedLb, raoVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, raoVar.ub(), DOUBLE_TOLERANCE);
        // OR-Tools object
        assertEquals(expectedLb, ortoolsVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, ortoolsVar.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddIntVar() {
        openRaoMPSolver.makeIntVar(5, 10, "var1");
        MPVariable orToolsVar = mpSolver.lookupVariableOrNull("var1");
        assertNotNull(orToolsVar);
        assertEquals(5., orToolsVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(10., orToolsVar.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddBoolVar() {
        openRaoMPSolver.makeBoolVar("var1");
        MPVariable orToolsVar = mpSolver.lookupVariableOrNull("var1");
        assertNotNull(orToolsVar);
        assertEquals(0., orToolsVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., orToolsVar.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddAndRemoveConstraint() {
        String varName = "var1";
        String constName = "const1";

        assertEquals(0, openRaoMPSolver.numConstraints());

        // Add variable
        OpenRaoMPVariable var1 = openRaoMPSolver.makeNumVar(-5, 3.6, varName);

        // Check exception on get before adding constraint
        Exception e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.getConstraint(constName));
        assertEquals("Constraint const1 has not been created yet", e.getMessage());

        // Add constraint & coefficient
        OpenRaoMPConstraint const1 = openRaoMPSolver.makeConstraint(-121.6, 65.956, constName);
        const1.setCoefficient(var1, 648.9);

        // Check exception when re-adding
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.makeConstraint(-121.6, 65.956, constName));
        assertEquals("Constraint const1 already exists", e.getMessage());

        // Check OpenRaoMPConstraint
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));
        assertEquals(648.9, const1.getCoefficient(var1), DOUBLE_TOLERANCE);
        assertEquals(1, openRaoMPSolver.numConstraints());
        assertEquals(constName, const1.name());

        // Check OR-Tools object
        MPConstraint orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertNotNull(orToolsConst1);
        MPVariable orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertEquals(648.9, orToolsConst1.getCoefficient(orToolsVar1), DOUBLE_TOLERANCE);

        checkConstBounds(const1, orToolsConst1, -121.6, 65.956);

        // Change lb/ub & check
        const1.setLb(-100.);
        const1.setUb(150.7);
        checkConstBounds(const1, orToolsConst1, -100., 150.7);

        const1.setBounds(-98.5, -97.6);
        checkConstBounds(const1, orToolsConst1, -98.5, -97.6);

        // Change coef & check
        const1.setCoefficient(var1, 465.9);
        assertEquals(465.9, const1.getCoefficient(var1), DOUBLE_TOLERANCE);
        assertEquals(465.9, orToolsConst1.getCoefficient(orToolsVar1), DOUBLE_TOLERANCE);
    }

    private void checkConstBounds(OpenRaoMPConstraint raoConst, MPConstraint ortoolsConst, double expectedLb, double expectedUb) {
        // OpenRAO object
        assertEquals(expectedLb, raoConst.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, raoConst.ub(), DOUBLE_TOLERANCE);
        // OR-Tools object
        assertEquals(expectedLb, ortoolsConst.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, ortoolsConst.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddConstraintWithNoBounds() {
        String constName = "const1";
        // Add constraint
        OpenRaoMPConstraint const1 = openRaoMPSolver.makeConstraint(constName);

        // Check OpenRaoMPConstraint
        assertEquals(-openRaoMPSolver.infinity(), const1.lb(), openRaoMPSolver.infinity() * 1e-3);
        assertEquals(openRaoMPSolver.infinity(), const1.ub(), openRaoMPSolver.infinity() * 1e-3);
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));

        // Check OR-Tools object
        MPConstraint orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertNotNull(orToolsConst1);
        assertEquals(-openRaoMPSolver.infinity(), orToolsConst1.lb(), openRaoMPSolver.infinity() * 1e-3);
        assertEquals(openRaoMPSolver.infinity(), orToolsConst1.ub(), openRaoMPSolver.infinity() * 1e-3);
    }

    @Test
    void testRounding() {
        double d1 = 1.;

        // big enough deltas are not rounded out by the rounding method
        double eps = 1e-6;
        double d2 = d1 + eps;
        assertNotEquals(OpenRaoMPSolver.roundDouble(d1), OpenRaoMPSolver.roundDouble(d2), 1e-20);

        // small deltas are rounded out as long as we round enough bits
        eps = 1e-15;
        d2 = d1 + eps;
        assertEquals(OpenRaoMPSolver.roundDouble(d1), OpenRaoMPSolver.roundDouble(d2), 1e-20);

        // infinity
        assertEquals(Double.POSITIVE_INFINITY, OpenRaoMPSolver.roundDouble(Double.POSITIVE_INFINITY));
    }

    @Test
    void testRoundingFailsOnNan() {
        Exception e = assertThrows(OpenRaoException.class, () -> OpenRaoMPSolver.roundDouble(Double.NaN));
        assertEquals("Trying to add a NaN value in MIP!", e.getMessage());
    }

    @Test
    void testSetSolverSpecificParametersAsString() {
        assertTrue(openRaoMPSolver.setSolverSpecificParametersAsString(null)); // acceptable
        assertTrue(openRaoMPSolver.setSolverSpecificParametersAsString("parallel/maxnthreads 1, lp/presolving TRUE")); // acceptable SCIP parameters
        assertFalse(openRaoMPSolver.setSolverSpecificParametersAsString("parallel/maxnthreads 1, lp/pre_solving TRUE")); // not acceptable SCIP parameters
    }

    @Test
    void testConvertResultStatus() {
        assertEquals(LinearProblemStatus.OPTIMAL, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.OPTIMAL));
        assertEquals(LinearProblemStatus.ABNORMAL, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.ABNORMAL));
        assertEquals(LinearProblemStatus.FEASIBLE, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.FEASIBLE));
        assertEquals(LinearProblemStatus.UNBOUNDED, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.UNBOUNDED));
        assertEquals(LinearProblemStatus.INFEASIBLE, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.INFEASIBLE));
        assertEquals(LinearProblemStatus.NOT_SOLVED, OpenRaoMPSolver.convertResultStatus(MPSolver.ResultStatus.NOT_SOLVED));
    }

    @Test
    void testObjective() {
        checkObjectiveSense(true); // minimization by default

        openRaoMPSolver.setMaximization();
        checkObjectiveSense(false);

        openRaoMPSolver.setMinimization();
        checkObjectiveSense(true);

        String varName = "var1";
        OpenRaoMPVariable var1 = openRaoMPSolver.makeNumVar(-5, 3.6, varName);

        openRaoMPSolver.getObjective().setCoefficient(var1, 3.5);
        assertEquals(3.5, openRaoMPSolver.getObjective().getCoefficient(var1));
        assertEquals(3.5, mpSolver.objective().getCoefficient(mpSolver.lookupVariableOrNull(varName)));

        openRaoMPSolver.getObjective().setCoefficient(var1, -963.5);
        assertEquals(-963.5, openRaoMPSolver.getObjective().getCoefficient(var1));
        assertEquals(-963.5, mpSolver.objective().getCoefficient(mpSolver.lookupVariableOrNull(varName)));
    }

    private void checkObjectiveSense(boolean minim) {
        // OpenRAO object
        assertEquals(minim, openRaoMPSolver.isMinimization());
        assertEquals(!minim, openRaoMPSolver.isMaximization());
        // OR-Tools object
        assertEquals(minim, mpSolver.objective().minimization());
        assertEquals(!minim, mpSolver.objective().maximization());
    }

    @Test
    void testSolve() {
        // Maximize 2 * x + y
        // such that: x + y <= 10
        //            0 <= x <= 4
        //            0 <= y <= 10
        // Should result in: x = 4, y = 6, obj = 14
        OpenRaoMPVariable x = openRaoMPSolver.makeNumVar(0, 4, "x");
        OpenRaoMPVariable y = openRaoMPSolver.makeNumVar(0, 10, "y");
        OpenRaoMPConstraint constraint = openRaoMPSolver.makeConstraint(-openRaoMPSolver.infinity(), 10, "constraint");
        constraint.setCoefficient(x, 1);
        constraint.setCoefficient(y, 1);
        openRaoMPSolver.getObjective().setCoefficient(x, 2);
        openRaoMPSolver.getObjective().setCoefficient(y, 1);
        openRaoMPSolver.setMaximization();
        LinearProblemStatus result = openRaoMPSolver.solve();

        assertTrue(mpSolver.objective().maximization());
        assertFalse(mpSolver.objective().minimization());
        assertEquals(LinearProblemStatus.OPTIMAL, result);
        assertEquals(4., x.solutionValue(), DOUBLE_TOLERANCE);
        assertEquals(6., y.solutionValue(), DOUBLE_TOLERANCE);

        // Test that after resetting, solver & obj sense is the same
        openRaoMPSolver.resetModel();
        assertNotNull(openRaoMPSolver.getObjective());
        checkObjectiveSense(false);
        assertEquals(RangeActionsOptimizationParameters.Solver.SCIP, openRaoMPSolver.getSolver());
        assertEquals(MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING, openRaoMPSolver.getMpSolver().problemType());
    }

    @Test
    void testInfinity() {
        OpenRaoMPSolver solver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.CBC);
        assertEquals(Double.POSITIVE_INFINITY, solver.infinity());

        solver = new OpenRaoMPSolver("solver", RangeActionsOptimizationParameters.Solver.SCIP);
        assertEquals(1e23, solver.infinity());

        // can't test XPRESS because we need the link to the library
    }

    @Test
    void testRoundSmallValues() {
        assertEquals(1e-5, OpenRaoMPSolver.roundDouble(1e-5), 1e-12);
        assertEquals(1e-6, OpenRaoMPSolver.roundDouble(1e-6), 1e-12);
        assertEquals(0., OpenRaoMPSolver.roundDouble(1e-6 * 0.999), 1e-12);
        assertEquals(0., OpenRaoMPSolver.roundDouble(1e-7), 1e-12);
        assertEquals(0., OpenRaoMPSolver.roundDouble(1e-11), 1e-12);
    }
}
