/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.flowbased_domain.DataMonitoredBranch;
import com.farao_community.farao.data.flowbased_domain.DataPtdfPerCountry;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationProvider;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationResult;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.farao_community.farao.util.SensitivityComputationService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowBasedComputationImplTest {
    private static final double EPSILON = 1e-3;
    private FlowBasedComputationProvider flowBasedComputationProvider;
    private Network network;
    private CracFile cracFile;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "MockLoadflow");

        flowBasedComputationProvider = new FlowBasedComputationImpl();
        network = ExampleGenerator.network();
        cracFile = ExampleGenerator.cracFile();
        glskProvider = ExampleGenerator.glskProvider();
        computationManager = LocalComputationManager.getDefault();
        parameters = FlowBasedComputationParameters.load(platformConfig);
        SensitivityComputationFactory sensitivityComputationFactory = ExampleGenerator.sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
    }

    @Test
    public void testProviderName() {
        assertEquals("SimpleIterativeFlowBased", flowBasedComputationProvider.getName());
    }

    @Test
    public void testProviderVersion() {
        assertEquals("1.0.0", flowBasedComputationProvider.getVersion());
    }

    @Test
    public void testRun() {
        FlowBasedComputationResult result = flowBasedComputationProvider.run(network, cracFile, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(FlowBasedComputationResult.Status.SUCCESS, result.getStatus());

        Map<String, Double> frefResults = frefResultById(result);
        Map<String, Double> fmaxResults = fmaxResultById(result);
        Map<String, Map<String, Double>> ptdfResults = ptdfResultById(result);
        assertEquals(50, frefResults.get("FR-BE"), EPSILON);
        assertEquals(100, fmaxResults.get("FR-BE"), EPSILON);
        assertEquals(0.375, ptdfResults.get("FR-BE").get("10YFR-RTE------C"), EPSILON);
        assertEquals(-0.375, ptdfResults.get("FR-BE").get("10YBE----------2"), EPSILON);
        assertEquals(0.125, ptdfResults.get("FR-BE").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.125, ptdfResults.get("FR-BE").get("10YNL----------L"), EPSILON);

        assertEquals(50, frefResults.get("FR-DE"), EPSILON);
        assertEquals(0.375, ptdfResults.get("FR-DE").get("10YFR-RTE------C"), EPSILON);
        assertEquals(0.125, ptdfResults.get("FR-DE").get("10YBE----------2"), EPSILON);
        assertEquals(-0.375, ptdfResults.get("FR-DE").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.125, ptdfResults.get("FR-DE").get("10YNL----------L"), EPSILON);

        assertEquals(50, frefResults.get("BE-NL"), EPSILON);
        assertEquals(0.125, ptdfResults.get("BE-NL").get("10YFR-RTE------C"), EPSILON);
        assertEquals(0.375, ptdfResults.get("BE-NL").get("10YBE----------2"), EPSILON);
        assertEquals(-0.125, ptdfResults.get("BE-NL").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.375, ptdfResults.get("BE-NL").get("10YNL----------L"), EPSILON);

        assertEquals(50, frefResults.get("DE-NL"), EPSILON);
        assertEquals(0.125, ptdfResults.get("DE-NL").get("10YFR-RTE------C"), EPSILON);
        assertEquals(-0.125, ptdfResults.get("DE-NL").get("10YBE----------2"), EPSILON);
        assertEquals(0.375, ptdfResults.get("DE-NL").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.375, ptdfResults.get("DE-NL").get("10YNL----------L"), EPSILON);

        assertEquals(0, frefResults.get("N-1 FR-BE / FR-BE"), EPSILON);
        assertEquals(0, ptdfResults.get("N-1 FR-BE / FR-BE").get("10YFR-RTE------C"), EPSILON);
        assertEquals(0, ptdfResults.get("N-1 FR-BE / FR-BE").get("10YBE----------2"), EPSILON);
        assertEquals(0, ptdfResults.get("N-1 FR-BE / FR-BE").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(0, ptdfResults.get("N-1 FR-BE / FR-BE").get("10YNL----------L"), EPSILON);

        assertEquals(100, frefResults.get("N-1 FR-BE / FR-DE"), EPSILON);
        assertEquals(0.75, ptdfResults.get("N-1 FR-BE / FR-DE").get("10YFR-RTE------C"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / FR-DE").get("10YBE----------2"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / FR-DE").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / FR-DE").get("10YNL----------L"), EPSILON);

        assertEquals(0, frefResults.get("N-1 FR-BE / BE-NL"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / BE-NL").get("10YFR-RTE------C"), EPSILON);
        assertEquals(0.75, ptdfResults.get("N-1 FR-BE / BE-NL").get("10YBE----------2"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / BE-NL").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.25, ptdfResults.get("N-1 FR-BE / BE-NL").get("10YNL----------L"), EPSILON);

        assertEquals(100, frefResults.get("N-1 FR-BE / DE-NL"), EPSILON);
        assertEquals(0.5, ptdfResults.get("N-1 FR-BE / DE-NL").get("10YFR-RTE------C"), EPSILON);
        assertEquals(-0.5, ptdfResults.get("N-1 FR-BE / DE-NL").get("10YBE----------2"), EPSILON);
        assertEquals(0.5, ptdfResults.get("N-1 FR-BE / DE-NL").get("10YCB-GERMANY--8"), EPSILON);
        assertEquals(-0.5, ptdfResults.get("N-1 FR-BE / DE-NL").get("10YNL----------L"), EPSILON);

    }

    private Map<String, Double> frefResultById(FlowBasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFref));
    }

    private Map<String, Double> fmaxResultById(FlowBasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
                .collect(Collectors.toMap(
                        DataMonitoredBranch::getId,
                        DataMonitoredBranch::getFmax));
    }

    private Map<String, Map<String, Double>> ptdfResultById(FlowBasedComputationResult result) {
        return result.getFlowBasedDomain().getDataPreContingency().getDataMonitoredBranches().stream()
            .collect(Collectors.toMap(
                DataMonitoredBranch::getId,
                dataMonitoredBranch -> dataMonitoredBranch.getPtdfList().stream()
                .collect(Collectors.toMap(
                    DataPtdfPerCountry::getCountry,
                    DataPtdfPerCountry::getPtdf
                ))
            ));
    }
}
