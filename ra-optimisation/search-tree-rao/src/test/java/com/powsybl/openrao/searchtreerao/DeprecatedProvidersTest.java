/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.TimeCoupledRaoInput;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoimpl.algorithms.OpenRao;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The deprecated provider façades keep the historical registry names so that existing
 * {@code Rao.find(...)} / {@code TimeCoupledRao.find(...)} calls keep working; they all
 * delegate to the unique {@link OpenRao} provider.
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class DeprecatedProvidersTest {

    private static List<RaoProvider> raoProviders() {
        return new ServiceLoaderCache<>(RaoProvider.class).getServices();
    }

    private static RaoProvider findRaoProvider(String name) {
        return raoProviders().stream()
            .filter(provider -> provider.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void searchTreeRaoFacadeIsTheOnlyProviderWithThisName() {
        assertThat(raoProviders().stream().filter(p -> p.getName().equals("SearchTreeRao"))).hasSize(1);
        assertThat(findRaoProvider("SearchTreeRao")).isInstanceOf(Castor.class);
        assertThatCode(() -> Rao.find("SearchTreeRao")).doesNotThrowAnyException();
    }

    @Test
    void fastRaoFacadeIsTheOnlyProviderWithThisName() {
        assertThat(raoProviders().stream().filter(p -> p.getName().equals("FastRao"))).hasSize(1);
        assertThat(findRaoProvider("FastRao")).isInstanceOf(FastRao.class);
        assertThatCode(() -> Rao.find("FastRao")).doesNotThrowAnyException();
    }

    @Test
    void timeCoupledRaoFacadeIsTheOnlyProviderWithThisName() {
        assertThat(raoProviders().stream().filter(p -> p.getName().equals("TimeCoupledRao"))).hasSize(1);
        assertThat(findRaoProvider("TimeCoupledRao")).isInstanceOf(Marmot.class);
        assertThat(new ServiceLoaderCache<>(TimeCoupledRaoProvider.class).getServices().stream()
            .filter(p -> p.getName().equals("TimeCoupledRao"))).hasSize(1);
        assertThat(TimeCoupledRao.find("TimeCoupledRao", ReportNode.NO_OP).getName()).isEqualTo("TimeCoupledRao");
        assertThatCode(() -> Rao.find("TimeCoupledRao")).doesNotThrowAnyException();
    }

    @Test
    void openRaoProviderIsRegisteredAndIsTheDefault() {
        assertThat(findRaoProvider("OpenRAO")).isInstanceOf(OpenRao.class);
        assertThatCode(() -> Rao.find()).doesNotThrowAnyException();
    }

    @Test
    void castorDelegatesAfterRemovingFastRaoParameters() {
        RaoInput input = mock(RaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        parameters.addExtension(FastRaoParameters.class, new FastRaoParameters());
        Instant deadline = Instant.parse("2026-01-01T00:00:00Z");
        CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(mock(RaoResult.class));

        try (MockedConstruction<OpenRao> openRao = mockConstruction(OpenRao.class, (delegate, context) ->
            when(delegate.run(input, parameters, deadline, ReportNode.NO_OP)).thenAnswer(invocation -> {
                assertThat(parameters.hasExtension(FastRaoParameters.class)).isFalse();
                return result;
            }))) {
            assertThat(new Castor().run(input, parameters, deadline, ReportNode.NO_OP)).isSameAs(result);
            assertThat(openRao.constructed()).hasSize(1);
            verify(openRao.constructed().get(0)).run(input, parameters, deadline, ReportNode.NO_OP);
        }
    }

    @Test
    void fastRaoDelegatesAfterAddingMissingParameters() {
        RaoInput input = mock(RaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(mock(RaoResult.class));

        try (MockedConstruction<OpenRao> openRao = mockConstruction(OpenRao.class, (delegate, context) ->
            when(delegate.run(input, parameters, null, ReportNode.NO_OP)).thenAnswer(invocation -> {
                assertThat(parameters.hasExtension(FastRaoParameters.class)).isTrue();
                return result;
            }))) {
            assertThat(new FastRao().run(input, parameters, ReportNode.NO_OP)).isSameAs(result);
            assertThat(openRao.constructed()).hasSize(1);
            verify(openRao.constructed().get(0)).run(input, parameters, null, ReportNode.NO_OP);
        }
    }

    @Test
    void fastRaoDelegatesWithoutReplacingExistingParameters() {
        RaoInput input = mock(RaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        FastRaoParameters fastParameters = new FastRaoParameters();
        parameters.addExtension(FastRaoParameters.class, fastParameters);
        Instant deadline = Instant.parse("2026-01-01T00:00:00Z");
        CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(mock(RaoResult.class));

        try (MockedConstruction<OpenRao> openRao = mockConstruction(OpenRao.class, (delegate, context) ->
            when(delegate.run(input, parameters, deadline, ReportNode.NO_OP)).thenAnswer(invocation -> {
                FastRaoParameters actualParameters = parameters.getExtension(FastRaoParameters.class);
                assertThat(actualParameters).isSameAs(fastParameters);
                return result;
            }))) {
            assertThat(new FastRao().run(input, parameters, deadline, ReportNode.NO_OP)).isSameAs(result);
            assertThat(openRao.constructed()).hasSize(1);
            verify(openRao.constructed().get(0)).run(input, parameters, deadline, ReportNode.NO_OP);
        }
    }

    @Test
    void marmotDelegatesTimeCoupledRun() {
        TimeCoupledRaoInput input = mock(TimeCoupledRaoInput.class);
        RaoParameters parameters = new RaoParameters(ReportNode.NO_OP);
        CompletableFuture<RaoResult> result = CompletableFuture.completedFuture(mock(RaoResult.class));

        try (MockedConstruction<OpenRao> openRao = mockConstruction(OpenRao.class, (delegate, context) ->
            when(delegate.run(input, parameters, ReportNode.NO_OP)).thenReturn(result))) {
            assertThat(new Marmot().run(input, parameters, ReportNode.NO_OP)).isSameAs(result);
            assertThat(openRao.constructed()).hasSize(1);
            verify(openRao.constructed().get(0)).run(input, parameters, ReportNode.NO_OP);
        }
    }
}
