/*
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
import com.farao_community.farao.util.LoadFlowService;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.ortools.linearsolver.*;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ClosedOptimisationRao implements RaoComputation {

    static {
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClosedOptimisationRao.class);

    private Network network;
    private CracFile cracFile;
    private ComputationManager computationManager;

    public ClosedOptimisationRao(Network network,
                                 CracFile cracFile,
                                 ComputationManager computationManager,
                                 LoadFlow.Runner loadFlowRunner,
                                 SensitivityComputationFactory sensitivityComputationFactory) {
        this.network = network;
        this.cracFile = cracFile;
        this.computationManager = computationManager;

        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Override
    public CompletableFuture<RaoComputationResult> run(String workingVariantId, RaoComputationParameters parameters) {
        Objects.requireNonNull(workingVariantId);
        Objects.requireNonNull(parameters);

        // Check RAO computation configuration
        List<String> configurationIssues = ConfigurationUtil.checkRaoConfiguration(parameters);
        if (!configurationIssues.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() +
                    String.join(System.lineSeparator(), configurationIssues));
        }

        // Change working variant
        network.getVariantManager().setWorkingVariant(workingVariantId);
        network.getVariantManager().allowVariantMultiThreadAccess(true);

        ClosedOptimisationRaoParameters parametersExtension = Objects.requireNonNull(parameters.getExtension(ClosedOptimisationRaoParameters.class)); // Should not be null, checked previously

        MPSolver solver = new MPSolver("FARAO optimisation", MPSolver.OptimizationProblemType.valueOf(parametersExtension.getSolverType())); // Should not be null, checked previously

        Map<String, Object> data = OptimisationComponentUtil.getDataMap(network, cracFile, computationManager, parametersExtension);

        Queue<AbstractOptimisationProblemFiller> fillers = OptimisationComponentUtil.getFillersStack(network, cracFile, data, parametersExtension);

        fillers.forEach(filler -> filler.fillProblem(solver));

        MPSolverParameters solverParameters = ConfigurationUtil.getSolverParameters(parametersExtension, solver);

        final MPSolver.ResultStatus resultStatus = solver.solve(solverParameters);

        RaoComputationResult.Status status = RaoComputationResult.Status.SUCCESS;
        // Check that the problem has an optimal solution.
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            LOGGER.error("The problem does not have an optimal solution!");
            status = RaoComputationResult.Status.FAILURE;
        }

        // Verify that the solution satisfies all constraints (when using solvers
        // others than GLOP_LINEAR_PROGRAMMING, this is highly recommended!).
        if (status == RaoComputationResult.Status.SUCCESS && !solver.verifySolution(1e-7, true)) {
            LOGGER.error("The solution returned by the solver violated the"
                    + " problem constraints by at least 1e-7");
            status = RaoComputationResult.Status.FAILURE;
        }

        RaoComputationResult result = new RaoComputationResult(status);
        ClosedOptimisationRaoResult resultExtension = new ClosedOptimisationRaoResult();
        fillSolverInfo(resultExtension, solver, resultStatus);

        if (status == RaoComputationResult.Status.SUCCESS) {
            fillers.forEach(filler -> {
                filler.variablesProvided().forEach(var -> fillVariableInfo(resultExtension, solver, var));
                filler.constraintsProvided().forEach(constraint -> fillConstraintInfo(resultExtension, solver, constraint));
            });
            fillObjectiveInfo(resultExtension, solver);
            OptimisationComponentUtil.fillResults(parametersExtension, network, cracFile, solver, data, result);
        }

        result.addExtension(ClosedOptimisationRaoResult.class, resultExtension);

        network.getVariantManager().allowVariantMultiThreadAccess(false);
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
