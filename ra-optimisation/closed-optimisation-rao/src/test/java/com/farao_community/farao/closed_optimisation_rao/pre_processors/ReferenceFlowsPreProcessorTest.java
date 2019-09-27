/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.farao_community.farao.closed_optimisation_rao.mocks.MockLoadFlowProvider;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.util.LoadFlowService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ReferenceFlowsPreProcessorTest {
    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private ReferenceFlowsPreProcessor referenceFlowsPreProcessor;

    @Before
    public void setUp() throws Exception {
        referenceFlowsPreProcessor = new ReferenceFlowsPreProcessor();
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "Mock");
        ComputationManager computationManager = new LocalComputationManager();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find("Mock", Collections.singletonList(new MockLoadFlowProvider()), platformConfig);
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Test
    public void checkProvidedDataListing() {
        Map<String, Class> dataProvided = referenceFlowsPreProcessor.dataProvided();
        assertTrue(dataProvided.containsKey("reference_flows"));
        assertEquals(Map.class, dataProvided.get("reference_flows"));
    }

    @Test
    public void checkProvidedDataContent() throws Exception {
        Network network = Importers.loadNetwork("5_3nodes_PSTandRD_N-1.xiidm", getClass().getResourceAsStream("/5_3nodes_PSTandRD_N-1.xiidm"));
        CracFile cracFile = JsonCracFile.read(getClass().getResourceAsStream("/5_3nodes_PSTandRD_N-1.json"));
        ComputationManager computationManager = new LocalComputationManager();
        Map<String, Object> dataToFeed = new HashMap<>();

        referenceFlowsPreProcessor.fillData(network, cracFile, computationManager, dataToFeed);

        assertTrue(dataToFeed.containsKey("reference_flows"));
        assertTrue(dataToFeed.get("reference_flows") instanceof Map);
        Map<String, Double> referenceFlows = (Map<String, Double>) dataToFeed.get("reference_flows");

        assertTrue(referenceFlows.containsKey("MONITORED_FRANCE_BELGIUM_1"));
        assertTrue(referenceFlows.containsKey("C2_MONITORED_FRANCE_BELGIUM_2"));
    }

}
