/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.result.api.LinearProblemStatus;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates OR-Tools' MPSolver objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPSolver {

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    private final MPSolver mpSolver;
    private MPSolverParameters solveConfiguration;
    Map<String, FaraoMPConstraint> constraints = new HashMap<>();
    Map<String, FaraoMPVariable> variables = new HashMap<>();
    FaraoMPObjective objective;

    // Only for tests
    protected FaraoMPSolver() {
        mpSolver = null;
    }

    public FaraoMPSolver(String optProblemName, RangeActionsOptimizationParameters.Solver solver) {
        switch (solver) {
            case CBC:
                this.mpSolver = new MPSolver(optProblemName, MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
                break;
            case SCIP:
                this.mpSolver = new MPSolver(optProblemName, MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING);
                break;
            case XPRESS:
                this.mpSolver = new MPSolver(optProblemName, MPSolver.OptimizationProblemType.XPRESS_MIXED_INTEGER_PROGRAMMING);
                break;
            default:
                throw new FaraoException(String.format("unknown solver %s in RAO parameters", solver));
        }
        solveConfiguration = new MPSolverParameters();
    }

    public FaraoMPConstraint getConstraint(String name) {
        if (constraints.containsKey(name)) {
            return constraints.get(name);
        } else {
            throw new FaraoException(String.format("Constraint %s has not been created yet", name));
        }
    }

    public FaraoMPVariable getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else {
            throw new FaraoException(String.format("Variable %s has not been created yet", name));
        }
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
        if (FaraoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled()) {
            mpSolver.enableOutput();
        }
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
