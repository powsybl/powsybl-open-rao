/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

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

/**
 * Encapsulates OR-Tools' MPSolver objects in order to round up doubles
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPSolver {

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    private final MPSolver mpSolver;
    private MPSolverParameters solveConfiguration;
    Map<String, OpenRaoMPConstraint> constraints = new HashMap<>();
    Map<String, OpenRaoMPVariable> variables = new HashMap<>();
    OpenRaoMPObjective objective;

    // Only for tests
    protected OpenRaoMPSolver() {
        mpSolver = null;
    }

    public OpenRaoMPSolver(String optProblemName, RangeActionsOptimizationParameters.Solver solver) {
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
                throw new OpenRaoException(String.format("unknown solver %s in RAO parameters", solver));
        }
        solveConfiguration = new MPSolverParameters();
    }

    public OpenRaoMPConstraint getConstraint(String name) {
        if (constraints.containsKey(name)) {
            return constraints.get(name);
        } else {
            throw new OpenRaoException(String.format("Constraint %s has not been created yet", name));
        }
    }

    public OpenRaoMPVariable getVariable(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else {
            throw new OpenRaoException(String.format("Variable %s has not been created yet", name));
        }
    }

    public OpenRaoMPObjective getObjective() {
        return this.objective;
    }

    public OpenRaoMPObjective objective() {
        if (this.objective == null) {
            this.objective = new OpenRaoMPObjective(mpSolver.objective(), NUMBER_OF_BITS_TO_ROUND_OFF);
        }
        return this.objective;
    }

    public OpenRaoMPVariable makeNumVar(double lb, double ub, String name) {
        OpenRaoMPVariable mpVariable = new OpenRaoMPVariable(
            mpSolver.makeNumVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public OpenRaoMPVariable makeIntVar(double lb, double ub, String name) {
        OpenRaoMPVariable mpVariable = new OpenRaoMPVariable(
            mpSolver.makeIntVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public OpenRaoMPVariable makeBoolVar(String name) {
        OpenRaoMPVariable mpVariable = new OpenRaoMPVariable(mpSolver.makeBoolVar(name), NUMBER_OF_BITS_TO_ROUND_OFF);
        variables.put(name, mpVariable);
        return mpVariable;
    }

    public OpenRaoMPConstraint makeConstraint(double lb, double ub, String name) {
        OpenRaoMPConstraint mpConstraint = new OpenRaoMPConstraint(
            mpSolver.makeConstraint(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name),
            NUMBER_OF_BITS_TO_ROUND_OFF
        );
        constraints.put(name, mpConstraint);
        return mpConstraint;
    }

    public OpenRaoMPConstraint makeConstraint(String name) {
        OpenRaoMPConstraint mpConstraint = new OpenRaoMPConstraint(mpSolver.makeConstraint(name), NUMBER_OF_BITS_TO_ROUND_OFF);
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
        if (OpenRaoLoggerProvider.TECHNICAL_LOGS.isTraceEnabled()) {
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
