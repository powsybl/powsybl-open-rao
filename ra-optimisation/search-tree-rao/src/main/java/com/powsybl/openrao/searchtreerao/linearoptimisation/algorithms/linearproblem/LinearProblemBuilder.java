/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.ProblemFillerHelper;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.*;
import com.powsybl.openrao.searchtreerao.linearoptimisation.inputs.IteratingLinearOptimizerInput;
import com.powsybl.openrao.searchtreerao.linearoptimisation.parameters.IteratingLinearOptimizerParameters;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;

import java.util.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblemBuilder {
    private final List<ProblemFiller> problemFillers = new ArrayList<>();
    private SearchTreeRaoRangeActionsOptimizationParameters.Solver solver;
    private double relativeMipGap = SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_RELATIVE_MIP_GAP;
    private String solverSpecificParameters = SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;
    private RangeActionActivationResult initialRangeActionActivationResult;

    public LinearProblem buildFromInputsAndParameters(IteratingLinearOptimizerInput inputs, IteratingLinearOptimizerParameters parameters) {
        Objects.requireNonNull(inputs);
        Objects.requireNonNull(parameters);

        this.withSolver(parameters.getSolverParameters().getSolver())
            .withRelativeMipGap(parameters.getSolverParameters().getRelativeMipGap())
            .withSolverSpecificParameters(parameters.getSolverParameters().getSolverSpecificParameters())
            .withInitialRangeActionActivationResult(inputs.raActivationFromParentLeaf());

        ProblemFillerHelper.getProblemFillers(inputs, parameters, inputs.optimizationPerimeter().getMainOptimizationState().getTimestamp().orElse(null)).forEach(this::withProblemFiller);

        return new LinearProblem(problemFillers, initialRangeActionActivationResult, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblem build() {
        return new LinearProblem(problemFillers, initialRangeActionActivationResult, solver, relativeMipGap, solverSpecificParameters);
    }

    public LinearProblemBuilder withProblemFiller(ProblemFiller problemFiller) {
        problemFillers.add(problemFiller);
        return this;
    }

    public LinearProblemBuilder withSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver solver) {
        this.solver = solver;
        return this;
    }

    public LinearProblemBuilder withRelativeMipGap(double relativeMipGap) {
        this.relativeMipGap = relativeMipGap;
        return this;
    }

    public LinearProblemBuilder withSolverSpecificParameters(String solverSpecificParameters) {
        this.solverSpecificParameters = solverSpecificParameters;
        return this;
    }

    public LinearProblemBuilder withInitialRangeActionActivationResult(RangeActionActivationResult rangeActionActivationResult) {
        this.initialRangeActionActivationResult = rangeActionActivationResult;
        return this;
    }
}
