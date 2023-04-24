/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.LinearProblemStatus;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters.Solver.*;

/**
 * Encapsulates OR-Tools' MPSolver objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPSolver {
    private static final String OPT_PROBLEM_NAME = "OptProblem";
    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    private final RangeActionsOptimizationParameters.Solver solver;
    private final MPSolver mpSolver;
    private MPSolverParameters solveConfiguration;
    Map<String, FaraoMPConstraint> constraints = new HashMap<>();
    Map<String, FaraoMPVariable> variables = new HashMap<>();
    FaraoMPObjective objective;
    private final static Map<RangeActionsOptimizationParameters.Solver, Map<MPSolver, Boolean>> AVAILABLE_MP_SOLVERS = new HashMap<>(
        Map.of(
            CBC, new HashMap<>(),
            SCIP, new HashMap<>(),
            XPRESS, new HashMap<>()
        )
    );

    // Only for tests
    protected FaraoMPSolver() {
        solver = null;
        mpSolver = null;
    }

    public FaraoMPSolver(RangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        this.mpSolver = getMPSolver(solver);
        solveConfiguration = new MPSolverParameters();
    }

    synchronized private static MPSolver getMPSolver(RangeActionsOptimizationParameters.Solver solver) {
        Optional<MPSolver> availableMpSolver = AVAILABLE_MP_SOLVERS.get(solver).entrySet()
            .stream().filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .findAny();
        MPSolver mpSolver = availableMpSolver.orElseGet(() -> initNewMPSolver(solver));
        AVAILABLE_MP_SOLVERS.get(solver).put(mpSolver, false);
        return mpSolver;
    }

    synchronized public void release() {
        mpSolver.reset();
        if (solver.equals(CBC)) {
            // The reset method seems to be badly implemented for CBC. Just destroy MPSolver, do not re-use it
            AVAILABLE_MP_SOLVERS.get(solver).remove(mpSolver);
        } else {
            AVAILABLE_MP_SOLVERS.get(solver).put(mpSolver, true);
        }
    }

    private static MPSolver initNewMPSolver(RangeActionsOptimizationParameters.Solver solver) {
        switch (solver) {
            case CBC:
                return new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
            case SCIP:
                return new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING);
            case XPRESS:
                return new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.XPRESS_MIXED_INTEGER_PROGRAMMING);
            default:
                throw new FaraoException(String.format("unknown solver %s in RAO parameters", solver));
        }
    }

    public FaraoMPConstraint getConstraint(String name) {
        return constraints.get(name);
    }

    public FaraoMPVariable getVariable(String name) {
        return variables.get(name);
    }

    public FaraoMPObjective getObjective() {
        return this.objective;
    }

    public FaraoMPObjective objective() {
        if (this.objective == null) {
            this.objective = new FaraoMPObjective(mpSolver.objective(), NUMBER_OF_BITS_TO_ROUND_OFF);
        }
        return this.objective;
    }

    public FaraoMPVariable makeNumVar(double lb, double ub, String name) {
        FaraoMPVariable mpVariable = new FaraoMPVariable(
            mpSolver.makeNumVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public FaraoMPVariable makeIntVar(double lb, double ub, String name) {
        FaraoMPVariable mpVariable = new FaraoMPVariable(
            mpSolver.makeIntVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public FaraoMPVariable makeBoolVar(String name) {
        FaraoMPVariable mpVariable = new FaraoMPVariable(mpSolver.makeBoolVar(name), NUMBER_OF_BITS_TO_ROUND_OFF);
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public FaraoMPConstraint makeConstraint(double lb, double ub, String name) {
        FaraoMPConstraint mpConstraint = new FaraoMPConstraint(
            mpSolver.makeConstraint(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        constraints.put(name, mpConstraint);
        return mpConstraint;
    }

    public FaraoMPConstraint makeConstraint(String name) {
        FaraoMPConstraint mpConstraint = new FaraoMPConstraint(mpSolver.makeConstraint(name), NUMBER_OF_BITS_TO_ROUND_OFF);
        constraints.put(name, mpConstraint);
        return mpConstraint;
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
        return convertResultStatus(mpSolver.solve(solveConfiguration));
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

    public int numVariables() {
        return mpSolver.numVariables();
    }

    public int numConstraints() {
        return mpSolver.numConstraints();
    }
}
