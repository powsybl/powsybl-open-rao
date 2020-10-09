/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flowbased_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
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
public class FlowbasedComputationTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    private Network network;

    private Crac crac;

    private GlskProvider glskProvider;

    private ComputationManager computationManager;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        network = Mockito.mock(Network.class);
        crac = Mockito.mock(Crac.class);
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
        FlowbasedComputation.Runner defaultFlowBased = FlowbasedComputation.find(null, ImmutableList.of(new FlowbasedComputationProviderMock()), platformConfig);
        assertEquals("FlowBasedComputationMock", defaultFlowBased.getName());
        assertEquals("1.0", defaultFlowBased.getVersion());
        FlowbasedComputationResult result = defaultFlowBased.run(network, crac, glskProvider, computationManager, new FlowbasedComputationParameters());
        assertNotNull(result);
        FlowbasedComputationResult resultAsync = defaultFlowBased.runAsync(network, crac, glskProvider, computationManager, new FlowbasedComputationParameters()).join();
        assertNotNull(resultAsync);
    }

    @Test(expected = FaraoException.class)
    public void testDefaultTwoProviders() {
        FlowbasedComputation.find(null, ImmutableList.of(new FlowbasedComputationProviderMock(), new AnotherFlowbasedComputationProviderMock()), platformConfig);
    }

    @Test(expected = FaraoException.class)
    public void testDefaultNoProvider() {
        FlowbasedComputation.find(null, ImmutableList.of(), platformConfig);
    }

    @Test
    public void testTwoProviders() {
        // case with 2 providers without any config but specifying which one to use programmatically
        FlowbasedComputation.Runner otherFlowBasedComputation = FlowbasedComputation.find("AnotherFlowBasedComputationMock", ImmutableList.of(new FlowbasedComputationProviderMock(), new AnotherFlowbasedComputationProviderMock()), platformConfig);
        assertEquals("AnotherFlowBasedComputationMock", otherFlowBasedComputation.getName());
    }

    @Test
    public void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("flowbased-computation").setStringProperty("default", "AnotherFlowBasedComputationMock");
        FlowbasedComputation.Runner otherFlowBasedComputation2 = FlowbasedComputation.find(null, ImmutableList.of(new FlowbasedComputationProviderMock(), new AnotherFlowbasedComputationProviderMock()), platformConfig);
        assertEquals("AnotherFlowBasedComputationMock", otherFlowBasedComputation2.getName());
    }

    @Test(expected = FaraoException.class)
    public void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("flowbased-computation").setStringProperty("default", "AnotherFlowBasedComputationMock");
        FlowbasedComputation.find(null, ImmutableList.of(new FlowbasedComputationProviderMock()), platformConfig);
    }
}
