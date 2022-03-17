package com.farao_community.farao.search_tree_rao.linear_optimisation.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

public class SolverParameters {

    private final RaoParameters.Solver solver;
    private final double relativeMipGap;
    private final String solverSpecificParameters;

    public SolverParameters(RaoParameters.Solver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = solver;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
    }

    public RaoParameters.Solver getSolver() {
        return solver;
    }

    public double getRelativeMipGap() {
        return relativeMipGap;
    }

    public String getSolverSpecificParameters() {
        return solverSpecificParameters;
    }

    public static SolverParameters buildFromRaoParameters(RaoParameters raoParameters) {

        /*
        for now, values of SolverParameters are constant over all the SearchTreeRao
        they can therefore be instantiated directly from a RaoParameters
         */

        return new SolverParameters(raoParameters.getSolver(),
            raoParameters.getRelativeMipGap(),
            raoParameters.getSolverSpecificParameters());
    }
}
