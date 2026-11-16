/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.openrao.raoapi.Rao;
import com.powsybl.openrao.raoapi.RaoProvider;
import com.powsybl.openrao.raoapi.TimeCoupledRao;
import com.powsybl.openrao.raoapi.TimeCoupledRaoProvider;
import com.powsybl.openrao.raoimpl.OpenRao;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Castor;
import com.powsybl.openrao.searchtreerao.fastrao.FastRao;
import com.powsybl.openrao.searchtreerao.marmot.Marmot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
}
