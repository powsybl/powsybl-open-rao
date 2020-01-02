/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;

import java.util.*;

/**
 * Utility class designed to check availability of all plugins listed in the
 * RAO parameters, and to interpret them in different contexts of the closed
 * optimisation RAO.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class ConfigurationUtil {

    private ConfigurationUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Validates RAO computation parameters compatibility with platform available plugins.
     * Return a list of errors, that is empty if the configuration is correct.
     *
     * @param parameters RAO computation parameters
     * @return a list of configuration issues
     */
    public static List<String> checkRaoConfiguration(RaoComputationParameters parameters) {
        List<String> errors = new ArrayList<>();

        // Check that correct extension is provided
        // Return directly if the extension is not provided
        ClosedOptimisationRaoParameters parametersExtension = parameters.getExtension(ClosedOptimisationRaoParameters.class);
        if (Objects.isNull(parametersExtension)) {
            errors.add("Closed optimisation RAO computation parameters not available");
            return errors;
        }

        // Check that expected solver exists
        try {
            MPSolver.OptimizationProblemType.valueOf(parametersExtension.getSolverType());
        } catch (IllegalArgumentException e) {
            errors.add("Solver " + parametersExtension.getSolverType() + " not available");
        }

        // Check that relativeMipGap is in [0,1]
        if (parametersExtension.getRelativeMipGap() < 0 || parametersExtension.getRelativeMipGap() > 1) {
            errors.add("Relative MIP gap must be in [0;1] ( " + parametersExtension.getRelativeMipGap() + " not valid)");
        }

        // Check that maxTimeInSeconds is strictly positive
        if (parametersExtension.getMaxTimeInSeconds() <= 0) {
            errors.add("Max time must be strictly positive ( " + parametersExtension.getMaxTimeInSeconds() + " not valid)");
        }

        // Check that sensitivityThreshold is positive
        if (parametersExtension.getRdSensitivityThreshold() < 0) {
            errors.add("Redispatching sensitivity threshold must be strictly positive ( " + parametersExtension.getRdSensitivityThreshold() + " not valid)");
        }

        // Check that sensitivityThreshold is positive
        if (parametersExtension.getPstSensitivityThreshold() < 0) {
            errors.add("PST sensitivity threshold must be strictly positive ( " + parametersExtension.getPstSensitivityThreshold() + " not valid)");
        }

        // Check that the number of parallel threads is above 1
        if (parametersExtension.getNumberOfParallelThreads() < 1) {
            errors.add("Number of parallel threads must be at least 1 ( " + parametersExtension.getNumberOfParallelThreads() + " not valid)");
        }

        // Check that all expected pre-processors are provided
        parametersExtension.getPreProcessorsList().forEach(preProcessor -> {
            try {
                Class.forName(preProcessor);
            } catch (ClassNotFoundException e) {
                errors.add("Pre-processor " + preProcessor + " not available");
            }
        });

        // Check that all expected problem-fillers are provided
        parametersExtension.getFillersList().forEach(filler -> {
            try {
                Class.forName(filler);
            } catch (ClassNotFoundException e) {
                errors.add("Filler " + filler + " not available");
            }
        });

        // Check that all expected post-processors are provided
        parametersExtension.getPostProcessorsList().forEach(postProcessor -> {
            try {
                Class.forName(postProcessor);
            } catch (ClassNotFoundException e) {
                errors.add("Post-processor " + postProcessor + " not available");
            }
        });
        return errors;
    }

    /**
     * Get the optimisation constants given in the RAO parameters as a map readable
     * by the optimisation problem fillers
     */
    public static Map<String, Object> getOptimisationConstants(ClosedOptimisationRaoParameters parameters) {
        Map<String, Object> constants = new HashMap<>();
        constants.put(ClosedOptimisationRaoNames.OVERLOAD_PENALTY_COST_NAME, parameters.getOverloadPenaltyCost());
        constants.put(ClosedOptimisationRaoNames.RD_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME, parameters.getRdSensitivityThreshold());
        constants.put(ClosedOptimisationRaoNames.PST_SENSITIVITY_SIGNIFICANCE_THRESHOLD_NAME, parameters.getPstSensitivityThreshold());
        constants.put(ClosedOptimisationRaoNames.NUMBER_OF_PARALLEL_THREADS_NAME, parameters.getNumberOfParallelThreads());
        return constants;
    }

    /**
     * Update the or-tools object MPSolver, and get a MPSolverParameters aligned with
     * the configuration given in the RAO parameters
     */
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
        solver.setTimeLimit((int) (parameters.getMaxTimeInSeconds() * 1000.0)); // read in milliseconds by setTimeLimit
    }
}
