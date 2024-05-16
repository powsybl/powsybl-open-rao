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
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-international.com>}
 */
public class OpenRaoMPSolverTest {
    static final double DOUBLE_TOLERANCE = 1e-4;
    static final double INFINITY_TOLERANCE = LinearProblem.infinity() * 0.001;

    private OpenRaoMPSolver openRaoMPSolver;
    private MPSolver mpSolver;

    @BeforeEach
    public void setUp() {
        openRaoMPSolver = new OpenRaoMPSolver("test", RangeActionsOptimizationParameters.Solver.SCIP);
        mpSolver = openRaoMPSolver.getMpSolver();
    }

    @Test
    public void testConstruct() {
        assertEquals(MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING, new OpenRaoMPSolver("rao_test_prob", RangeActionsOptimizationParameters.Solver.SCIP).getMpSolver().problemType());
        assertEquals(MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING, new OpenRaoMPSolver("rao_test_prob", RangeActionsOptimizationParameters.Solver.CBC).getMpSolver().problemType());
    }

    @Test
    public void testAddAndRemoveVariable() {
        String varName = "var1";
        assertEquals(0, openRaoMPSolver.numVariables());

        // Add variable
        OpenRaoMPVariable var1 = openRaoMPSolver.makeNumVar(-5, 3.6, varName);

        // Check exception when re-adding
        Exception e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.makeNumVar(-5, 3.6, varName));
        assertEquals("Variable var1 already exists", e.getMessage());

        // Check OpenRaoMPVariable
        assertEquals(-5, var1.lb(), DOUBLE_TOLERANCE);
        assertEquals(3.6, var1.ub(), DOUBLE_TOLERANCE);
        assertTrue(openRaoMPSolver.hasVariable(varName));
        assertEquals(var1, openRaoMPSolver.getVariable(varName));
        assertEquals(1, openRaoMPSolver.numVariables());

        // Check OR-Tools object
        MPVariable orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertNotNull(orToolsVar1);
        assertEquals(-5, orToolsVar1.lb(), DOUBLE_TOLERANCE);
        assertEquals(3.6, orToolsVar1.ub(), DOUBLE_TOLERANCE);

        // Remove variable
        openRaoMPSolver.removeVariable(varName);

