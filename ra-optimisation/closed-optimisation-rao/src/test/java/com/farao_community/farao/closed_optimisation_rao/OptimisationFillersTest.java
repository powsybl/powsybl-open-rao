package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.closed_optimisation_rao.fillers.BranchMarginsVariablesFiller;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OptimisationFillersTest {

    private static double tolerance = 1e-6;

    private boolean areVariablesPresent(List<String> variablesNames, MPSolver solver) {
        return variablesNames.stream().allMatch(v ->
            solver.lookupVariableOrNull(v) != null);
    }

    private boolean areConstraintsPresent(List<String> constraintsNames, MPSolver solver) {
        return constraintsNames.stream().allMatch(v ->
                solver.lookupConstraintOrNull(v) != null);
    }

    @Test
    public void curativeAndPreventiveFreeToUseRedispatching() {
        /*
        Files : 4_2nodes_preContingency_RD_N-1
          - Test case with two nodes
          - preContingency and N-1 contingencies
          - 4 redispatching remedial actions, all free-to-use : 2 preventive, 2 curative
         */

        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.json"));
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.xiidm");
        Network network = Importers.loadNetwork("/4_2nodes_preContingency_RD_N-1.xiidm", is);

        // build manually the required data Map to bypass the load-flow computation and test independently the fillers
        HashMap<String, Object> data = new HashMap<>();
        HashMap<String, Double> pstSensitivities = new HashMap<>();
        HashMap<Pair<String, String>, Double> generatorSensitivities = new HashMap<>();
        HashMap<String, Double> referenceFlows = new HashMap<>();

        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE_1.1"), 0.3786);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE_1.2"), 0.3786);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR_1"), -0.4348);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR_2"), -0.4348);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.1"), 0.2899);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.2"), 0.2899);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_1"), 0.5652);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_2"), 0.5652);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.1"), -0.1449);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE_1.2"), -0.1449);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_1"), 0.1884);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR_2"), 0.1884);

        referenceFlows.put("MONITORED_FRANCE_BELGIUM_1", -266.667);
        referenceFlows.put("MONITORED_FRANCE_BELGIUM_2", -533.333);
        referenceFlows.put("C1_MONITORED_FRANCE_BELGIUM_1", -800.000);

        data.put("pst_branch_sensitivities", pstSensitivities);
        data.put("generator_branch_sensitivities", generatorSensitivities);
        data.put("reference_flows", referenceFlows);

        MPSolver solver = new MPSolverMock();

        // BranchMarginVariablesFiller
        BranchMarginsVariablesFiller filler1 = new BranchMarginsVariablesFiller();
        filler1.initFiller(network, cracFile, data);

        List<String> variablesProvided = filler1.variablesProvided();
        List<String> constraintsProvided = filler1.constraintsProvided();

        assertTrue(variablesProvided.contains("MONITORED_FRANCE_BELGIUM_1_estimated_flow"));
        assertTrue(variablesProvided.contains("C1_MONITORED_FRANCE_BELGIUM_1_estimated_flow"));
        assertTrue(constraintsProvided.contains("MONITORED_FRANCE_BELGIUM_1_estimated_flow_equation"));
        assertTrue(constraintsProvided.contains("C1_MONITORED_FRANCE_BELGIUM_1_estimated_flow_equation"));

        filler1.fillProblem(solver);
        assertTrue(areVariablesPresent(variablesProvided, solver));
        assertTrue(areConstraintsPresent(constraintsProvided, solver));
        assertEquals(-266.667, solver.lookupConstraintOrNull("MONITORED_FRANCE_BELGIUM_1_estimated_flow_equation").lb(), tolerance);
        assertEquals(-266.667, solver.lookupConstraintOrNull("MONITORED_FRANCE_BELGIUM_1_estimated_flow_equation").ub(), tolerance);
        assertEquals(1, solver.lookupConstraintOrNull("MONITORED_FRANCE_BELGIUM_1_estimated_flow_equation").
                getCoefficient(solver.lookupVariableOrNull("MONITORED_FRANCE_BELGIUM_1_estimated_flow")), tolerance);

    }

}
