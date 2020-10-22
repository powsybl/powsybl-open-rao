/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.rao_mock.AnotherRaoProviderMock;
import com.farao_community.farao.rao_api.rao_mock.RaoProviderMock;
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
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
public class RaoTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private RaoInput raoInput;
    private ComputationManager computationManager;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        Network network = Mockito.mock(Network.class);
        Crac crac = Mockito.mock(Crac.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("v");
        raoInput = RaoInput.builder().withNetwork(network).withCrac(crac).withNetworkVariantId("variant-id").build();
        computationManager = Mockito.mock(ComputationManager.class);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testDefaultOneProvider() {
        // case with only one provider, no need for config
        // find rao
        Rao.Runner defaultRao = Rao.find(null, ImmutableList.of(new RaoProviderMock()), platformConfig);
        assertEquals("RandomRAO", defaultRao.getName());
        assertEquals("1.0", defaultRao.getVersion());

        // run rao
        RaoResult result = defaultRao.run(raoInput, new RaoParameters());
        assertNotNull(result);
        assertEquals(RaoResult.Status.SUCCESS, result.getStatus());
        RaoResult resultAsync = defaultRao.runAsync(raoInput, new RaoParameters()).join();
        assertNotNull(resultAsync);
        assertEquals(RaoResult.Status.SUCCESS, resultAsync.getStatus());
    }

    @Test(expected = FaraoException.class)
    public void testDefaultTwoProviders() {
        // case with two providers : should throw as no config defines which provider must be selected
        Rao.find(null, ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
    }

    @Test
    public void testDefinedAmongTwoProviders() {
        // case with two providers where one the two RAOs is specifically selected
        Rao.Runner definedRao = Rao.find("GlobalRAOptimizer", ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", definedRao.getName());
        assertEquals("2.3", definedRao.getVersion());
    }

    @Test(expected = FaraoException.class)
    public void testDefaultNoProvider() {
        // case with no provider
        Rao.find(null, ImmutableList.of(), platformConfig);
    }

    @Test
    public void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("rao").setStringProperty("default", "GlobalRAOptimizer");
        Rao.Runner globalRaOptimizer = Rao.find(null, ImmutableList.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", globalRaOptimizer.getName());
        assertEquals("2.3", globalRaOptimizer.getVersion());
    }

    @Test(expected = FaraoException.class)
    public void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("rao").setStringProperty("default", "UnknownRao");
        Rao.find(null, ImmutableList.of(new RaoProviderMock()), platformConfig);
    }
}
