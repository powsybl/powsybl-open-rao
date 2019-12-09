/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoParameters;
import com.farao_community.farao.closed_optimisation_rao.ConfigurationUtil;
import com.farao_community.farao.closed_optimisation_rao.mocks.MockSensitivityComputationFactory;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.util.SensitivityComputationService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.OPTIMISATION_CONSTANTS_DATA_NAME;
import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class SensitivityPreProcessorTest {
    private SensitivityPreProcessor sensitivityPreProcessor;

    @Before
    public void setUp() throws Exception {
        sensitivityPreProcessor = new SensitivityPreProcessor();
        ComputationManager computationManager = new LocalComputationManager();
        SensitivityComputationFactory sensitivityComputationFactory = new MockSensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
    }

    @Test
    public void checkProvidedDataListing() {
        Map<String, Class> dataProvided = sensitivityPreProcessor.dataProvided();
        assertTrue(dataProvided.containsKey("pst_branch_sensitivities"));
        assertEquals(Map.class, dataProvided.get("pst_branch_sensitivities"));
        assertTrue(dataProvided.containsKey("generators_branch_sensitivities"));
        assertEquals(Map.class, dataProvided.get("generators_branch_sensitivities"));
        assertTrue(dataProvided.containsKey("reference_flows"));
        assertEquals(Map.class, dataProvided.get("reference_flows"));
    }

    @Test
    public void checkProvidedDataContent() throws Exception {
        Network network = Importers.loadNetwork("5_3nodes_PSTandRD_N-1.xiidm", getClass().getResourceAsStream("/5_3nodes_PSTandRD_N-1.xiidm"));
        CracFile cracFile = JsonCracFile.read(getClass().getResourceAsStream("/5_3nodes_PSTandRD_N-1.json"));
        ComputationManager computationManager = new LocalComputationManager();
        Map<String, Object> dataToFeed = new HashMap<>();
        dataToFeed.put(OPTIMISATION_CONSTANTS_DATA_NAME, ConfigurationUtil.getOptimisationConstants(new ClosedOptimisationRaoParameters()));

        sensitivityPreProcessor.fillData(network, cracFile, computationManager, dataToFeed);

        assertTrue(dataToFeed.containsKey("pst_branch_sensitivities"));
        assertTrue(dataToFeed.containsKey("generators_branch_sensitivities"));
        assertTrue(dataToFeed.containsKey("reference_flows"));
        assertTrue(dataToFeed.get("pst_branch_sensitivities") instanceof Map);
        Map<Pair<String, String>, Double> pstBranchSensitivities = (Map<Pair<String, String>, Double>) dataToFeed.get("pst_branch_sensitivities");

        assertTrue(pstBranchSensitivities.containsKey(Pair.of("MONITORED_FRANCE_BELGIUM_1", "PST")));
        assertTrue(pstBranchSensitivities.containsKey(Pair.of("C2_MONITORED_FRANCE_BELGIUM_2", "PST")));

        assertTrue(dataToFeed.get("generators_branch_sensitivities") instanceof Map);
        Map<Pair<String, String>, Double> generatorBranchSensitivities = (Map<Pair<String, String>, Double>) dataToFeed.get("generators_branch_sensitivities");
        assertTrue(generatorBranchSensitivities.containsKey(Pair.of("MONITORED_FRANCE_BELGIUM_1", "GENERATOR_FR")));
        assertTrue(generatorBranchSensitivities.containsKey(Pair.of("C2_MONITORED_FRANCE_BELGIUM_2", "GENERATOR_FR")));

        assertTrue(dataToFeed.get("reference_flows") instanceof Map);
        Map<String, Double> referenceFlows = (Map<String, Double>) dataToFeed.get("reference_flows");
        assertTrue(referenceFlows.containsKey("MONITORED_FRANCE_BELGIUM_1"));
        assertTrue(referenceFlows.containsKey("C2_MONITORED_FRANCE_BELGIUM_2"));
    }
}
