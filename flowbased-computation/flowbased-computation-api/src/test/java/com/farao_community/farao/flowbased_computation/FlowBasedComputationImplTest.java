/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.PtdfSensitivityConverter;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.sensitivity.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import com.powsybl.sensitivity.SensitivityFactor;

/**
 * FlowBased Computation Impl Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
public class FlowBasedComputationImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowBasedComputationImplTest.class);

    private FlowBasedComputationImpl flowBasedComputationImplMock;
    private Network network;
    private Network testNetwork;

    private CracFile cracFile;
    private CracFile testCracFile;

    private Instant instant;
    private FlowBasedGlskValuesProvider flowBasedGlskValuesProvider;
    private ComputationManager computationManager;
    private LoadFlowFactory loadFlowFactory;
    private SensitivityComputationFactory sensitivityComputationFactory;

//    class MockSensitivityComputationFactory implements SensitivityComputationFactory {
//
//        @Override
//        public SensitivityComputation create(Network network, ComputationManager computationManager, int i) {
//            return new MockSensitivityComputation();
//        }
//    }

//    class MockSensitivityComputation implements SensitivityComputation {
//
//        @Override
//        public CompletableFuture<SensitivityComputationResults> run(SensitivityFactorsProvider factorsProvider, String stateId, SensitivityComputationParameters parameters) {
//            List<SensitivityValue> values = factorsProvider.getFactors(testNetwork).stream()
//                    .map(factor -> new SensitivityValue(factor, 1, 2, 3))
//                    .collect(Collectors.toList());
//            return CompletableFuture.completedFuture(new SensitivityComputationResults(true, new HashMap<>(), "", values));
//        }
//
//        @Override
//        public String getName() {
//            return "Mock";
//        }
//
//        @Override
//        public String getVersion() {
//            return "0.0";
//        }
//    }

    @Before
    public void setup() throws IOException {
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        cracFile = JsonCracFile.read(Files.newInputStream(Paths.get("src/test/resources/cracDataFlowBased.json")));

        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        testCracFile = JsonCracFile.read(NetworkUtilTest.class.getResourceAsStream("/simpleInputs.json"));

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
    public void testFillFlowBasedComputationResult() {
        flowBasedComputationImplMock = new FlowBasedComputationImpl(network,
                cracFile,
                flowBasedGlskValuesProvider,
                instant,
                computationManager,
                loadFlowFactory,
                sensitivityComputationFactory
        );
        List<MonitoredBranch> branches = cracFile.getPreContingency().getMonitoredBranches();
        for (MonitoredBranch branch : branches) {
            LOGGER.info(branch.getId());
        }

        List<SensitivityValue> sensitivityValues = new ArrayList<>();

        SensitivityComputationResults sensitivityComputationResults = new SensitivityComputationResults(true, Collections.emptyMap(), "", sensitivityValues);

        Map<String, Double> referenceFlows = new HashMap<>();
        referenceFlows.put("BBE1AA1  BBE3AA1  1", 999.);
        referenceFlows.put("FFR2AA1  FFR3AA1  1", 999.);

        PtdfSensitivityConverter ptdfSensitivityConverter = new PtdfSensitivityConverter(testCracFile);
        List<SensitivityFactor> factors = ptdfSensitivityConverter.getFactors(testNetwork);
        for (SensitivityFactor factor : factors) {
            sensitivityValues.add(new SensitivityValue(factor, 1, 2, 3));
        }

        FlowBasedComputationResult flowBasedComputationResult = new FlowBasedComputationResult(FlowBasedComputationResult.Status.SUCCESS);
        flowBasedComputationImplMock.fillFlowBasedComputationResult(flowBasedComputationImplMock.getCracFile(),
                referenceFlows,
                sensitivityComputationResults,
                flowBasedComputationResult);
    }

    @Test
    public void testGetterSetter() {
        flowBasedComputationImplMock = new FlowBasedComputationImpl(network,
                cracFile,
                flowBasedGlskValuesProvider,
                instant,
                computationManager,
                loadFlowFactory,
                sensitivityComputationFactory
        );
        flowBasedComputationImplMock.getNetwork();
        flowBasedComputationImplMock.getCracFile();
        flowBasedComputationImplMock.getComputationManager();
        flowBasedComputationImplMock.getFlowBasedGlskValuesProvider();
        flowBasedComputationImplMock.getInstant();
        flowBasedComputationImplMock.setNetwork(flowBasedComputationImplMock.getNetwork());
        flowBasedComputationImplMock.setCracFile(flowBasedComputationImplMock.getCracFile());
        flowBasedComputationImplMock.setComputationManager(flowBasedComputationImplMock.getComputationManager());
        flowBasedComputationImplMock.setFlowBasedGlskValuesProvider(flowBasedComputationImplMock.getFlowBasedGlskValuesProvider());
        flowBasedComputationImplMock.setInstant(flowBasedComputationImplMock.getInstant());
    }
}
