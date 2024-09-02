/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.raoapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.raoresultapi.ComputationStatus;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.raomock.AnotherRaoProviderMock;
import com.powsybl.openrao.raoapi.raomock.RaoProviderMock;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot <baptiste.seguinot at rte-france.com>
 */
class RaoTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private RaoInput raoInput;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        Network network = Mockito.mock(Network.class);
        Crac crac = Mockito.mock(Crac.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("v");
        raoInput = RaoInput.build(network, crac).withNetworkVariantId("variant-id").build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testDefaultOneProvider() {
        // case with only one provider, no need for config
        // find rao
        Rao.Runner defaultRao = Rao.find(null, List.of(new RaoProviderMock()), platformConfig);
        assertEquals("RandomRAO", defaultRao.getName());
        assertEquals("1.0", defaultRao.getVersion());

        // run rao
        RaoResult result = defaultRao.run(raoInput, new RaoParameters());
        assertNotNull(result);
        assertEquals(ComputationStatus.DEFAULT, result.getComputationStatus());
        RaoResult resultAsync = defaultRao.runAsync(raoInput, new RaoParameters()).join();
        assertNotNull(resultAsync);
        assertEquals(ComputationStatus.DEFAULT, resultAsync.getComputationStatus());
    }

    @Test
    void testDefaultTwoProviders() {
        // case with two providers : should throw as no config defines which provider must be selected
        List<RaoProvider> raoProviders = List.of(new RaoProviderMock(), new AnotherRaoProviderMock());
        assertThrows(OpenRaoException.class, () -> Rao.find(null, raoProviders, platformConfig));
    }

    @Test
    void testDefinedAmongTwoProviders() {
        // case with two providers where one the two RAOs is specifically selected
        Rao.Runner definedRao = Rao.find("GlobalRAOptimizer", List.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", definedRao.getName());
        assertEquals("2.3", definedRao.getVersion());
    }

    @Test
    void testDefaultNoProvider() {
        // case with no provider
        List<RaoProvider> raoProviders = List.of();
        assertThrows(OpenRaoException.class, () -> Rao.find(null, raoProviders, platformConfig));
    }

    @Test
    void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("rao").setStringProperty("default", "GlobalRAOptimizer");
        Rao.Runner globalRaOptimizer = Rao.find(null, List.of(new RaoProviderMock(), new AnotherRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", globalRaOptimizer.getName());
        assertEquals("2.3", globalRaOptimizer.getVersion());
    }

    @Test
    void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("rao").setStringProperty("default", "UnknownRao");
        List<RaoProvider> raoProviders = List.of(new RaoProviderMock());
        assertThrows(OpenRaoException.class, () -> Rao.find(null, raoProviders, platformConfig));
    }
}
