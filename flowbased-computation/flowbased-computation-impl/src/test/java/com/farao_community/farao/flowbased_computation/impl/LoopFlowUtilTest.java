/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation.impl;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LoopFlowUtil.class)
public class LoopFlowUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoopFlowUtilTest.class);

    private static final double EPSILON = 1e-3;
    private Network network;
    private Crac crac;
    private GlskProvider glskProvider;
    private ComputationManager computationManager;
    private FlowBasedComputationParameters parameters;
    private List<String> countries;

    @Before
    public void setUp() {
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        platformConfig.createModuleConfig("load-flow").setStringProperty("default", "MockLoadflow");
        network = ExampleGenerator.network();
        crac = ExampleGenerator.crac();
        glskProvider = ExampleGenerator.glskProvider();
        computationManager = LocalComputationManager.getDefault();
        parameters = FlowBasedComputationParameters.load(platformConfig);
        SensitivityComputationFactory sensitivityComputationFactory = ExampleGenerator.sensitivityComputationFactory();
        SensitivityComputationService.init(sensitivityComputationFactory, computationManager);
        countries = Arrays.asList("FR", "BE", "DE", "NL");
    }

    @Test
    public void testRunCrac() {
        FlowBasedComputationCracImpl flowBasedComputationCracImpl = new FlowBasedComputationCracImpl();
        assertEquals("SimpleIterativeFlowBasedCrac", flowBasedComputationCracImpl.getName());
        assertEquals("1.0.0", flowBasedComputationCracImpl.getVersion());
        assertNull(flowBasedComputationCracImpl.run(network, Mockito.mock(CracFile.class), glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters));
        FlowBasedComputationResult result = flowBasedComputationCracImpl.run(network, crac, glskProvider, computationManager, network.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(FlowBasedComputationResult.Status.SUCCESS, result.getStatus());

        Map<String, Double> refNetPositionCountryMap = new HashMap<>();
        refNetPositionCountryMap.put("FR", 100.0);
        refNetPositionCountryMap.put("BE", 0.0);
        refNetPositionCountryMap.put("DE", 0.0);
        refNetPositionCountryMap.put("NL", -100.0);
        PowerMockito.spy(LoopFlowUtil.class);
        try {
            PowerMockito.doReturn(refNetPositionCountryMap).when(LoopFlowUtil.class, "getRefNetPositionByCountry", any(), any());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Double> loopFlows = LoopFlowUtil.calculateLoopFlows(network, crac, glskProvider, countries, computationManager, parameters);
        assertEquals(0.0, loopFlows.get("FR-DE"), EPSILON);
        assertEquals(0.0, loopFlows.get("DE-NL"), EPSILON);
        assertEquals(0.0, loopFlows.get("FR-BE"), EPSILON);
        assertEquals(0.0, loopFlows.get("BE-NL"), EPSILON);
    }
}
