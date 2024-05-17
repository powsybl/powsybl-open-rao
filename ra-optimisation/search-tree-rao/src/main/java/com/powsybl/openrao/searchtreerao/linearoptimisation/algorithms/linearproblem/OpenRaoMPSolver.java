/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates OR-Tools' MPSolver objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPSolver {
    static {
        try {
            Loader.loadNativeLibraries();
        } catch (Exception e) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    private final MPSolver mpSolver;
    private final RangeActionsOptimizationParameters.Solver solver;
    private MPSolverParameters solveConfiguration;
    Map<String, OpenRaoMPConstraint> constraints = new HashMap<>();
    Map<String, OpenRaoMPVariable> variables = new HashMap<>();
    OpenRaoMPObjective objective;

    public OpenRaoMPSolver(String optProblemName, RangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        this.mpSolver = new MPSolver(optProblemName, getOrToolsProblemType(solver));
        this.objective = new OpenRaoMPObjective(mpSolver.objective(), NUMBER_OF_BITS_TO_ROUND_OFF);
        solveConfiguration = new MPSolverParameters();
    }

    public RangeActionsOptimizationParameters.Solver getSolver() {
        return solver;
    }

    private MPSolver.OptimizationProblemType getOrToolsProblemType(RangeActionsOptimizationParameters.Solver solver) {
        Objects.requireNonNull(solver);
        return switch (solver) {
            case CBC -> MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING;
            case SCIP -> MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING;
            case XPRESS -> MPSolver.OptimizationProblemType.XPRESS_MIXED_INTEGER_PROGRAMMING;
            default -> throw new OpenRaoException(String.format("unknown solver %s in RAO parameters", solver));
        };
    }

    // Only for this class' tests
    MPSolver getMpSolver() {
        return mpSolver;
    }

    public boolean hasConstraint(String name) {
        return constraints.containsKey(name);
    }

    public OpenRaoMPConstraint getConstraint(String name) {
        if (hasConstraint(name)) {
            return constraints.get(name);
        } else {
            throw new OpenRaoException(String.format("Constraint %s has not been created yet", name));
        }
    }

    public void removeConstraint(String name) {
        if (hasConstraint(name)) {
            constraints.get(name).remove(mpSolver);
            constraints.remove(name);
        } else {
            throw new OpenRaoException(String.format("Constraint %s has not been created yet", name));
        }
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    public OpenRaoMPVariable getVariable(String name) {
        if (hasVariable(name)) {
            return variables.get(name);
        } else {
            throw new OpenRaoException(String.format("Variable %s has not been created yet", name));
        }
    }

    public void removeVariable(String name) {
        if (hasVariable(name)) {
            variables.get(name).remove();
            variables.remove(name);
        } else {
            throw new OpenRaoException(String.format("Variable %s has not been created yet", name));
        }
    }

    public OpenRaoMPObjective getObjective() {
        return this.objective;
    }

    public OpenRaoMPVariable makeNumVar(double lb, double ub, String name) {
        return makeVar(lb, ub, false, name);
    }

    public OpenRaoMPVariable makeIntVar(double lb, double ub, String name) {
        return makeVar(lb, ub, true, name);
    }

    public OpenRaoMPVariable makeBoolVar(String name) {
        return makeVar(0, 1, true, name);
    }

    private OpenRaoMPVariable makeVar(double lb, double ub, boolean integer, String name) {
        if (hasVariable(name)) {
            throw new OpenRaoException(String.format("Variable %s already exists", name));
        } else {
            double roundedLb = RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF);
            double roundedUb = RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF);
            MPVariable mpVariable;
            if (mpSolver.lookupVariableOrNull(name) != null) {
                // This means that the variable was created before but removed in the meantime during an update
                // Since it could not be removed from OR-Tools, we should re-use it
                mpVariable = mpSolver.lookupVariableOrNull(name);
                mpVariable.setBounds(roundedLb, roundedUb);
                mpVariable.setInteger(integer);
            } else {
                mpVariable = mpSolver.makeVar(roundedLb, roundedUb, integer, name);
            }
            OpenRaoMPVariable variable = new OpenRaoMPVariable(mpVariable, NUMBER_OF_BITS_TO_ROUND_OFF);
            variables.put(name, variable);
            return variable;
        }
    }

    public OpenRaoMPConstraint makeConstraint(double lb, double ub, String name) {
        if (hasConstraint(name)) {
            throw new OpenRaoException(String.format("Constraint %s already exists", name));
        } else {
            double roundedLb = RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF);
            double roundedUb = RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF);
            MPConstraint mpConstraint;
            if (mpSolver.lookupConstraintOrNull(name) != null) {
                // This means that the constraint was created before but removed in the meantime during an update
                // Since it could not be removed from OR-Tools, we should re-use it
                mpConstraint = mpSolver.lookupConstraintOrNull(name);
                mpConstraint.setBounds(roundedLb, roundedUb);
            } else {
                mpConstraint = mpSolver.makeConstraint(roundedLb, roundedUb, name);
            }
            OpenRaoMPConstraint constraint = new OpenRaoMPConstraint(mpConstraint, NUMBER_OF_BITS_TO_ROUND_OFF);
            constraints.put(name, constraint);
            return constraint;
        }
    }

    public OpenRaoMPConstraint makeConstraint(String name) {
        return makeConstraint(-LinearProblem.infinity(), LinearProblem.infinity(), name);
    }

    public boolean setSolverSpecificParametersAsString(String solverSpecificParameters) {
        if (solverSpecificParameters != null) {
            return mpSolver.setSolverSpecificParametersAsString(solverSpecificParameters);
        } else {
            return true;
        }
    }

    public void setRelativeMipGap(double relativeMipGap) {
        solveConfiguration.setDoubleParam(MPSolverParameters.DoubleParam.RELATIVE_MIP_GAP, relativeMipGap);
    }

    public LinearProblemStatus solve() {
        String lp = mpSolver.exportModelAsLpFormat();
        if (OpenRaoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled()) {
            mpSolver.enableOutput();
        }
        return convertResultStatus(mpSolver.solve(solveConfiguration));
    }

    private static LinearProblemStatus convertResultStatus(MPSolver.ResultStatus status) {
        return switch (status) {
            case OPTIMAL -> LinearProblemStatus.OPTIMAL;
            case ABNORMAL -> LinearProblemStatus.ABNORMAL;
            case FEASIBLE -> LinearProblemStatus.FEASIBLE;
            case UNBOUNDED -> LinearProblemStatus.UNBOUNDED;
            case INFEASIBLE -> LinearProblemStatus.INFEASIBLE;
            case NOT_SOLVED -> LinearProblemStatus.NOT_SOLVED;
            default -> throw new NotImplementedException(String.format("Status %s not handled.", status));
        };
    }

    public int numVariables() {
        return variables.size();
    }

    public int numConstraints() {
        return constraints.size();
    }
}
