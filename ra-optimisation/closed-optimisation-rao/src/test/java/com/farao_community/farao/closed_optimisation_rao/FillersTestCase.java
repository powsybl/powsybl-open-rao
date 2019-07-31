package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoParameters;
import com.farao_community.farao.closed_optimisation_rao.MPSolverMock;
import com.farao_community.farao.data.crac_file.CracFile;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import static com.farao_community.farao.closed_optimisation_rao.OptimisationComponentUtil.getFillersStack;
import static junit.framework.TestCase.assertTrue;

public class FillersTestCase {

    private CracFile cracFile;

    private Network network;

    private Queue<AbstractOptimisationProblemFiller> fillers;

    private HashMap<String, Object> data;

    private MPSolver solver;

    public FillersTestCase(CracFile cracFile, Network network, HashMap<String, Object> data, List<String> fillersToTest) {
        this.cracFile = cracFile;
        this.network = network;
        this.data = data;
        this.solver = new MPSolverMock();

        ClosedOptimisationRaoParameters parameters = new ClosedOptimisationRaoParameters();
        parameters.addAllFillers(fillersToTest);
        this.fillers = getFillersStack(network, cracFile, data, parameters);
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

    private boolean areVariablesPresent(List<String> variablesNames) {
        return variablesNames.stream().allMatch(v ->
                solver.lookupVariableOrNull(v) != null);
    }

    private boolean areConstraintsPresent(List<String> constraintsNames) {
        return constraintsNames.stream().allMatch(v ->
                solver.lookupConstraintOrNull(v) != null);
    }



}
