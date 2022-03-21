/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
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
