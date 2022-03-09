package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.*;

import java.util.*;

public class LinearProblemBuilder {

    private final List<ProblemFiller> problemFillers = new ArrayList<>();
    private FaraoMPSolver solver;
    private double relativeMipGap = RaoParameters.DEFAULT_RELATIVE_MIP_GAP;
    private String solverSpecificParameters = RaoParameters.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

    public LinearProblemBuilder withProblemFiller(ProblemFiller problemFiller) {
        problemFillers.add(problemFiller);
        return this;
    }

    public LinearProblemBuilder withSolver(FaraoMPSolver solver) {
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

    public LinearProblem build() {
        // TODO: add checks on fillers consistency
        // create and build linearProblem
        return new LinearProblem(problemFillers, solver, relativeMipGap, solverSpecificParameters);
    }
}
