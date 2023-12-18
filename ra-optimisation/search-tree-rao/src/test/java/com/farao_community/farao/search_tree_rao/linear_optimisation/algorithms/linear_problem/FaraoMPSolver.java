/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.powsybl.open_rao.search_tree_rao.result.api.LinearProblemStatus;
import com.google.ortools.linearsolver.MPSolver;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This class is used to mock MPSolver objects in the unit test of this package.
 * It is necessary to bypass the JNI binding of the or-tools MPSolver object.
 * <p>
 * FaraoMPSolver handles the creation and browsing of the variables and constraints
 * usually defined within a MPSolver.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FaraoMPSolver {

    private final MPObjectiveMock objective;
    private final List<MPConstraintMock> constraints;
    private final List<MPVariableMock> variables;

    public FaraoMPSolver() {
        constraints = new ArrayList<>();
        variables = new ArrayList<>();
        objective = new MPObjectiveMock();
    }

    public FaraoMPSolver(String optProblemName, RangeActionsOptimizationParameters.Solver solver) {
        this();
    }

    public static double infinity() {
        return 1e10;
    }

    public FaraoMPVariable makeNumVar(double lb, double ub, String name) {
        // check that variable does not already exists
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, lb, ub, false);
        variables.add(newVariable);
        return newVariable;
    }

    public FaraoMPConstraint makeConstraint(double lb, double ub, String name) {
        // check that constraint does not already exist
        assertFalse(constraints.stream().anyMatch(v -> v.name().equals(name)));

        MPConstraintMock newConstraint = new MPConstraintMock(name, lb, ub);
        constraints.add(newConstraint);
        return newConstraint;
    }

    public FaraoMPVariable makeBoolVar(String name) {
        // check that variable does not already exist
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, 0, 1, true);
        variables.add(newVariable);
        return newVariable;
    }

    public FaraoMPVariable makeIntVar(double lb, double ub, String name) {
        // check that variable does not already exist
        assertFalse(variables.stream().anyMatch(v -> v.name().equals(name)));

        MPVariableMock newVariable = new MPVariableMock(name, lb, ub, true);
        variables.add(newVariable);
        return newVariable;
    }

    public FaraoMPConstraint makeConstraint(String name) {
        MPConstraintMock newConstraint = new MPConstraintMock(name, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        constraints.add(newConstraint);
        return newConstraint;
    }

    public FaraoMPVariable getVariable(String varName) {
        List<MPVariableMock> variablesWithSameName = variables.stream().filter(v -> v.name().equals(varName)).collect(Collectors.toList());
        if (variablesWithSameName.isEmpty()) {
            throw new FaraoException(String.format("Variable %s has not been created yet", varName));
        }
        return variablesWithSameName.get(0);
    }

    public FaraoMPObjective objective() {
        return objective;
    }

    public FaraoMPObjective getObjective() {
        return objective;
    }

    public FaraoMPConstraint getConstraint(String constraintName) {
        List<MPConstraintMock> constraintsWithSameName = constraints.stream().filter(v -> v.name().equals(constraintName)).collect(Collectors.toList());
        if (constraintsWithSameName.isEmpty()) {
            throw new FaraoException(String.format("Constraint %s has not been created yet", constraintName));
        }
        return constraintsWithSameName.get(0);
    }

    public int numVariables() {
        return variables.size();
    }

    public int numConstraints() {
        return constraints.size();
    }

    public void randomSolve() {
        variables.forEach(MPVariableMock::setRandomSolutionValue);
    }

    public enum ResultStatusMock {
        OPTIMAL, FEASIBLE, INFEASIBLE, UNBOUNDED, ABNORMAL, NOT_SOLVED
    }

    public boolean setSolverSpecificParametersAsString(String solverSpecificParameters) {
        return true;
    }

    public void setRelativeMipGap(double relativeMipGap) {

    }

    public LinearProblemStatus solve() {
        return LinearProblemStatus.OPTIMAL;
    }

    private static LinearProblemStatus convertResultStatus(MPSolver.ResultStatus status) {
        switch (status) {
            case OPTIMAL:
                return LinearProblemStatus.OPTIMAL;
            case ABNORMAL:
                return LinearProblemStatus.ABNORMAL;
            case FEASIBLE:
                return LinearProblemStatus.FEASIBLE;
            case UNBOUNDED:
                return LinearProblemStatus.UNBOUNDED;
            case INFEASIBLE:
                return LinearProblemStatus.INFEASIBLE;
            case NOT_SOLVED:
                return LinearProblemStatus.NOT_SOLVED;
            default:
                throw new NotImplementedException(String.format("Status %s not handled.", status));
        }
    }
}
