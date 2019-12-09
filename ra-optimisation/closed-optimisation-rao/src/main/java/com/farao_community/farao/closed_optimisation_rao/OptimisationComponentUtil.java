/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.OPTIMISATION_CONSTANTS_DATA_NAME;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class OptimisationComponentUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimisationComponentUtil.class);

    private OptimisationComponentUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    private static boolean isProblemComplete(List<AbstractOptimisationProblemFiller> fillers, Map<String, Object> data) {
        /**
         * Check if problem is complete, that means:
         * - all expected data has its provider
         * - all expected variable has its provider
         * - all expected constraint has its provider
         */
        List<String> expectedData = new ArrayList<>();
        List<String> expectedVariables = new ArrayList<>();
        List<String> expectedConstraints = new ArrayList<>();
        List<String> providedVariables = new ArrayList<>();
        List<String> providedConstraints = new ArrayList<>();

        for (AbstractOptimisationProblemFiller filler : fillers) {
            providedVariables.addAll(filler.variablesProvided());
            providedConstraints.addAll(filler.constraintsProvided());
            expectedData.addAll(filler.dataExpected().keySet());
            expectedVariables.addAll(filler.variablesExpected());
            expectedConstraints.addAll(filler.constraintsExpected());
        }

        // Logging in case of uncomplete problem
        expectedData.stream().filter(d -> !data.containsKey(d))
            .forEach(d -> LOGGER.error(String.format("Data '%s' expected but not provided to the RAO engine", d)));
        expectedVariables.stream().filter(variable -> !providedVariables.contains(variable))
                .forEach(variable -> LOGGER.error(String.format("Variable '%s' expected but not provided to the RAO engine", variable)));
        expectedConstraints.stream().filter(constraint -> !providedConstraints.contains(constraint))
                .forEach(constraint -> LOGGER.error(String.format("Constraint '%s' expected but not provided to the RAO engine", constraint)));

        return data.keySet().containsAll(expectedData) &&
                providedVariables.containsAll(expectedVariables) &&
                providedConstraints.containsAll(expectedConstraints);
    }

    public static Queue<AbstractOptimisationProblemFiller> getFillersStack(Network network, CracFile cracFile, Map<String, Object> data, ClosedOptimisationRaoParameters parameters) {
        // List available optimisation problem fillers on the platform
        Map<String, AbstractOptimisationProblemFiller> fillersMap = new HashMap<>();
        for (AbstractOptimisationProblemFiller filler : ServiceLoader.load(AbstractOptimisationProblemFiller.class)) {
            fillersMap.put(filler.getClass().getName(), filler);
        }

        // Only keep expected optimisation problem fillers
        // All should be available, checked previously
        List<AbstractOptimisationProblemFiller> fillers = fillersMap.values().stream()
                .filter(filler -> parameters.getFillersList().contains(filler.getClass().getName()))
                .collect(Collectors.toList());

        // Initialize optimisation problem fillers
        fillers.forEach(filler -> filler.initFiller(network, cracFile, data));

        // Check that the problem is complete
        if (!isProblemComplete(fillers, data)) {
            throw new FaraoException("Problem is not complete");
        }

        // Sort optimisation problem fillers based on their dependencies
        // TODO topological sorting algorithm to improve sorting performances
        Queue<AbstractOptimisationProblemFiller> fillersOrdered = new LinkedList<>();
        List<String> providedVariables = new ArrayList<>();
        List<String> providedConstraints = new ArrayList<>();
        AtomicBoolean fillerPickedAtIteration = new AtomicBoolean(true);

        while (fillersOrdered.size() != fillers.size() && fillerPickedAtIteration.get()) {
            fillerPickedAtIteration.set(false);
            fillers.forEach(filler -> {
                if (fillersOrdered.contains(filler)) {
                    return; // filler already sorted
                }
                if (!providedVariables.containsAll(filler.variablesExpected()) ||
                        !providedConstraints.containsAll(filler.constraintsExpected())) {
                    return; // dependency not yet available
                }
                providedVariables.addAll(filler.variablesProvided());
                providedConstraints.addAll(filler.constraintsProvided());
                fillersOrdered.add(filler);
                fillerPickedAtIteration.set(true);
            });
        }
        // end of TODO

        return fillersOrdered;
    }

    public static Map<String, Object> getDataMap(Network network, CracFile cracFile, ComputationManager computationManager, ClosedOptimisationRaoParameters parameters) {
        Map<String, Object> data = new HashMap<>();
        // List available optimisation pre-processors on the platform
        Map<String, OptimisationPreProcessor> preProcessorMap = new HashMap<>();
        for (OptimisationPreProcessor preProcessor : ServiceLoader.load(OptimisationPreProcessor.class)) {
            preProcessorMap.put(preProcessor.getClass().getName(), preProcessor);
        }

        // Only keep expected optimisation pre-processors
        // All should be available, checked previously
        List<OptimisationPreProcessor> preProcessors = preProcessorMap.values().stream()
                .filter(filler -> parameters.getPreProcessorsList().contains(filler.getClass().getName()))
                .collect(Collectors.toList());

        // add rao closed-optimisation parameters in data
        data.put(OPTIMISATION_CONSTANTS_DATA_NAME, ConfigurationUtil.getOptimisationConstants(parameters));

        for (OptimisationPreProcessor preProcessor : preProcessors) {
            preProcessor.fillData(network, cracFile, computationManager, data);
        }
        return data;
    }

    public static void fillResults(ClosedOptimisationRaoParameters parameters, Network network, CracFile cracFile, MPSolver solver, Map<String, Object> data, RaoComputationResult result) {
        // List available optimisation post-processors on the platform
        Map<String, OptimisationPostProcessor> postProcessorMap = new HashMap<>();
        for (OptimisationPostProcessor postProcessor : ServiceLoader.load(OptimisationPostProcessor.class)) {
            postProcessorMap.put(postProcessor.getClass().getName(), postProcessor);
        }

        // Only keep expected optimisation post-processors
        // All should be available, checked previously
        parameters.getPostProcessorsList().forEach(postProcessorName -> {
            postProcessorMap.get(postProcessorName).fillResults(network, cracFile, solver, data, result);
        });
    }
}
