/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.mocks;

import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;

/**
 * This class is used to mock MPSolver objects in the unit test of this package.
 * It is necessary to bypass the JNI binding of the or-tools MPSolver object.
 *
 * MPSolverMock handles the creation and browsing of the variables and constraints
 * usually defined within a MPSolver.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MPSolverMock extends MPSolver {

    private MPObjectiveMock objective;
    private List<MPConstraintMock> constraints;
    private List<MPVariableMock> variables;

    public MPSolverMock() {
        super(0, false);
        constraints = new ArrayList<>();
        variables = new ArrayList<>();
        objective = new MPObjectiveMock();
    }

    public static double infinity() {
        return Double.MAX_VALUE;
    }

    @Override
    public MPVariableMock makeNumVar(double lb, double ub, String name) {
        // check that variable does not already exists
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, lb, ub, false);
        variables.add(newVariable);
        return newVariable;
    }

    @Override
    public MPConstraintMock makeConstraint(double lb, double ub, String name) {
        // check that constraint does not already exists
        assertFalse(constraints.stream().anyMatch(v -> v.name().equals(name)));

        MPConstraintMock newConstraint = new MPConstraintMock(name, lb, ub);
        constraints.add(newConstraint);
        return newConstraint;
    }

    @Override
    public MPVariable makeBoolVar(String name) {
        // check that variable does not already exists
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, 0, 1, true);
        variables.add(newVariable);
        return newVariable;
    }

    @Override
    public MPVariable makeIntVar(double lb, double ub, String name) {
        // check that variable does not already exists
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, lb, ub, true);
        variables.add(newVariable);
        return newVariable;
    }

    @Override
    public MPConstraintMock makeConstraint(double lb, double ub) {
        MPConstraintMock newConstraint = new MPConstraintMock("", lb, ub);
        constraints.add(newConstraint);
        return newConstraint;
    }

    @Override
    public MPConstraintMock makeConstraint(String name) {
        MPConstraintMock newConstraint = new MPConstraintMock(name, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        constraints.add(newConstraint);
        return newConstraint;
    }

    @Override
    public MPVariableMock lookupVariableOrNull(String varName) {
        List<MPVariableMock> variablesWithSameName = variables.stream().filter(v -> v.name().equals(varName)).collect(Collectors.toList());
        if (variablesWithSameName.size() == 0) {
            return null;
        }
        return variablesWithSameName.get(0);
    }

    @Override
    public MPObjectiveMock objective() {
        return objective;
    }

    @Override
    public MPConstraintMock lookupConstraintOrNull(String constraintName) {
        List<MPConstraintMock> constraintsWithSameName = constraints.stream().filter(v -> v.name().equals(constraintName)).collect(Collectors.toList());
        if (constraintsWithSameName.size() == 0) {
            return null;
        }
        return constraintsWithSameName.get(0);
    }

    @Override
    public int numVariables() {
        return variables.size();
    }

    @Override
    public int numConstraints() {
        return constraints.size();
    }

    public void randomSolve() {
        variables.forEach(MPVariableMock::setRandomSolutionValue);
    }

    public enum ResultStatusMock {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        UNBOUNDED,
        ABNORMAL,
        NOT_SOLVED
    }
}
