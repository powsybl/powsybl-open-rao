package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.closed_optimisation_rao.fillers.*;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import static junit.framework.TestCase.assertTrue;

public class OptimisationFillersTest {

    @Test
    public void testCase1() {
        /*
        Files : 4_2nodes_preContingency_RD_N-1
          - Test case with two nodes
          - preContingency and N-1 contingencies
          - 4 redispatching remedial actions, all free-to-use : 2 preventive, 2 curative
         */
        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.json"));
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/4_2nodes_preContingency_RD_N-1.xiidm");
        Network network = Importers.loadNetwork("/4_2nodes_preContingency_RD_N-1.xiidm", is);

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
        data.put("generators_branch_sensitivities", generatorSensitivities);
        data.put("reference_flows", referenceFlows);

        List<String> fillersToTest = new ArrayList<>();

        fillersToTest.add(BranchMarginsPositivityConstraintFiller.class.getName());
        fillersToTest.add(BranchMarginsVariablesFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchCostsFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchVariablesFiller.class.getName());
        fillersToTest.add(PstAngleImpactOnBranchFlowFiller.class.getName());
        fillersToTest.add(PstAngleVariablesFiller.class.getName());
        fillersToTest.add(RedispatchCostMinimizationObjectiveFiller.class.getName());
        fillersToTest.add(RedispatchEquilibriumConstraintFiller.class.getName());
        fillersToTest.add(RedispatchImpactOnBranchFlowFiller.class.getName());
        FillersTestCase fillersTestCase = new FillersTestCase(cracFile, network, data, fillersToTest);

        fillersTestCase.fillersTest();

    }


    @Test
    public void test() {


    }
}