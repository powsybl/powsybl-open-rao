/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.TimeCoupledRaoResult;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.raomock.AnotherRaoProviderMock;
import com.powsybl.openrao.raoapi.raomock.RaoProviderMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.FileSystem;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
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

        // run rao
        RaoResult result = defaultRao.run(raoInput, new RaoParameters(ReportNode.NO_OP));
        assertNotNull(result);
        assertEquals(ComputationStatus.DEFAULT, result.getComputationStatus());
        RaoResult resultAsync = defaultRao.runAsync(raoInput, new RaoParameters(ReportNode.NO_OP)).join();
        assertNotNull(resultAsync);
        assertEquals(ComputationStatus.DEFAULT, resultAsync.getComputationStatus());
    }

    private RaoProvider mockTimeCoupledProvider(TimeCoupledRaoResult timeCoupledRaoResult) {
        RaoProvider provider = Mockito.mock(RaoProvider.class);
        Mockito.when(provider.getName()).thenReturn("TimeCoupledRAO");
        Mockito.when(provider.run(Mockito.any(TimeCoupledRaoInput.class), Mockito.any(RaoParameters.class), Mockito.any(ReportNode.class)))
            .thenReturn(CompletableFuture.completedFuture(timeCoupledRaoResult));
        return provider;
    }

    @Test
    void testTimeCoupledRunSignatures() {
        TimeCoupledRaoResult expected = Mockito.mock(TimeCoupledRaoResult.class);
        RaoProvider provider = mockTimeCoupledProvider(expected);
        Rao.Runner runner = new Rao.Runner(provider);
        TimeCoupledRaoInput input = Mockito.mock(TimeCoupledRaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);

        assertSame(expected, runner.run(input, parameters, ReportNode.NO_OP));
        assertSame(expected, runner.run(input, parameters));
        assertSame(expected, runner.run(input, ReportNode.NO_OP));
        assertSame(expected, runner.run(input));
        Mockito.verify(provider, Mockito.times(4)).run(Mockito.eq(input), Mockito.any(RaoParameters.class), Mockito.any(ReportNode.class));
        Mockito.verify(provider, Mockito.times(2)).run(input, parameters, ReportNode.NO_OP);
    }

    @Test
    void testTimeCoupledRunWithNullArgumentsThrows() {
        Rao.Runner runner = new Rao.Runner(mockTimeCoupledProvider(Mockito.mock(TimeCoupledRaoResult.class)));
        TimeCoupledRaoInput input = Mockito.mock(TimeCoupledRaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        assertThrows(NullPointerException.class, () -> runner.run((TimeCoupledRaoInput) null, parameters, ReportNode.NO_OP));
        assertThrows(NullPointerException.class, () -> runner.run(input, (RaoParameters) null, ReportNode.NO_OP));
    }

    @Test
    void testTimeCoupledRunNotSupportedByProvider() {
        Rao.Runner runner = Rao.find(null, List.of(new RaoProviderMock()), platformConfig);
        TimeCoupledRaoInput input = Mockito.mock(TimeCoupledRaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        assertThrows(OpenRaoException.class, () -> runner.run(input, parameters));
    }

    @Test
    void testRunSignatures() {
        Rao.Runner runner = Rao.find(null, List.of(new RaoProviderMock()), platformConfig);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        Instant targetEndInstant = Instant.now().plusSeconds(3600);

        assertNotNull(runner.run(raoInput, parameters, targetEndInstant, ReportNode.NO_OP));
        assertNotNull(runner.run(raoInput, parameters, targetEndInstant));
        assertNotNull(runner.run(raoInput, parameters, ReportNode.NO_OP));
        assertNotNull(runner.run(raoInput, parameters));
        assertNotNull(runner.runAsync(raoInput, parameters, targetEndInstant, ReportNode.NO_OP).join());
        assertNotNull(runner.runAsync(raoInput, parameters, targetEndInstant).join());
        assertNotNull(runner.runAsync(raoInput, parameters, ReportNode.NO_OP).join());
    }

    @Test
    void testRunWithNullArgumentsThrows() {
        Rao.Runner runner = Rao.find(null, List.of(new RaoProviderMock()), platformConfig);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        assertThrows(NullPointerException.class, () -> runner.run((RaoInput) null, parameters));
        assertThrows(NullPointerException.class, () -> runner.run(raoInput, (RaoParameters) null));
        assertThrows(NullPointerException.class, () -> runner.run(raoInput, parameters, null, null));
        assertThrows(NullPointerException.class, () -> runner.runAsync((RaoInput) null, parameters));
        assertThrows(NullPointerException.class, () -> runner.runAsync(raoInput, (RaoParameters) null));
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
    }

    @Test
    void testOneProviderAndMistakeInPlatformConfig() {
        // case with 1 provider with config but with a name that is not the one of provider.
        platformConfig.createModuleConfig("rao").setStringProperty("default", "UnknownRao");
        List<RaoProvider> raoProviders = List.of(new RaoProviderMock());
        assertThrows(OpenRaoException.class, () -> Rao.find(null, raoProviders, platformConfig));
    }
}
