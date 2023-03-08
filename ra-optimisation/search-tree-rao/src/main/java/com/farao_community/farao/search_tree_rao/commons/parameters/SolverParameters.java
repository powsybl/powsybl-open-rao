/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RangeActionsOptimizationParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
// TODO l replace with RangeActionsOptimizationParameters.LinearOptimizationSolver
public class SolverParameters {

    private final RangeActionsOptimizationParameters.Solver solver;
    private final double relativeMipGap;
    private final String solverSpecificParameters;

    public SolverParameters(RangeActionsOptimizationParameters.Solver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = solver;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
    }

    public RangeActionsOptimizationParameters.Solver getSolver() {
        return solver;
    }

    public double getRelativeMipGap() {
        return relativeMipGap;
    }

    public String getSolverSpecificParameters() {
        return solverSpecificParameters;
    }

    public static SolverParameters buildFromRaoParameters(RaoParameters raoParameters) {
        return new SolverParameters(raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver(),
            raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap(),
            raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SolverParameters that = (SolverParameters) o;
        return Double.compare(that.relativeMipGap, relativeMipGap) == 0 && solver == that.solver && Objects.equals(solverSpecificParameters, that.solverSpecificParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(solver, relativeMipGap, solverSpecificParameters);
    }
}
