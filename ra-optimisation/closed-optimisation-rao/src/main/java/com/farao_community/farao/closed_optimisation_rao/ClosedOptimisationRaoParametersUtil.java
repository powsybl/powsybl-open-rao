/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ClosedOptimisationRaoParametersUtil {

    private ClosedOptimisationRaoParametersUtil() {
        throw new AssertionError("Utility class should not have constructor");
    }

    public static MPSolverParameters getSolverParameters(ClosedOptimisationRaoParameters parameters, MPSolver solver) {
        // in or-tools, some parameters are defined in the MPSolver object...
        addParametersToSolver(parameters, solver);
        // ... while some other must be passed as a MPSolverParameters during the solve()
        return buildMPSolverParameters(parameters);
    }

    private static MPSolverParameters buildMPSolverParameters(ClosedOptimisationRaoParameters parameters) {
        MPSolverParameters solverParameters = new MPSolverParameters();
        solverParameters.setDoubleParam(MPSolverParameters.DoubleParam.RELATIVE_MIP_GAP, parameters.getRelativeMipGap());
        return solverParameters;
    }

    private static void addParametersToSolver(ClosedOptimisationRaoParameters parameters, MPSolver solver) {
        solver.setTimeLimit((int) parameters.getMaxTimeInSeconds()*1000); // read in milliseconds by setTimeLimit
    }

    public static Map<String, Double> getOptimisationConstants (ClosedOptimisationRaoParameters parameters) {
        Map<String, Double> constants = new HashMap<>();
        constants.put(ClosedOptimisationRaoNames.OVERLOAD_PENALTY_COST, parameters.getOverloadPenaltyCost());
        return constants;
    }
}
