/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.raomock.AnotherInterTemporalRaoProviderMock;
import com.powsybl.openrao.raoapi.raomock.InterTemporalRaoProviderMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class InterTemporalRaoTest {

    private FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;
    private InterTemporalRaoInput raoInput;

    @BeforeEach
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
        Network network = Mockito.mock(Network.class);
        Crac crac = Mockito.mock(Crac.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        Mockito.when(variantManager.getWorkingVariantId()).thenReturn("v");
        raoInput = new InterTemporalRaoInput(new TemporalDataImpl<>(Map.of(OffsetDateTime.of(2024, 12, 13, 16, 17, 0, 0, ZoneOffset.UTC), RaoInput.build(network, crac).build())), new HashSet<>());
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testDefaultOneProvider() {
        // case with only one provider, no need for config
        // find rao
        InterTemporalRao.Runner defaultRao = InterTemporalRao.find(null, List.of(new InterTemporalRaoProviderMock()), platformConfig);
        assertEquals("RandomInterTemporalRAO", defaultRao.getName());
        assertEquals("1.0", defaultRao.getVersion());

        // run rao
        GlobalRaoResult result = defaultRao.run(raoInput, new RaoParameters());
        assertNotNull(result);
    }

    @Test
    void testDefaultTwoProviders() {
        // case with two providers : should throw as no config defines which provider must be selected
        List<InterTemporalRaoProvider> raoProviders = List.of(new InterTemporalRaoProviderMock(), new AnotherInterTemporalRaoProviderMock());
        assertThrows(OpenRaoException.class, () -> InterTemporalRao.find(null, raoProviders, platformConfig));
    }

    @Test
    void testDefinedAmongTwoProviders() {
        // case with two providers where one the two RAOs is specifically selected
        InterTemporalRao.Runner definedRao = InterTemporalRao.find("GlobalRAOptimizer", List.of(new InterTemporalRaoProviderMock(), new AnotherInterTemporalRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", definedRao.getName());
        assertEquals("2.3", definedRao.getVersion());
    }

    @Test
    void testDefaultNoProvider() {
        // case with no provider
        List<InterTemporalRaoProvider> raoProviders = List.of();
        assertThrows(OpenRaoException.class, () -> InterTemporalRao.find(null, raoProviders, platformConfig));
    }

    @Test
    void testDefaultTwoProvidersPlatformConfig() {
        // case with 2 providers without any config but specifying which one to use in platform config
        platformConfig.createModuleConfig("rao").setStringProperty("default", "GlobalRAOptimizer");
        InterTemporalRao.Runner globalRaOptimizer = InterTemporalRao.find(null, List.of(new InterTemporalRaoProviderMock(), new AnotherInterTemporalRaoProviderMock()), platformConfig);
        assertEquals("GlobalRAOptimizer", globalRaOptimizer.getName());
        assertEquals("2.3", globalRaOptimizer.getVersion());
    }

    @Test
    void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("rao").setStringProperty("default", "UnknownRao");
        List<InterTemporalRaoProvider> raoProviders = List.of(new InterTemporalRaoProviderMock());
        assertThrows(OpenRaoException.class, () -> InterTemporalRao.find(null, raoProviders, platformConfig));
    }
}
