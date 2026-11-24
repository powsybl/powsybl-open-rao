/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.mathopt.LinearConstraint;
import com.google.ortools.mathopt.SolveResult;
import com.google.ortools.mathopt.SolverType;
import com.google.ortools.mathopt.Variable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoimpl.result.api.LinearProblemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.ortools.mathopt.SolveResult.TerminationReason.*;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver.CBC;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver.GUROBI;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver.HIGHS;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver.SCIP;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.Solver.XPRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-international.com>}
 */
class OpenRaoMPSolverTest {
    static final double DOUBLE_TOLERANCE = 1e-4;

    private OpenRaoMPSolver openRaoMPSolver;

    @BeforeEach
    void setUp() {
        openRaoMPSolver = new OpenRaoMPSolver("test", SCIP);
    }

    @Test
    void basicTest() {
        assertNotNull(openRaoMPSolver.getObjective());
        assertEquals(SCIP, openRaoMPSolver.getSolver());
        assertEquals(SolverType.GSCIP, OpenRaoMPSolver.getOrToolsSolverType(SCIP));

        openRaoMPSolver = new OpenRaoMPSolver("rao_test_prob", XPRESS);
        assertEquals(XPRESS, openRaoMPSolver.getSolver());
        assertEquals(SolverType.XPRESS, OpenRaoMPSolver.getOrToolsSolverType(XPRESS));
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
        Variable orToolsVar1 = openRaoMPSolver.getVariable(varName).getVariable();
        assertNotNull(orToolsVar1);

        checkVarBounds(var1, orToolsVar1, -5, 3.6);

        // Change lb/ub & check
        var1.setLb(-100.);
        var1.setUb(150.7);
        checkVarBounds(var1, orToolsVar1, -100., 150.7);

        var1.setBounds(-98.5, -97.6);
        checkVarBounds(var1, orToolsVar1, -98.5, -97.6);
    }

