/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.RaoComputation;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ClosedOptimisationRao implements RaoComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClosedOptimisationRao.class);

    static {
        System.loadLibrary("jniortools");
    }

    private MPSolver createSolver(String solverType) {
        try {
            return new MPSolver("FARAO optimisation",
                            MPSolver.OptimizationProblemType.valueOf(solverType));
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        }
    }

    private Network network;
    private CracFile cracFile;
    private ComputationManager computationManager;

    public ClosedOptimisationRao(Network network,
                                 CracFile cracFile,
                                 ComputationManager computationManager,
                                 LoadFlowFactory loadFlowFactory,
                                 SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = network;
        this.cracFile = cracFile;
        this.computationManager = computationManager;

        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        LoadFlowService.init(loadFlowFactory, computationManager);
        network.getStateManager().allowStateMultiThreadAccess(true);
    }

    @Override
    public CompletableFuture<RaoComputationResult> run(String workingStateId, RaoComputationParameters parameters) {
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        // Change working state
        network.getStateManager().setWorkingState(workingStateId);

        ClosedOptimisationRaoParameters parametersExtension = parameters.getExtension(ClosedOptimisationRaoParameters.class);
        if (Objects.isNull(parametersExtension)) {
            throw new FaraoException("Closed optimisation RAO computation parameters not available");
        }

        MPSolver solver = createSolver(parametersExtension.getSolverType());
        if (Objects.isNull(solver)) {
            throw new FaraoException("Could not create solver " + parametersExtension.getSolverType());
        }

        Map<String, Object> data = OptimisationComponentUtil.getDataMap(network, cracFile, computationManager, parametersExtension);

        Queue<AbstractOptimisationProblemFiller> fillers = OptimisationComponentUtil.getFillersStack(network, cracFile, data, parametersExtension);

        fillers.forEach(filler -> filler.fillProblem(solver));

        final MPSolver.ResultStatus resultStatus = solver.solve();

        RaoComputationResult.Status status = RaoComputationResult.Status.SUCCESS;
        // Check that the problem has an optimal solution.
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            LOGGER.error("The problem does not have an optimal solution!");
            status = RaoComputationResult.Status.FAILED;
        }

        // Verify that the solution satisfies all constraints (when using solvers
        // others than GLOP_LINEAR_PROGRAMMING, this is highly recommended!).
        if (!solver.verifySolution(1e-7, true)) {
            LOGGER.error("The solution returned by the solver violated the"
                    + " problem constraints by at least 1e-7");
            status = RaoComputationResult.Status.FAILED;
        }

        RaoComputationResult result = new RaoComputationResult(status);
        ClosedOptimisationRaoResult resultExtension = new ClosedOptimisationRaoResult();
        fillSolverInfo(resultExtension, solver, resultStatus);
        fillers.forEach(filler -> {
            filler.variablesProvided().forEach(var -> fillVariableInfo(resultExtension, solver, var));
            filler.constraintsProvided().forEach(constraint -> fillConstraintInfo(resultExtension, solver, constraint));
        });
        fillObjectiveInfo(resultExtension, solver);
        result.addExtension(ClosedOptimisationRaoResult.class, resultExtension);

        network.getStateManager().allowStateMultiThreadAccess(false);
        return CompletableFuture.completedFuture(result);
    }

    private void fillSolverInfo(ClosedOptimisationRaoResult result, MPSolver solver, MPSolver.ResultStatus resultStatus) {
        result.setSolverInfo(
                solver.numVariables(),
                solver.numConstraints(),
                solver.iterations(),
                solver.wallTime(),
                resultStatus.name()
        );
    }

    private void fillVariableInfo(ClosedOptimisationRaoResult result, MPSolver solver, String varName) {
        MPVariable variable = Objects.requireNonNull(solver.lookupVariableOrNull(varName), String.format("variable '%s' not found", varName));
        result.addVariableInfo(
                varName,
                variable.solutionValue(),
                variable.lb(),
                variable.ub()
        );
    }

    private void fillConstraintInfo(ClosedOptimisationRaoResult result, MPSolver solver, String constraintName) {
        MPConstraint constraint = Objects.requireNonNull(solver.lookupConstraintOrNull(constraintName), String.format("constraint '%s' not found", constraintName));
        result.addConstraintInfo(
                constraintName,
                constraint.dualValue(),
                constraint.isLazy(),
                constraint.lb(),
                constraint.ub(),
                constraint.basisStatus().name()
        );
    }

    private void fillObjectiveInfo(ClosedOptimisationRaoResult result, MPSolver solver) {
        MPObjective objective = solver.objective();
        result.setObjectiveInfo(
                objective.maximization(),
                objective.value()
        );
    }
}
