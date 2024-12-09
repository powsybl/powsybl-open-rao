/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.raoapi.parameters.extensions.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Encapsulates OR-Tools' MPSolver objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-international.com>}
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
    private static final double MIN_DOUBLE = 1e-6;
    private static final Map<RangeActionsOptimizationParameters.Solver, Double> SOLVER_INFINITY = Map.of(
        RangeActionsOptimizationParameters.Solver.CBC, Double.POSITIVE_INFINITY,
        RangeActionsOptimizationParameters.Solver.SCIP, 1E20,
        RangeActionsOptimizationParameters.Solver.XPRESS, 1E20
    );

    private final RangeActionsOptimizationParameters.Solver solver;
    private final String optProblemName;
    private MPSolver mpSolver;
    private final MPSolverParameters solveConfiguration;
    private String solverSpecificParameters;
    Map<String, OpenRaoMPConstraint> constraints = new TreeMap<>();
    Map<String, OpenRaoMPVariable> variables = new TreeMap<>();
    OpenRaoMPObjective objective;
    private boolean objectiveMinimization = true;

    public OpenRaoMPSolver(String optProblemName, RangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        this.optProblemName = optProblemName;
        solveConfiguration = new MPSolverParameters();
        resetModel();
    }

    public void resetModel() {
        this.mpSolver = new MPSolver(optProblemName, getOrToolsProblemType(solver));
        constraints = new TreeMap<>();
        variables = new TreeMap<>();
        this.objective = new OpenRaoMPObjective(mpSolver.objective());
        setSolverSpecificParametersAsString(solverSpecificParameters);
        if (objectiveMinimization) {
            setMinimization();
        } else {
            setMaximization();
        }
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
        }
        double roundedLb = roundDouble(lb);
        double roundedUb = roundDouble(ub);
        OpenRaoMPVariable variable = new OpenRaoMPVariable(mpSolver.makeVar(roundedLb, roundedUb, integer, name));
        variables.put(name, variable);
        return variable;
    }

    public OpenRaoMPConstraint makeConstraint(double lb, double ub, String name) {
        if (hasConstraint(name)) {
            throw new OpenRaoException(String.format("Constraint %s already exists", name));
        } else {
            double roundedLb = roundDouble(lb);
            double roundedUb = roundDouble(ub);
            OpenRaoMPConstraint constraint = new OpenRaoMPConstraint(mpSolver.makeConstraint(roundedLb, roundedUb, name));
            constraints.put(name, constraint);
            return constraint;
        }
    }

    public OpenRaoMPConstraint makeConstraint(String name) {
        return makeConstraint(-infinity(), infinity(), name);
    }

    public boolean setSolverSpecificParametersAsString(String solverSpecificParameters) {
        this.solverSpecificParameters = solverSpecificParameters;
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
        if (OpenRaoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled()) {
            mpSolver.enableOutput();
        }
        return convertResultStatus(mpSolver.solve(solveConfiguration));
    }

    static LinearProblemStatus convertResultStatus(MPSolver.ResultStatus status) {
        return switch (status) {
            case OPTIMAL -> LinearProblemStatus.OPTIMAL;
            case ABNORMAL -> LinearProblemStatus.ABNORMAL;
            case FEASIBLE -> LinearProblemStatus.FEASIBLE;
            case UNBOUNDED -> LinearProblemStatus.UNBOUNDED;
            case INFEASIBLE -> LinearProblemStatus.INFEASIBLE;
            case NOT_SOLVED -> LinearProblemStatus.NOT_SOLVED;
            default -> throw new OpenRaoException(String.format("Status %s not handled.", status));
        };
    }

    public int numVariables() {
        return variables.size();
    }

    public int numConstraints() {
        return constraints.size();
    }

    public boolean isMinimization() {
        return objectiveMinimization;
    }

    public void setMinimization() {
        mpSolver.objective().setMinimization();
        objectiveMinimization = true;
    }

    public boolean isMaximization() {
        return !objectiveMinimization;
    }

    public void setMaximization() {
        mpSolver.objective().setMaximization();
        objectiveMinimization = false;
    }

    /* Method used to make sure the MIP is reproducible. This basically rounds the least significant bits of a double.
     Let's say a double has 10 precision bits (in reality, 52)
     We take an initial double:
       .............//////////.....
     To which we add a "bigger" double :
       .........\\\\\\\\\\..........
      =>
       .........\\\\||||||..........
       (we "lose" the least significant bits of the first double because the sum double doesn't have enough precision to show them)
     Then we subtract the same "bigger" double:
       .............//////..........
       We get back our original bits for the most significant part, but the least significant bits are still gone.
     */
    static double roundDouble(double value) {
        if (Double.isNaN(value)) {
            throw new OpenRaoException("Trying to add a NaN value in MIP!");
        }
        if (Math.abs(value) < MIN_DOUBLE) {
            return 0.;
        }
        double t = value * (1L << NUMBER_OF_BITS_TO_ROUND_OFF);
        if (t != Double.POSITIVE_INFINITY && value != Double.NEGATIVE_INFINITY && !Double.isNaN(t)) {
            return value - t + t;
        }
        return value;
    }

    public double infinity() {
        return SOLVER_INFINITY.get(solver) * 1000;
        // we must use something greater than the solver's infinity, in case we subtract things from it, it should
        // stay greater than infinity
        // TODO: replace with mpsSolver.solverInfinity() * 1000 when made available
    }
}