    private void checkVarBounds(OpenRaoMPVariable raoVar, Variable ortoolsVar, double expectedLb, double expectedUb) {
        // OpenRAO object
        assertEquals(expectedLb, raoVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, raoVar.ub(), DOUBLE_TOLERANCE);
        // OR-Tools object
        assertEquals(expectedLb, ortoolsVar.getLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, ortoolsVar.getUpperBound(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddIntVar() {
        openRaoMPSolver.makeIntVar(5, 10, "var1");
        Variable orToolsVar = openRaoMPSolver.getVariable("var1").getVariable();
        assertNotNull(orToolsVar);
        assertEquals(5., orToolsVar.getLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(10., orToolsVar.getUpperBound(), DOUBLE_TOLERANCE);
        assertTrue(orToolsVar.isInteger());
    }

    @Test
    void testAddBoolVar() {
        openRaoMPSolver.makeBoolVar("var1");
        Variable orToolsVar = openRaoMPSolver.getVariable("var1").getVariable();
        assertNotNull(orToolsVar);
        assertEquals(0., orToolsVar.getLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(1., orToolsVar.getUpperBound(), DOUBLE_TOLERANCE);
        assertTrue(orToolsVar.isInteger());
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
        LinearConstraint orToolsConst1 = openRaoMPSolver.getConstraint(constName).getLinearConstraint();
        assertNotNull(orToolsConst1);
        Variable orToolsVar1 = openRaoMPSolver.getVariable(varName).getVariable();
        assertEquals(648.9, orToolsConst1.getTerm(orToolsVar1), DOUBLE_TOLERANCE);

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
        assertEquals(465.9, orToolsConst1.getTerm(orToolsVar1), DOUBLE_TOLERANCE);
    }

    private void checkConstBounds(OpenRaoMPConstraint raoConst, LinearConstraint ortoolsConst, double expectedLb, double expectedUb) {
        // OpenRAO object
        assertEquals(expectedLb, raoConst.lb(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, raoConst.ub(), DOUBLE_TOLERANCE);
        // OR-Tools object
        assertEquals(expectedLb, ortoolsConst.getLowerBound(), DOUBLE_TOLERANCE);
        assertEquals(expectedUb, ortoolsConst.getUpperBound(), DOUBLE_TOLERANCE);
    }

    @Test
    void testAddConstraintWithNoBounds() {
        String constName = "const1";
        // Add constraint
        OpenRaoMPConstraint const1 = openRaoMPSolver.makeConstraint(constName);

        // Check OpenRAO object
        assertEquals(-Double.POSITIVE_INFINITY, const1.lb());
        assertEquals(Double.POSITIVE_INFINITY, const1.ub());
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));

        // Check OR-Tools object
        LinearConstraint orToolsConst1 = openRaoMPSolver.getConstraint(constName).getLinearConstraint();
        assertNotNull(orToolsConst1);
        assertEquals(-Double.POSITIVE_INFINITY, orToolsConst1.getLowerBound());
        assertEquals(Double.POSITIVE_INFINITY, orToolsConst1.getUpperBound());
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
        assertTrue(openRaoMPSolver.setSolverSpecificParametersAsString("parallel/maxnthreads 1, lp/presolving TRUE")); // SCIP parameters
        assertFalse(openRaoMPSolver.setSolverSpecificParametersAsString("invalid_parameter")); // no value after the parameter name
        assertFalse(openRaoMPSolver.setSolverSpecificParametersAsString("")); // no parameter
    }

    @Test
    void testConvertResultStatus() {
        SolveResult.ProblemStatus feasibleStatus = new SolveResult.ProblemStatus(SolveResult.FeasibilityStatus.FEASIBLE, SolveResult.FeasibilityStatus.FEASIBLE, false);
        SolveResult.ProblemStatus infeasibleStatus = new SolveResult.ProblemStatus(SolveResult.FeasibilityStatus.INFEASIBLE, SolveResult.FeasibilityStatus.INFEASIBLE, true);
        SolveResult.ProblemStatus unboundedStatus = new SolveResult.ProblemStatus(SolveResult.FeasibilityStatus.FEASIBLE, SolveResult.FeasibilityStatus.INFEASIBLE, false);

        assertEquals(LinearProblemStatus.OPTIMAL, OpenRaoMPSolver.convertResultStatus(OPTIMAL, feasibleStatus));
        assertEquals(LinearProblemStatus.FEASIBLE, OpenRaoMPSolver.convertResultStatus(FEASIBLE, feasibleStatus));
        assertEquals(LinearProblemStatus.INFEASIBLE, OpenRaoMPSolver.convertResultStatus(INFEASIBLE, infeasibleStatus));
        assertEquals(LinearProblemStatus.UNBOUNDED, OpenRaoMPSolver.convertResultStatus(UNBOUNDED, unboundedStatus));
        assertEquals(LinearProblemStatus.UNBOUNDED, OpenRaoMPSolver.convertResultStatus(INFEASIBLE_OR_UNBOUNDED, unboundedStatus));
        assertEquals(LinearProblemStatus.INFEASIBLE, OpenRaoMPSolver.convertResultStatus(INFEASIBLE_OR_UNBOUNDED, infeasibleStatus));
        assertEquals(LinearProblemStatus.NOT_SOLVED, OpenRaoMPSolver.convertResultStatus(NO_SOLUTION_FOUND, feasibleStatus));
        assertEquals(LinearProblemStatus.ABNORMAL, OpenRaoMPSolver.convertResultStatus(NUMERICAL_ERROR, feasibleStatus));
        assertEquals(LinearProblemStatus.ABNORMAL, OpenRaoMPSolver.convertResultStatus(OTHER_ERROR, feasibleStatus));
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
        assertEquals(3.5, openRaoMPSolver.getModel().getObjective().getLinearTerm(var1.getVariable()));

        openRaoMPSolver.getObjective().setCoefficient(var1, -963.5);
        assertEquals(-963.5, openRaoMPSolver.getObjective().getCoefficient(var1));
        assertEquals(-963.5, openRaoMPSolver.getModel().getObjective().getLinearTerm(var1.getVariable()));
    }

    private void checkObjectiveSense(boolean minim) {
        // OpenRAO object
        assertEquals(minim, openRaoMPSolver.isMinimization());
        assertEquals(!minim, openRaoMPSolver.isMaximization());
        // OR-Tools object
        assertEquals(minim, !openRaoMPSolver.getModel().getObjective().isMaximize());
        assertEquals(!minim, openRaoMPSolver.getModel().getObjective().isMaximize());
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

        assertTrue(openRaoMPSolver.getModel().getObjective().isMaximize());
        assertEquals(LinearProblemStatus.OPTIMAL, result);
        assertEquals(4., x.solutionValue(), DOUBLE_TOLERANCE);
        assertEquals(6., y.solutionValue(), DOUBLE_TOLERANCE);

        // Test that after resetting, solver & obj sense is the same
        openRaoMPSolver.resetModel();
        assertNotNull(openRaoMPSolver.getObjective());
        checkObjectiveSense(false);
        assertEquals(SCIP, openRaoMPSolver.getSolver());
        assertEquals(SolverType.GSCIP, OpenRaoMPSolver.getOrToolsSolverType(SCIP));
    }

    @Test
    void testSolveWithHiGhs() {
        // Maximize 2 * x + y
        // such that: x + 2 * y <= 12
        //            x - y >= 1
        //            x <= y + 2
        //            x, y integers, 0 <= x <= 10, 0 <= y <= 10
        // Should result in: x = 5, y = 3, obj = 13
        OpenRaoMPSolver highsSolver = new OpenRaoMPSolver("highs_test", HIGHS);
        // HiGHS-specific parameters must parse without error
        assertTrue(highsSolver.setSolverSpecificParametersAsString("output_flag FALSE, time_limit 30"));
        OpenRaoMPVariable x = highsSolver.makeIntVar(0, 10, "x");
        OpenRaoMPVariable y = highsSolver.makeIntVar(0, 10, "y");
        OpenRaoMPConstraint c1 = highsSolver.makeConstraint(-highsSolver.infinity(), 12, "c1");
        c1.setCoefficient(x, 1);
        c1.setCoefficient(y, 2);
        OpenRaoMPConstraint c2 = highsSolver.makeConstraint(1, highsSolver.infinity(), "c2");
        c2.setCoefficient(x, 1);
        c2.setCoefficient(y, -1);
        OpenRaoMPConstraint c3 = highsSolver.makeConstraint(-highsSolver.infinity(), 2, "c3");
        c3.setCoefficient(x, 1);
        c3.setCoefficient(y, -1);
        highsSolver.getObjective().setCoefficient(x, 2);
        highsSolver.getObjective().setCoefficient(y, 1);
        highsSolver.setMaximization();
        LinearProblemStatus result = highsSolver.solve();

        assertEquals(HIGHS, highsSolver.getSolver());
        assertEquals(LinearProblemStatus.OPTIMAL, result);
        assertEquals(5., x.solutionValue(), DOUBLE_TOLERANCE);
        assertEquals(3., y.solutionValue(), DOUBLE_TOLERANCE);
    }

    @Test
    void testInfinity() {
        // OR-Tools MathOpt handles infinite bounds natively, for every solver
        for (var solverType : new SearchTreeRaoRangeActionsOptimizationParameters.Solver[] {SCIP, XPRESS, GUROBI, HIGHS}) {
            OpenRaoMPSolver solver = new OpenRaoMPSolver("solver", solverType);
            assertEquals(Double.POSITIVE_INFINITY, solver.infinity());
        }
    }

    @Test
    void testOrToolsSolverType() {
        assertEquals(SolverType.GSCIP, OpenRaoMPSolver.getOrToolsSolverType(SCIP));
        assertEquals(SolverType.XPRESS, OpenRaoMPSolver.getOrToolsSolverType(XPRESS));
        assertEquals(SolverType.GUROBI, OpenRaoMPSolver.getOrToolsSolverType(GUROBI));
        assertEquals(SolverType.HIGHS, OpenRaoMPSolver.getOrToolsSolverType(HIGHS));

        // CBC is not supported anymore by OR-Tools MathOpt
        Exception e = assertThrows(OpenRaoException.class, () -> OpenRaoMPSolver.getOrToolsSolverType(CBC));
        assertTrue(e.getMessage().contains("CBC"));
        // constructing the solver fails fast as well
        e = assertThrows(OpenRaoException.class, () -> new OpenRaoMPSolver("solver", CBC));
        assertTrue(e.getMessage().contains("CBC"));
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
