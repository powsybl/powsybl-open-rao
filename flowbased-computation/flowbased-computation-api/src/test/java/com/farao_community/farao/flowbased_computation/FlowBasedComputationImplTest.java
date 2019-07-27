/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * FlowBased Computation Impl Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationImplTest {

    private FlowBasedComputationImpl flowBasedComputationImplMock;
    private Network network;
    private CracFile cracFile;
    private Instant instant;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;
    private LoadFlowFactory loadFlowFactory;
    private SensitivityComputationFactory sensitivityComputationFactory;

    @Before
    public void setup() throws IOException {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        cracFile = JsonCracFile.read(Files.newInputStream(Paths.get("src/test/resources/cracDataFlowBased.json")));
        instant = Instant.parse("2018-08-28T22:00:00Z");
        flowBasedGlskValuesProvider = new FlowBasedGlskValuesProvider(network, "src/test/resources/GlskCountry.xml");

        computationManager = Mockito.mock(ComputationManager.class);
        loadFlowFactory = Mockito.mock(LoadFlowFactory.class);
        sensitivityComputationFactory = Mockito.mock(SensitivityComputationFactory.class);
    }

    @Test (expected = NullPointerException.class)
    public void runTest() {
        flowBasedComputationImplMock = new FlowBasedComputationImpl(network,
                cracFile,
                flowBasedGlskValuesProvider,
                instant,
                computationManager,
                loadFlowFactory,
                sensitivityComputationFactory
                );
        FlowBasedComputationParameters parameters = Mockito.mock(FlowBasedComputationParameters.class);
        String workingStateId = "0";
        flowBasedComputationImplMock.run(workingStateId, parameters);
    }

    @Test
    public void runTestTre() {
        flowBasedComputationImplMock = new FlowBasedComputationImpl(network,
                cracFile,
                flowBasedGlskValuesProvider,
                instant,
                computationManager,
                loadFlowFactory,
                sensitivityComputationFactory
        );
        FlowBasedComputationParameters parameters = Mockito.mock(FlowBasedComputationParameters.class);
        String workingStateId = "0";
        flowBasedComputationImplMock.run(workingStateId, parameters);
    }
}
