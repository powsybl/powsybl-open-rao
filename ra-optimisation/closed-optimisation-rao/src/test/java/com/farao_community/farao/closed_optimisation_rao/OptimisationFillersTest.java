/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao;

import com.farao_community.farao.closed_optimisation_rao.fillers.*;
import com.farao_community.farao.closed_optimisation_rao.post_processors.BranchResultsPostProcessor;
import com.farao_community.farao.closed_optimisation_rao.post_processors.PstElementResultsPostProcessor;
import com.farao_community.farao.closed_optimisation_rao.post_processors.RedispatchElementResultsPostProcessor;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.ortools.linearsolver.MPSolver;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(MPSolver.class)
public class OptimisationFillersTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(MPSolver.class);
        when(MPSolver.infinity()).thenReturn(Double.POSITIVE_INFINITY);
    }

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

        Map<String, Object> data = new HashMap<>();
        Map<String, Double> pstSensitivities = new HashMap<>();
        Map<Pair<String, String>, Double> generatorSensitivities = new HashMap<>();
        Map<String, Double> referenceFlows = new HashMap<>();

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
        List<String> postProcessorsToTest = new ArrayList<>();

        fillersToTest.add(BranchMarginsPositivityConstraintFiller.class.getName());
        fillersToTest.add(BranchMarginsVariablesFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchCostsFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchVariablesFiller.class.getName());
        fillersToTest.add(PstAngleImpactOnBranchFlowFiller.class.getName());
        fillersToTest.add(PstAngleVariablesFiller.class.getName());
        fillersToTest.add(RedispatchCostMinimizationObjectiveFiller.class.getName());
        fillersToTest.add(RedispatchEquilibriumConstraintFiller.class.getName());
        fillersToTest.add(RedispatchImpactOnBranchFlowFiller.class.getName());

        postProcessorsToTest.add(BranchResultsPostProcessor.class.getName());
        postProcessorsToTest.add(PstElementResultsPostProcessor.class.getName());
        postProcessorsToTest.add(RedispatchElementResultsPostProcessor.class.getName());

        FillersTestCase testCase = new FillersTestCase(cracFile, network, data, fillersToTest, postProcessorsToTest);

        testCase.fillersTest();
        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        testCase.postProcessorsTest(raoComputationResult);
    }

    @Test
    public void testCase2() {
         /*
        Files : 5_3nodes_preContingency_PSTandRD_N-1
          - Test case with three nodes
          - preContingency and N-1 contingencies
          - 2 preventive redispatching remedial actions, all free-to-use
          - 1 curative PST remedial action, free-to-use
         */
        CracFile cracFile = JsonCracFile.read(CracFile.class.getResourceAsStream("/5_3nodes_preContingency_PSTandRD_N-1.json"));
        InputStream is = JsonClosedOptimisationRaoResultTest.class.getResourceAsStream("/5_3nodes_preContingency_PSTandRD_N-1.xiidm");
        Network network = Importers.loadNetwork("/5_3nodes_preContingency_PSTandRD_N-1.xiidm", is);

        Map<String, Object> data = new HashMap<>();
        Map<Pair<String, String>, Double> pstSensitivities = new HashMap<>();
        Map<Pair<String, String>, Double> generatorSensitivities = new HashMap<>();
        Map<String, Double> referenceFlows = new HashMap<>();

        pstSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "PST"), -69.81317138671875);
        pstSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_3", "PST"), -139.6263427734375);
        pstSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "PST"), -139.6263427734375);
        pstSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_3", "PST"), 186.16845703125);
        pstSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "PST"), -139.6263427734375);
        pstSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_2", "PST"), 186.16845703125);
        pstSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_3", "PST"), -139.6263427734375);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_3", "GENERATOR_FR"), 0.21739129722118378);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_3", "GENERATOR_BE"), 0.28260868787765503);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE"), 0.28260868787765503);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR"), -0.21739129722118378);
        generatorSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_3", "GENERATOR_BE"), 0.18840579688549042);
        generatorSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_2", "GENERATOR_BE"), 0.37681159377098083);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE"), -0.14130434393882751);
        generatorSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR"), -0.28985506296157837);
        generatorSensitivities.put(Pair.of("C2_MONITORED_FRANCE_BELGIUM_3", "GENERATOR_FR"), -0.14492753148078918);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR"), -0.21739129722118378);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_1", "GENERATOR_BE"), 0.28260868787765503);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_3", "GENERATOR_FR"), -0.21739129722118378);
        generatorSensitivities.put(Pair.of("C1_MONITORED_FRANCE_BELGIUM_3", "GENERATOR_BE"), 0.28260868787765503);
        generatorSensitivities.put(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR"), -0.10869564861059189);

        referenceFlows.put("MONITORED_FRANCE_BELGIUM_1", -158.11209106445312);
        referenceFlows.put("MONITORED_FRANCE_BELGIUM_3", -316.22418212890625);
        referenceFlows.put("MONITORED_FRANCE_BELGIUM_2", -316.22418212890625);
        referenceFlows.put("C1_MONITORED_FRANCE_BELGIUM_1", -316.22418212890625);
        referenceFlows.put("C2_MONITORED_FRANCE_BELGIUM_3", -378.36773681640625);
        referenceFlows.put("C2_MONITORED_FRANCE_BELGIUM_2", -421.63226318359375);
        referenceFlows.put("C1_MONITORED_FRANCE_BELGIUM_3", -316.22418212890625);

        data.put("pst_branch_sensitivities", pstSensitivities);
        data.put("generators_branch_sensitivities", generatorSensitivities);
        data.put("reference_flows", referenceFlows);

        List<String> fillersToTest = new ArrayList<>();
        List<String> postProcessorsToTest = new ArrayList<>();

        fillersToTest.add(BranchMarginsPositivityConstraintFiller.class.getName());
        fillersToTest.add(BranchMarginsVariablesFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchCostsFiller.class.getName());
        fillersToTest.add(GeneratorRedispatchVariablesFiller.class.getName());
        fillersToTest.add(PstAngleImpactOnBranchFlowFiller.class.getName());
        fillersToTest.add(PstAngleVariablesFiller.class.getName());
        fillersToTest.add(RedispatchCostMinimizationObjectiveFiller.class.getName());
        fillersToTest.add(RedispatchEquilibriumConstraintFiller.class.getName());
        fillersToTest.add(RedispatchImpactOnBranchFlowFiller.class.getName());

        postProcessorsToTest.add(BranchResultsPostProcessor.class.getName());
        postProcessorsToTest.add(PstElementResultsPostProcessor.class.getName());
        postProcessorsToTest.add(RedispatchElementResultsPostProcessor.class.getName());

        FillersTestCase testCase = new FillersTestCase(cracFile, network, data, fillersToTest, postProcessorsToTest);

        testCase.fillersTest();
        RaoComputationResult raoComputationResult = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        testCase.postProcessorsTest(raoComputationResult);
    }
}