        // Check OpenRaoMPVariable has disappeared
        assertFalse(openRaoMPSolver.hasVariable(varName));
        assertEquals(0, openRaoMPSolver.numVariables());
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.getVariable(varName));
        assertEquals("Variable var1 has not been created yet", e.getMessage());

        // Check OR-Tools object
        orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertEquals(-LinearProblem.infinity(), orToolsVar1.lb(), INFINITY_TOLERANCE);
        assertEquals(LinearProblem.infinity(), orToolsVar1.ub(), INFINITY_TOLERANCE);

        // Re-add variable
        var1 = openRaoMPSolver.makeNumVar(10.7, 30.6, varName);

        // Check OpenRaoMPVariable
        assertEquals(10.7, var1.lb(), DOUBLE_TOLERANCE);
        assertEquals(30.6, var1.ub(), DOUBLE_TOLERANCE);
        assertTrue(openRaoMPSolver.hasVariable(varName));
        assertEquals(var1, openRaoMPSolver.getVariable(varName));
        assertEquals(1, openRaoMPSolver.numVariables());

        // Check OR-Tools object
        orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertNotNull(orToolsVar1);
        assertEquals(10.7, orToolsVar1.lb(), DOUBLE_TOLERANCE);
        assertEquals(30.6, orToolsVar1.ub(), DOUBLE_TOLERANCE);

        // Check throws if trying to remove null variable
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.removeVariable("var2"));
        assertEquals("Variable var2 has not been created yet", e.getMessage());
    }

    @Test
    public void testAddIntVar() {
        openRaoMPSolver.makeIntVar(5, 10, "var1");
        MPVariable orToolsVar = mpSolver.lookupVariableOrNull("var1");
        assertNotNull(orToolsVar);
        assertEquals(5., orToolsVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(10., orToolsVar.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddBoolVar() {
        openRaoMPSolver.makeBoolVar("var1");
        MPVariable orToolsVar = mpSolver.lookupVariableOrNull("var1");
        assertNotNull(orToolsVar);
        assertEquals(0., orToolsVar.lb(), DOUBLE_TOLERANCE);
        assertEquals(1., orToolsVar.ub(), DOUBLE_TOLERANCE);
    }

    @Test
    public void testAddAndRemoveConstraint() {
        String varName = "var1";
        String constName = "const1";

        assertEquals(0, openRaoMPSolver.numConstraints());

        // Add variable
        OpenRaoMPVariable var1 = openRaoMPSolver.makeNumVar(-5, 3.6, varName);

        // Add constraint & coefficient
        OpenRaoMPConstraint const1 = openRaoMPSolver.makeConstraint(-121.6, 65.956, constName);
        const1.setCoefficient(var1, 648.9);

        // Check exception when re-adding
        Exception e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.makeConstraint(-121.6, 65.956, constName));
        assertEquals("Constraint const1 already exists", e.getMessage());

        // Check OpenRaoMPConstraint
        assertEquals(-121.6, const1.lb(), DOUBLE_TOLERANCE);
        assertEquals(65.956, const1.ub(), DOUBLE_TOLERANCE);
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));
        assertEquals(648.9, const1.getCoefficient(var1), DOUBLE_TOLERANCE);
        assertEquals(1, openRaoMPSolver.numConstraints());

        // Check OR-Tools object
        MPConstraint orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertNotNull(orToolsConst1);
        assertEquals(-121.6, orToolsConst1.lb(), DOUBLE_TOLERANCE);
        assertEquals(65.956, orToolsConst1.ub(), DOUBLE_TOLERANCE);
        MPVariable orToolsVar1 = mpSolver.lookupVariableOrNull(varName);
        assertEquals(648.9, orToolsConst1.getCoefficient(orToolsVar1), DOUBLE_TOLERANCE);

        // Remove constraint
        openRaoMPSolver.removeConstraint(constName);

        // Check OpenRaoMPConstraint has disappeared
        assertFalse(openRaoMPSolver.hasConstraint(constName));
        assertEquals(0, openRaoMPSolver.numConstraints());
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.getConstraint(constName));
        assertEquals("Constraint const1 has not been created yet", e.getMessage());

        // Check OR-Tools object
        orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertEquals(-LinearProblem.infinity(), orToolsConst1.lb(), INFINITY_TOLERANCE);
        assertEquals(LinearProblem.infinity(), orToolsConst1.ub(), INFINITY_TOLERANCE);
        assertEquals(0., orToolsConst1.getCoefficient(orToolsVar1), DOUBLE_TOLERANCE);

        // Re-add constraint
        const1 = openRaoMPSolver.makeConstraint(10.7, 30.6, constName);

        // Check OpenRaoMPVariable
        assertEquals(10.7, const1.lb(), DOUBLE_TOLERANCE);
        assertEquals(30.6, const1.ub(), DOUBLE_TOLERANCE);
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));
        assertEquals(1, openRaoMPSolver.numConstraints());

        // Check OR-Tools object
        orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertNotNull(orToolsConst1);
        assertEquals(10.7, orToolsConst1.lb(), DOUBLE_TOLERANCE);
        assertEquals(30.6, orToolsConst1.ub(), DOUBLE_TOLERANCE);

        // Check throws if trying to remove null variable
        e = assertThrows(OpenRaoException.class, () -> openRaoMPSolver.removeConstraint("const2"));
        assertEquals("Constraint const2 has not been created yet", e.getMessage());
    }

    @Test
    public void testAddConstraintWithNoBounds() {
        String constName = "const1";
        // Add constraint
        OpenRaoMPConstraint const1 = openRaoMPSolver.makeConstraint(constName);

        // Check OpenRaoMPConstraint
        assertEquals(-LinearProblem.infinity(), const1.lb(), INFINITY_TOLERANCE);
        assertEquals(LinearProblem.infinity(), const1.ub(), INFINITY_TOLERANCE);
        assertTrue(openRaoMPSolver.hasConstraint(constName));
        assertEquals(const1, openRaoMPSolver.getConstraint(constName));

        // Check OR-Tools object
        MPConstraint orToolsConst1 = mpSolver.lookupConstraintOrNull(constName);
        assertNotNull(orToolsConst1);
        assertEquals(-LinearProblem.infinity(), orToolsConst1.lb(), INFINITY_TOLERANCE);
        assertEquals(LinearProblem.infinity(), orToolsConst1.ub(), INFINITY_TOLERANCE);
    }
}
