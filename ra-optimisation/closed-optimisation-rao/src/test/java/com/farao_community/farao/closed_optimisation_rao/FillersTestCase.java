/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.closed_optimisation_rao.mocks.MPSolverMock;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.OptimisationComponentUtil.getFillersStack;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FillersTestCase {

    private CracFile cracFile;

    private Network network;

    private Queue<AbstractOptimisationProblemFiller> fillers;

    private List<String> postProcessors;

    private Map<String, Object> data;

    private MPSolver solver;

    public FillersTestCase(CracFile cracFile, Network network, Map<String, Object> data, List<String> fillersToTest, List<String> postProcessors) {
        this.cracFile = cracFile;
        this.network = network;
        this.data = data;
        this.solver = new MPSolverMock();
        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();
        parameters.addAllFillers(fillersToTest);
        this.fillers = getFillersStack(network, cracFile, data, parameters);
        this.postProcessors = postProcessors;

    }

    public void fillersTest() {
        fillers.forEach(filler -> {
            assertTrue(areVariablesPresent(filler.variablesExpected()));
            assertTrue(areConstraintsPresent(filler.constraintsExpected()));
            filler.fillProblem(solver);
            assertTrue(areVariablesPresent(filler.variablesProvided()));
            assertTrue(areConstraintsPresent(filler.constraintsProvided()));
        });
    }

    public void postProcessorsTest(RaoComputationResult raoComputationResult) {
        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();
        parameters.addAllPostProcessors(this.postProcessors);

        // load processor <String = class name, OptimisationPostProcessor = proccessor object>
        HashMap<String, OptimisationPostProcessor> proccessorsMap = new HashMap<>();
        for (OptimisationPostProcessor postProcesor : ServiceLoader.load(OptimisationPostProcessor.class)) {
            proccessorsMap.put(postProcesor.getClass().getName(), postProcesor);
        }

        // list of processors you want test
        List<OptimisationPostProcessor> processorsList = proccessorsMap.values().stream()
                .filter(filler -> parameters.getPostProcessorsList().contains(filler.getClass().getName()))
                .collect(Collectors.toList());

        // randomly fill the solution of the RAO optimisation
        MPSolverMock solverMock = (MPSolverMock) solver;
        solverMock.randomSolve();

        // start fileResultsMethods
        processorsList.stream().forEach(processor -> {
            processor.fillResults(network, cracFile, solverMock, data, raoComputationResult);
        });
    }

    private boolean areVariablesPresent(List<String> variablesNames) {
        return variablesNames.stream().allMatch(v ->
                solver.lookupVariableOrNull(v) != null);
    }

    private boolean areConstraintsPresent(List<String> constraintsNames) {
        return constraintsNames.stream().allMatch(v ->
                solver.lookupConstraintOrNull(v) != null);
    }

}
