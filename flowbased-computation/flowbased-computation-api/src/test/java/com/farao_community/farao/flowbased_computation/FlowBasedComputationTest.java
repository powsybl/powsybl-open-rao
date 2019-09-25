/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.flowbased_computation.glsk_provider.GlskProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey <sebastien.murgey at rte-france.com>
 */
public class FlowBasedComputationTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    private Network network;

    private CracFile cracFile;

    private GlskProvider glskProvider;

    private ComputationManager computationManager;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        network = Mockito.mock(Network.class);
        cracFile = Mockito.mock(CracFile.class);
        glskProvider = Mockito.mock(GlskProvider.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("v");
        computationManager = Mockito.mock(ComputationManager.class);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testDefaultOneProvider() {
        // case with only one provider, no need for config
        FlowBasedComputation.Runner defaultFlowBased = FlowBasedComputation.find(null, ImmutableList.of(new FlowBasedComputationProviderMock()), platformConfig);
        assertEquals("FlowBasedComputationMock", defaultFlowBased.getName());
        assertEquals("1.0", defaultFlowBased.getVersion());
        FlowBasedComputationResult result = defaultFlowBased.run(network, cracFile, glskProvider, computationManager, new FlowBasedComputationParameters());
        assertNotNull(result);
        FlowBasedComputationResult resultAsync = defaultFlowBased.runAsync(network, cracFile, glskProvider, computationManager, new FlowBasedComputationParameters()).join();
        assertNotNull(resultAsync);
    }

    @Test(expected = FaraoException.class)
    public void testDefaultTwoProviders() {
        FlowBasedComputation.find(null, ImmutableList.of(new FlowBasedComputationProviderMock(), new AnotherFlowBasedComputationProviderMock()), platformConfig);
    }

    @Test(expected = FaraoException.class)
    public void testDefaultNoProvider() {
        FlowBasedComputation.find(null, ImmutableList.of(), platformConfig);
    }

    @Test
    public void testTwoProviders() {
        // case with 2 providers without any config but specifying which one to use programmatically
        FlowBasedComputation.Runner otherFlowBasedComputation = FlowBasedComputation.find("AnotherFlowBasedComputationMock", ImmutableList.of(new FlowBasedComputationProviderMock(), new AnotherFlowBasedComputationProviderMock()), platformConfig);
        assertEquals("AnotherFlowBasedComputationMock", otherFlowBasedComputation.getName());
    }

    @Test
    public void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("flowbased-computation").setStringProperty("default", "AnotherFlowBasedComputationMock");
        FlowBasedComputation.Runner otherFlowBasedComputation2 = FlowBasedComputation.find(null, ImmutableList.of(new FlowBasedComputationProviderMock(), new AnotherFlowBasedComputationProviderMock()), platformConfig);
        assertEquals("AnotherFlowBasedComputationMock", otherFlowBasedComputation2.getName());
    }

    @Test(expected = FaraoException.class)
    public void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("flowbased-computation").setStringProperty("default", "AnotherFlowBasedComputationMock");
        FlowBasedComputation.find(null, ImmutableList.of(new FlowBasedComputationProviderMock()), platformConfig);
    }
}
